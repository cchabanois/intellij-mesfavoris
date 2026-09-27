package mesfavoris.github.test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PacketLineOut;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.RefAdvertiser;
import org.eclipse.jgit.transport.UploadPack;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;

/**
 * Serves each fake gist as a real git repository over the git smart HTTP protocol, so code that clones or pushes
 * a gist ({@code git_pull_url} / {@code git_push_url}) runs offline. Built on JGit's {@link UploadPack} and
 * {@link ReceivePack} behind the JDK {@link HttpServer} — what JGit's {@code GitServlet} does internally — to
 * keep a servlet container off the test classpath. Requests must use HTTP Basic auth whose password is the
 * expected token, like GitHub's {@code x-access-token:<token>}.
 */
public class FakeGistGitServer {

    private static final String BRANCH = Constants.R_HEADS + "main";

    private final String token;
    private final BiConsumer<String, Map<String, String>> onPush;

    private File baseDir;
    private HttpServer server;
    private ExecutorService executor;

    /**
     * @param onPush called with the gist id and its new files after a successful push, so the REST view can
     *               follow the repository
     */
    public FakeGistGitServer(String token, BiConsumer<String, Map<String, String>> onPush) {
        this.token = token;
        this.onPush = onPush;
    }

    public void start() throws IOException {
        baseDir = Files.createTempDirectory("fake-gist-git-").toFile();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        if (baseDir != null) {
            try {
                FileUtils.delete(baseDir, FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS);
            } catch (IOException e) {
                // best effort
            }
        }
    }

