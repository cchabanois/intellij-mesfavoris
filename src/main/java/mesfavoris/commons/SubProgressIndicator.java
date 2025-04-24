package mesfavoris.commons;

import com.intellij.ide.util.DelegatingProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.Nls;

public class SubProgressIndicator extends DelegatingProgressIndicator implements AutoCloseable {
    private final double subFraction = 0.0;
    private final double parentBaseFraction;
    private final double parentFraction;


    public SubProgressIndicator(ProgressIndicator parentProgressIndicator, double parentFraction) {
        super(parentProgressIndicator);
        this.parentBaseFraction = parentProgressIndicator.getFraction();
        this.parentFraction = parentFraction;
    }

    public double getFraction() {
        return subFraction;
    }

    @Override
    public void setText(@Nls @NlsContexts.ProgressText String text) {
        super.setText2(text);
    }

    @Override
    public void setFraction(double fraction) {
        double actualFraction = Math.min(parentBaseFraction + subFraction * parentFraction, 1.0);
        super.setFraction(actualFraction);
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
        super.setIndeterminate(indeterminate);
    }

    @Override
    public void close() {
        setFraction(1.0);
        setText("");
    }
}
