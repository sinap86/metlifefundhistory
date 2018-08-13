package hu.sinap86.metlifefundhistory.ui;

import hu.sinap86.metlifefundhistory.web.WebRequestManager;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import org.apache.commons.collections4.CollectionUtils;

import java.awt.*;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;

import javax.swing.*;

public class TransactionHistoryQuerySettingsDialog extends JDialog {

    private final WebRequestManager webRequestManager;
    final JComboBox<String> cbContracts;
    final DatePicker dpFromDate;
    final DatePicker dpToDate;
    private final JTextField tfDirectory;
    private File selectedDirectory;

    public TransactionHistoryQuerySettingsDialog(Frame parent, final WebRequestManager webRequestManager) {
        super(parent, "Lekérdezés beállítások", true);
        this.webRequestManager = webRequestManager;
        final Collection<String> contracts = webRequestManager.getUserContracts();

        final JPanel topPanel = new JPanel(new GridBagLayout());

        final JLabel lblContracts = new JLabel("Szerződések: ");
        topPanel.add(lblContracts, getConstraints(0, 0));

        cbContracts = new JComboBox<>(contracts.toArray(new String[]{}));
        cbContracts.setSelectedIndex(0);
        cbContracts.setEnabled(CollectionUtils.size(contracts) > 1);
        topPanel.add(cbContracts, getConstraints(1, 0, 2));

        final JLabel lblFromDate = new JLabel("Kezdő dátum: ");
        topPanel.add(lblFromDate, getConstraints(0, 1));

        dpFromDate = createDatePicker();
        topPanel.add(dpFromDate, getConstraints(1, 1, 2));

        final JLabel lblToDate = new JLabel("Végdátum: ");
        topPanel.add(lblToDate, getConstraints(0, 2));

        dpToDate = createDatePicker();
        dpToDate.setDateToToday();
        topPanel.add(dpToDate, getConstraints(1, 2, 2));

        final JLabel lblDirectory = new JLabel("Adatok mentése ide: ");
        topPanel.add(lblDirectory, getConstraints(0, 3));

        tfDirectory = new JTextField(20);
        tfDirectory.setEnabled(false);
        tfDirectory.setPreferredSize(new Dimension(100, 26));
        topPanel.add(tfDirectory, getConstraints(1, 3));

        JButton btnChooseDirectory = new JButton("...");
        btnChooseDirectory.setPreferredSize(new Dimension(26, 26));
        btnChooseDirectory.addActionListener(event -> {
            showDirectoryChooser();
        });
        topPanel.add(btnChooseDirectory, getConstraints(2, 3));

        JButton btnQuery = new JButton("Lekérdez");
        btnQuery.addActionListener(event -> {
            if (validateUserInput()) {
                // TODO create query settings object
                setVisible(false);
                dispose();
            }
        });

        JButton btnCancel = new JButton("Mégse");
        btnCancel.addActionListener(event -> {
            setVisible(false);
            dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnQuery);
        buttonPanel.add(btnCancel);

        getContentPane().add(topPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
    }

    private DatePicker createDatePicker() {
        final DatePickerSettings dateSettings = new DatePickerSettings(new Locale("hu"));
        dateSettings.setFirstDayOfWeek(DayOfWeek.MONDAY);
        dateSettings.setTranslationToday("Ma");
        dateSettings.setTranslationClear("Töröl");
        dateSettings.setFormatForDatesCommonEra("yyyy/MM/dd");
        dateSettings.setFormatForDatesBeforeCommonEra("uuuu/MM/dd");
        return new DatePicker(dateSettings);
    }

    private void showDirectoryChooser() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Könyvtár megnyitása");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // disable the "All files" option.
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedDirectory = chooser.getSelectedFile();
            tfDirectory.setText(selectedDirectory.getAbsolutePath());
        } else {
            tfDirectory.setText("");
        }
    }

    private boolean validateUserInput() {
        final Object contract = cbContracts.getSelectedItem();
        if (contract == null) {
            showErrorDialog("Nincs szerződés kiválasztva!");
            return false;
        }
        final LocalDate fromDate = dpFromDate.getDate();
        if (fromDate == null) {
            showErrorDialog("Nincs kezdő dátum kiválasztva!");
            return false;
        }
        final LocalDate toDate = dpToDate.getDate();
        if (toDate == null) {
            showErrorDialog("Nincs végdátum kiválasztva!");
            return false;
        }
        if (fromDate.isAfter(toDate)) {
            showErrorDialog("Vég dátum nem lehet a kezdő dátum előtti!");
            return false;
        }
        if (selectedDirectory == null) {
            showErrorDialog("Nincs adat mentési könyvtár kiválasztva!");
            return false;
        }
        if (!selectedDirectory.canWrite()) {
            showErrorDialog("A kiválasztott adat mentési könyvtár nem írható!");
            return false;
        }
        return true;
    }

    private void showErrorDialog(final String text) {
        JOptionPane.showMessageDialog(this, text, "Hiba", JOptionPane.ERROR_MESSAGE);
    }

    private static GridBagConstraints getConstraints(int gridx, int gridy) {
        return getConstraints(gridx, gridy, null);
    }

    private static GridBagConstraints getConstraints(int gridx, int gridy, Integer gridwidth) {
        final GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 0, 4, 4);  // padding
        gc.gridx = gridx;
        gc.gridy = gridy;
        if (gridwidth != null) {
            gc.gridwidth = gridwidth;
        }
        return gc;
    }

}
