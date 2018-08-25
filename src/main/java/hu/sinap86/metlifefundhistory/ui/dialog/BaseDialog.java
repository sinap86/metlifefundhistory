package hu.sinap86.metlifefundhistory.ui.dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class BaseDialog extends JDialog {

    public BaseDialog(final Frame owner, final String title, final boolean modal) {
        super(owner, title, modal);
    }

    protected void postConstruct(final Frame owner) {
        pack();
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(owner);
    }

    // create functionality to close the window when "Escape" button is pressed
    @Override
    public JRootPane createRootPane() {
        JRootPane rootPane = new JRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
        Action action = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        };
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", action);
        return rootPane;
    }

}
