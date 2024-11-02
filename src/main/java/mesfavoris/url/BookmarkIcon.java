package mesfavoris.url;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class BookmarkIcon implements Icon {
    private final byte[] favIconBytes;
    private volatile Icon icon;

    public BookmarkIcon(byte[] favIconBytes) {
        this.favIconBytes = favIconBytes;
    }


    private Icon getIcon() {
        if (icon == null) {
            synchronized (this) {
                if (icon == null) {
                    try {
                        icon = new ImageIcon(getBestImage());
                    } catch (IOException e) {
                        icon = UrlIcons.pageTagBlue;
                    }
                }
            }
        }
        return icon;
    }

    private Image getBestImage() throws IOException {
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(favIconBytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("No reader available for icon");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                BufferedImage bufferedImage = reader.read(getBestImageIndex(reader));
                if (bufferedImage.getWidth() <= 16 && bufferedImage.getHeight() <= 16) {
                    return bufferedImage;
                }
                return bufferedImage.getScaledInstance(16, 16, Image.SCALE_DEFAULT);
            } finally {
                // Dispose reader in finally block to avoid memory leaks
                reader.dispose();
            }
        }
    }

    private int getBestImageIndex(ImageReader reader) throws IOException {
        int minDistance = Integer.MAX_VALUE;
        int bestImageIndex = 0;
        for (int i = 0; i < reader.getNumImages(true); i++) {
            int distance = distanceFrom16x16Image(reader, i);
            if (distance < minDistance) {
                bestImageIndex = i;
                minDistance = distance;
            }
        }
        return bestImageIndex;
    }

    private int distanceFrom16x16Image(ImageReader reader, int imageIndex) throws IOException {
        return reader.getWidth(imageIndex) * reader.getHeight(imageIndex) - 16 * 16;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(favIconBytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BookmarkIcon)) {
            return false;
        }
        BookmarkIcon descriptor = (BookmarkIcon) obj;
        return Arrays.equals(descriptor.favIconBytes, favIconBytes);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        getIcon().paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
        return getIcon().getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return getIcon().getIconHeight();
    }
}
