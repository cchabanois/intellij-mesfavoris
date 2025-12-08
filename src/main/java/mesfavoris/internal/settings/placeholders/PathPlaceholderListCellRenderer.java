package mesfavoris.internal.settings.placeholders;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import mesfavoris.service.IBookmarksService;
import mesfavoris.path.PathBookmarkProperties;
import mesfavoris.placeholders.PathPlaceholder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static mesfavoris.placeholders.IPathPlaceholders.PLACEHOLDER_HOME_NAME;

/**
 * Cell renderer for placeholder list items
 */
public class PathPlaceholderListCellRenderer extends ColoredListCellRenderer<PathPlaceholder> {
    private final PathPlaceholderStats pathPlaceholderStats;

    public PathPlaceholderListCellRenderer(@NotNull Project project) {
        IBookmarksService bookmarksService = project.getService(IBookmarksService.class);
        List<String> pathPropertyNames = Arrays.asList(PathBookmarkProperties.PROP_FILE_PATH);
        this.pathPlaceholderStats = new PathPlaceholderStats(bookmarksService::getBookmarksTree, pathPropertyNames);
    }

    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends PathPlaceholder> list, PathPlaceholder placeholder,
                                         int index, boolean selected, boolean hasFocus) {
        boolean unmodifiable = isUnmodifiable(placeholder);

        // Set background color for unmodifiable placeholders
        if (unmodifiable && !selected) {
            // Use a slightly darker background for unmodifiable items
            Color normalBackground = list.getBackground();
            JBColor unmodifiableBackground = new JBColor(
                    new Color(
                            Math.max(0, normalBackground.getRed() - 10),
                            Math.max(0, normalBackground.getGreen() - 10),
                            Math.max(0, normalBackground.getBlue() - 10)
                    ),
                    new Color(
                            Math.min(255, normalBackground.getRed() + 10),
                            Math.min(255, normalBackground.getGreen() + 10),
                            Math.min(255, normalBackground.getBlue() + 10)
                    )
            );
            setBackground(unmodifiableBackground);
        }

        // Choose text attributes based on modifiability
        SimpleTextAttributes nameAttributes = unmodifiable ?
                SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES :
                SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;

        SimpleTextAttributes pathAttributes = unmodifiable ?
                SimpleTextAttributes.GRAYED_ATTRIBUTES :
                SimpleTextAttributes.REGULAR_ATTRIBUTES;

        // Placeholder name
        append(placeholder.getName(), nameAttributes);

        // Show usage statistics for the current project
        int usageCount = pathPlaceholderStats.getUsageCount(placeholder.getName());

        // Usage count in different color
        if (usageCount > 0) {
            append(" (%d matches)".formatted(usageCount), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        } else {
            append(" (unused)", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        // Path
        append(" - " + placeholder.getPath().toString(), pathAttributes);
    }

    private boolean isUnmodifiable(PathPlaceholder pathPlaceholder) {
        return PLACEHOLDER_HOME_NAME.equals(pathPlaceholder.getName());
    }

}
