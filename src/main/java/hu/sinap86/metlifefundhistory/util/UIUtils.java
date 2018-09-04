package hu.sinap86.metlifefundhistory.util;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import hu.sinap86.metlifefundhistory.config.Constants;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.Set;

public class UIUtils {

    public static final FileNameExtensionFilter XML_FILE_NAME_FILTER = new FileNameExtensionFilter(
            "XML fájlok", "xml", "XML");

    public static File showFileChooser(final Component parent, final String title, final int selectionMode) {
        return showFileChooser(parent, title, selectionMode, null);
    }

    public static File showFileChooser(final Component parent, final String title, final int selectionMode, final FileFilter filter) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(selectionMode);
        chooser.setFileFilter(filter);
        if (selectionMode == JFileChooser.DIRECTORIES_ONLY || filter != null) {
            chooser.setAcceptAllFileFilterUsed(false);
        }
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    public static void showErrorDialog(final Component parent, final String text) {
        JOptionPane.showMessageDialog(parent, text, "Hiba", JOptionPane.ERROR_MESSAGE);
    }

    public static JLabel addLabel(final String text, final Container container,
                                  final int row, final int column) {
        return addLabel(text, container, row, column, null);
    }

    public static JLabel addLabel(final String text, final Container container,
                                  final int row, final int column, final Integer columnWidth) {
        return addComponent(new JLabel(text), container, row, column, columnWidth);
    }

    public static JTextField addTextField(final int columns, final Container container,
                                          final int row, final int column) {
        return addComponent(new JTextField(columns), container, row, column);
    }

    public static <T extends Component> T addComponent(final T component, final Container container,
                                                       final int row, final int column) {
        return addComponent(component, container, row, column, null);
    }

    public static <T extends Component> T addComponent(final T component, final Container container,
                                                       final int row, final int column, final Integer columnWidth) {
        container.add(component, getConstraints(row, column, columnWidth));
        return component;
    }

    public static GridBagConstraints getConstraints(final int row, final int column) {
        return getConstraints(row, column, null);
    }

    public static GridBagConstraints getConstraints(final int row, final int column, final Integer columnWidth) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = (column == 0) ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);  // padding
        gbc.gridx = column;
        gbc.gridy = row;
        if (columnWidth != null) {
            gbc.gridwidth = columnWidth;
        }
        gbc.gridheight = 1;
        gbc.anchor = (column == 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;

        gbc.weightx = (column == 0) ? 0.1 : 1.0;
        gbc.weighty = 1.0;

        return gbc;
    }

    public static DatePicker createDatePicker() {
        final DatePickerSettings dateSettings = new DatePickerSettings(Constants.LOCALE_HU);
        dateSettings.setFirstDayOfWeek(DayOfWeek.MONDAY);
        dateSettings.setTranslationToday("Ma");
        dateSettings.setTranslationClear("Töröl");
        dateSettings.setFormatForDatesCommonEra("yyyy/MM/dd");
        dateSettings.setFormatForDatesBeforeCommonEra("uuuu/MM/dd");
        return new DatePicker(dateSettings);
    }

    public static void addFocusTraversalKey(final Container container, final int key) {
        final Set<AWTKeyStroke> keys = container.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        final Set<AWTKeyStroke> newKeys = new HashSet<>(keys);
        newKeys.add(KeyStroke.getKeyStroke(key, 0));
        container.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, newKeys);
    }


}
