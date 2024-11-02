package mesfavoris.ui.renderers;

import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StyledString implements CharSequence {
    private final List<StyledStringFragment> fragments;

    public StyledString() {
        this(new ArrayList<>());
    }

    public StyledString(String text) {
        this(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
    public StyledString(String text, SimpleTextAttributes style) {
        this(new ArrayList<>(Arrays.asList(new StyledStringFragment(text, style))));
    }

    private StyledString(List<StyledStringFragment> fragments) {
        this.fragments = fragments;
    }

    public StyledString append(String text) {
        return append(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    public StyledString append(String text, SimpleTextAttributes style) {
        List<StyledStringFragment> fragments = new ArrayList<>(this.fragments);
        fragments.add(new StyledStringFragment(text, style));
        return new StyledString(fragments);
    }

    public StyledString append(StyledString styledString) {
        List<StyledStringFragment> fragments = new ArrayList<>(this.fragments);
        fragments.addAll(styledString.getFragments());
        return new StyledString(fragments);
    }

    public StyledString setStyle(SimpleTextAttributes style) {
        return new StyledString(this.toString(), style);
    }

    public List<StyledStringFragment> getFragments() {
        return Collections.unmodifiableList(fragments);
    }

    public void appendTo(ColoredTextContainer coloredTextContainer) {
        for (StyledStringFragment fragment : fragments) {
            coloredTextContainer.append(fragment.getText(), fragment.getStyle());
        }
    }

    @Override
    public int length() {
        return fragments.stream().mapToInt(StyledStringFragment::length).sum();
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        fragments.forEach(fragment -> sb.append(fragment.text));
        return sb.toString();
    }

    public static class StyledStringFragment implements CharSequence {
        private final String text;
        private final SimpleTextAttributes style;

        private StyledStringFragment(String text, SimpleTextAttributes style) {
            this.text = text;
            this.style = style;
        }

        public String getText() {
            return text;
        }

        public SimpleTextAttributes getStyle() {
            return style;
        }

        @Override
        public int length() {
            return text.length();
        }

        @Override
        public char charAt(int index) {
            return text.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return text.subSequence(start, end);
        }

        @Override
        public String toString() {
            return text;
        }
    }

}
