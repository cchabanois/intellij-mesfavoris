package mesfavoris.internal.markers.inlay;

import com.intellij.codeInsight.hints.presentation.BasePresentation;
import com.intellij.codeInsight.hints.presentation.InlayTextMetrics;
import com.intellij.codeInsight.hints.presentation.InlayTextMetricsStorage;
import com.intellij.ide.ui.AntialiasingType;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * InlayPresentation that displays bookmark comments with multiple lines of text and a vertical line on the left
 */
public class BookmarkCommentInlayPresentation extends BasePresentation {
    private static final TextAttributesKey BOOKMARK_COMMENT_KEY = TextAttributesKey.createTextAttributesKey(
            "BOOKMARK_COMMENT",
            DefaultLanguageHighlighterColors.INLAY_DEFAULT
    );

    private static final int LINE_WIDTH = 1;
    private static final int LEFT_PADDING = 4;

    private final InlayTextMetricsStorage metricsStorage;
    private final EditorColorsScheme colorsScheme;
    private final String[] lines;

    public BookmarkCommentInlayPresentation(@NotNull InlayTextMetricsStorage metricsStorage,
                                            @NotNull EditorColorsScheme colorsScheme,
                                            @NotNull String[] lines) {
        this.metricsStorage = metricsStorage;
        this.colorsScheme = colorsScheme;
        this.lines = lines;
    }

    @Override
    public int getWidth() {
        int maxWidth = 0;
        var metrics = getMetrics();
        for (String line : lines) {
            int stringWidth = metrics.getStringWidth(line);
            if (stringWidth > maxWidth) {
                maxWidth = stringWidth;
            }
        }
        return maxWidth + LINE_WIDTH + LEFT_PADDING;
    }

    @Override
    public int getHeight() {
        var metrics = getMetrics();
        return metrics.getFontHeight() * lines.length;
    }

    @Override
    public void paint(@NotNull Graphics2D g, @NotNull TextAttributes attributes) {
        Graphics2D g2d = (Graphics2D) g.create();

        try {
            var metrics = getMetrics();
            var font = metrics.getFont();
            g2d.setFont(font);

            // Get the text color from color scheme
            Color foreground = getTextColor();
            g2d.setColor(foreground);

            // Draw the vertical line on the left with the same color as the text
            g2d.setStroke(new BasicStroke(LINE_WIDTH));

            int lineX = LINE_WIDTH / 2;
            int y1 = 0;
            int y2 = getHeight();

            g2d.drawLine(lineX, y1, lineX, y2);

            // Draw each line of text
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                AntialiasingType.getKeyForCurrentScope(false));

            int textX = LINE_WIDTH + LEFT_PADDING;
            int fontHeight = metrics.getFontHeight();

            for (int i = 0; i < lines.length; i++) {
                int textY = (i * fontHeight) + metrics.getFontBaseline();
                g2d.drawString(lines[i], textX, textY);
            }
        } finally {
            g2d.dispose();
        }
    }

    @NotNull
    private Color getTextColor() {
        TextAttributes textAttributes = colorsScheme.getAttributes(BOOKMARK_COMMENT_KEY);
        if (textAttributes != null && textAttributes.getForegroundColor() != null) {
            return textAttributes.getForegroundColor();
        }
        // Fallback to default inlay color
        Color inlayColor = colorsScheme.getAttributes(DefaultLanguageHighlighterColors.INLAY_DEFAULT).getForegroundColor();
        if (inlayColor != null) {
            return inlayColor;
        }
        // Ultimate fallback to gray
        return Color.GRAY;
    }

    private InlayTextMetrics getMetrics() {
        return metricsStorage.getFontMetrics(true);
    }

    @NotNull
    @Override
    public String toString() {
        return String.join("\n", lines);
    }
}

