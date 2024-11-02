package mesfavoris.internal;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;

public class MesFavorisProjectManagerListener implements ProjectManagerListener {
    private static final Logger LOG = Logger.getInstance(MesFavorisProjectManagerListener.class);

    @Override
    public void projectClosed(@NotNull Project project) {

    }

}