    public String repoUrl(String gistId) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/" + gistId + ".git";
    }

    /** Commits {@code files} as the full content of the gist, creating its repository on first use. */
    public void commit(String gistId, Map<String, String> files) throws IOException {
        File dir = repoDir(gistId);
        if (!dir.exists()) {
            try {
                Git.init().setBare(true).setDirectory(dir).setInitialBranch("main").call().close();
            } catch (GitAPIException e) {
                throw new IOException(e);
            }
        }
        try (Repository repo = open(dir); ObjectInserter inserter = repo.newObjectInserter()) {
            TreeFormatter tree = new TreeFormatter();
            for (Map.Entry<String, String> file : new TreeMap<>(files).entrySet()) {
                ObjectId blob = inserter.insert(Constants.OBJ_BLOB,
                        file.getValue().getBytes(StandardCharsets.UTF_8));
                tree.append(file.getKey(), FileMode.REGULAR_FILE, blob);
            }
            CommitBuilder commit = new CommitBuilder();
            commit.setTreeId(inserter.insert(tree));
            ObjectId head = repo.resolve(BRANCH);
            if (head != null) {
                commit.setParentId(head);
            }
            PersonIdent ident = new PersonIdent("test-user", "test-user@users.noreply.github.com");
            commit.setAuthor(ident);
            commit.setCommitter(ident);
            commit.setMessage("");
            ObjectId commitId = inserter.insert(commit);
            inserter.flush();
            RefUpdate update = repo.updateRef(BRANCH);
            update.setNewObjectId(commitId);
            RefUpdate.Result result = update.forceUpdate();
            if (result != RefUpdate.Result.NEW && result != RefUpdate.Result.FORCED
                    && result != RefUpdate.Result.FAST_FORWARD) {
                throw new IOException("Could not update " + BRANCH + " of gist " + gistId + ": " + result);
            }
        }
    }

    /** The commit id at the tip of the gist's branch. */
    public String head(String gistId) {
        try (Repository repo = open(repoDir(gistId))) {
            ObjectId head = repo.resolve(BRANCH);
            return head != null ? head.name() : null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void delete(String gistId) {
        try {
            FileUtils.delete(repoDir(gistId), FileUtils.RECURSIVE | FileUtils.IGNORE_ERRORS);
        } catch (IOException e) {
            // best effort
        }
    }

    private File repoDir(String gistId) {
        return new File(baseDir, gistId + ".git");
    }

    private static Repository open(File dir) throws IOException {
        return new FileRepositoryBuilder().setGitDir(dir).setBare().build();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (!authorized(exchange)) {
                exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"gist\"");
                exchange.sendResponseHeaders(401, -1);
                return;
            }
            // /<gistId>.git/<action>
            String path = exchange.getRequestURI().getPath();
            int gitSuffix = path.indexOf(".git/");
            if (gitSuffix < 1) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String gistId = path.substring(1, gitSuffix);
            String action = path.substring(gitSuffix + ".git/".length());
            File dir = repoDir(gistId);
            if (!dir.exists()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String method = exchange.getRequestMethod();
            try (Repository repo = open(dir)) {
                if (action.equals("info/refs") && method.equals("GET")) {
                    advertiseRefs(exchange, repo, service(exchange.getRequestURI().getQuery()));
                } else if (action.equals("git-upload-pack") && method.equals("POST")) {
                    uploadPack(exchange, repo);
                } else if (action.equals("git-receive-pack") && method.equals("POST")) {
                    receivePack(exchange, repo, gistId);
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            }
        } finally {
            exchange.close();
        }
    }

    private boolean authorized(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Basic ")) {
            return false;
        }
        String credentials = new String(Base64.getDecoder().decode(header.substring("Basic ".length()).trim()),
                StandardCharsets.UTF_8);
        int colon = credentials.indexOf(':');
        return colon >= 0 && credentials.substring(colon + 1).equals(token);
    }

    private static String service(String query) {
        return query != null && query.startsWith("service=") ? query.substring("service=".length()) : null;
    }

    private static void advertiseRefs(HttpExchange exchange, Repository repo, String service) throws IOException {
        if (!"git-upload-pack".equals(service) && !"git-receive-pack".equals(service)) {
            // No dumb-protocol support: only smart clients are expected.
            exchange.sendResponseHeaders(403, -1);
            return;
        }
        exchange.getResponseHeaders().add("Content-Type", "application/x-" + service + "-advertisement");
        exchange.getResponseHeaders().add("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, 0);
        OutputStream out = exchange.getResponseBody();
        PacketLineOut packetOut = new PacketLineOut(out);
        packetOut.writeString("# service=" + service + "\n");
        packetOut.end();
        RefAdvertiser.PacketLineOutRefAdvertiser advertiser = new RefAdvertiser.PacketLineOutRefAdvertiser(packetOut);
        if (service.equals("git-upload-pack")) {
            UploadPack uploadPack = new UploadPack(repo);
            uploadPack.setBiDirectionalPipe(false);
            uploadPack.sendAdvertisedRefs(advertiser);
        } else {
            ReceivePack receivePack = new ReceivePack(repo);
            receivePack.setBiDirectionalPipe(false);
            receivePack.sendAdvertisedRefs(advertiser);
        }
        out.flush();
    }

    private static void uploadPack(HttpExchange exchange, Repository repo) throws IOException {
        UploadPack uploadPack = new UploadPack(repo);
        uploadPack.setBiDirectionalPipe(false);
        exchange.getResponseHeaders().add("Content-Type", "application/x-git-upload-pack-result");
        exchange.sendResponseHeaders(200, 0);
        uploadPack.upload(requestBody(exchange), exchange.getResponseBody(), null);
    }

    private void receivePack(HttpExchange exchange, Repository repo, String gistId) throws IOException {
        ReceivePack receivePack = new ReceivePack(repo);
        receivePack.setBiDirectionalPipe(false);
        receivePack.setAllowNonFastForwards(false);
        receivePack.setPostReceiveHook((pack, commands) -> {
            try {
                onPush.accept(gistId, readFiles(repo));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        exchange.getResponseHeaders().add("Content-Type", "application/x-git-receive-pack-result");
        exchange.sendResponseHeaders(200, 0);
        receivePack.receive(requestBody(exchange), exchange.getResponseBody(), null);
    }

    /** The files at the tip of the gist's branch (gists are flat: no directories). */
    private static Map<String, String> readFiles(Repository repo) throws IOException {
        Map<String, String> files = new LinkedHashMap<>();
        ObjectId head = repo.resolve(BRANCH);
        if (head == null) {
            return files;
        }
        try (RevWalk walk = new RevWalk(repo); TreeWalk tree = new TreeWalk(repo)) {
            tree.addTree(walk.parseCommit(head).getTree());
            while (tree.next()) {
                byte[] content = repo.open(tree.getObjectId(0)).getBytes(Integer.MAX_VALUE);
                files.put(tree.getPathString(), new String(content, StandardCharsets.UTF_8));
            }
        }
        return files;
    }

    /**
     * The request body, fully buffered: UploadPack/ReceivePack drain leftover input with {@code skip()} when done,
     * and the JDK server's body stream bounds {@code read()} but not {@code skip()} to the request length, so a
     * direct skip would block on the socket while git waits for the response. git gzips larger bodies.
     */
    private static InputStream requestBody(HttpExchange exchange) throws IOException {
        InputStream in = new ByteArrayInputStream(exchange.getRequestBody().readAllBytes());
        return "gzip".equalsIgnoreCase(exchange.getRequestHeaders().getFirst("Content-Encoding"))
                ? new GZIPInputStream(in) : in;
    }
}
