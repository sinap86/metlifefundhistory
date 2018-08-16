package hu.sinap86.metlifefundhistory.ui.dialog;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.swing.*;

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

    // override the createRootPane inherited by the JDialog, to create the rootPane.
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

    protected void showErrorDialog(final String text) {
        JOptionPane.showMessageDialog(this, text, "Hiba", JOptionPane.ERROR_MESSAGE);
    }

    protected File showFileChooser(final String title, final int selectionMode) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(selectionMode);
        if (selectionMode == JFileChooser.DIRECTORIES_ONLY) {
            chooser.setAcceptAllFileFilterUsed(false);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    protected DatePicker createDatePicker() {
        final DatePickerSettings dateSettings = new DatePickerSettings(new Locale("hu"));
        dateSettings.setFirstDayOfWeek(DayOfWeek.MONDAY);
        dateSettings.setTranslationToday("Ma");
        dateSettings.setTranslationClear("Töröl");
        dateSettings.setFormatForDatesCommonEra("yyyy/MM/dd");
        dateSettings.setFormatForDatesBeforeCommonEra("uuuu/MM/dd");
        return new DatePicker(dateSettings);
    }

    protected JLabel addLabel(final String text, final JPanel panel, final int row, final int column) {
        return addComponent(new JLabel(text), panel, row, column);
    }

    protected JTextField addTextField(final int columns, final JPanel panel, final int row, final int column) {
        return addComponent(new JTextField(columns), panel, row, column);
    }

    protected <T extends Component> T addComponent(final T component, final JPanel panel, final int row, final int column) {
        return addComponent(component, panel, row, column, null);
    }

    protected <T extends Component> T addComponent(final T component, final JPanel panel, final int row, final int column, final Integer columnWidth) {
        panel.add(component, getConstraints(row, column, columnWidth));
        return component;
    }

    protected void addFocusTraversalKey(final Container container, final int key) {
        final Set<AWTKeyStroke> keys = container.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        final Set<AWTKeyStroke> newKeys = new HashSet<>(keys);
        newKeys.add(KeyStroke.getKeyStroke(key, 0));
        container.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, newKeys);
    }

    protected GridBagConstraints getConstraints(final int row, final int column) {
        return getConstraints(row, column, null);
    }

    protected GridBagConstraints getConstraints(final int row, final int column, final Integer columnWidth) {
        final GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 0, 4, 4);  // padding
        gc.gridx = column;
        gc.gridy = row;
        if (columnWidth != null) {
            gc.gridwidth = columnWidth;
        }
        return gc;
    }

}
