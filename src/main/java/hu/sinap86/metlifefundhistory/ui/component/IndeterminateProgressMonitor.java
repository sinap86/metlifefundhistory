package hu.sinap86.metlifefundhistory.ui.component;

import java.awt.*;

import javax.swing.*;

public class IndeterminateProgressMonitor extends ProgressMonitor {

    public IndeterminateProgressMonitor(final Component parent, final Object message, final String note) {
        super(parent, message, note, 0, 100);
        setMillisToDecideToPopup(0);
        setMillisToPopup(0);
        setProgress(50);
        makeIndeterminate();
    }

    public IndeterminateProgressMonitor(final Component parent, final Object message, final String note, final int min, final int max) {
        super(parent, message, note, min, max);
        makeIndeterminate();
    }

    private void makeIndeterminate() {
        try {
            final JProgressBar bar = (JProgressBar) getAccessibleContext().getAccessibleChild(1);
            bar.setIndeterminate(true);
        } catch (Exception ex) {
        }
    }

}
