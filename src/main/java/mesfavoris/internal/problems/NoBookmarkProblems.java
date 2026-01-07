package mesfavoris.internal.problems;

import mesfavoris.model.BookmarkId;
import mesfavoris.problems.BookmarkProblem;
import mesfavoris.problems.IBookmarkProblemDescriptor;
import mesfavoris.problems.IBookmarkProblems;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * No-op implementation of IBookmarkProblems that does nothing.
 * Used when bookmark problem tracking is not needed.
 */
public class NoBookmarkProblems implements IBookmarkProblems {

    @Override
    public Optional<BookmarkProblem> getBookmarkProblem(BookmarkId bookmarkId, String problemType) {
        return Optional.empty();
    }

    @Override
    public Set<BookmarkProblem> getBookmarkProblems(BookmarkId bookmarkId) {
        return Collections.emptySet();
    }

    @Override
    public void delete(BookmarkProblem problem) {
        // No-op
    }

    @Override
    public void delete(BookmarkId bookmarkId) {
        // No-op
    }

    @Override
    public void add(BookmarkProblem problem) {
        // No-op
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public @NotNull Iterator<BookmarkProblem> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public IBookmarkProblemDescriptor getBookmarkProblemDescriptor(String type) {
        return new NoOpBookmarkProblemDescriptor();
    }

    /**
     * No-op implementation of IBookmarkProblemDescriptor
     */
    private static class NoOpBookmarkProblemDescriptor implements IBookmarkProblemDescriptor {

        @Override
        public String getProblemType() {
            return "";
        }

        @Override
        public Severity getSeverity() {
            return Severity.INFO;
        }

        @Override
        public String getErrorMessage(BookmarkProblem bookmarkProblem) {
            return "";
        }

        @Override
        public Optional<mesfavoris.problems.IBookmarkProblemHandler> getBookmarkProblemHandler() {
            return Optional.empty();
        }
    }
}

