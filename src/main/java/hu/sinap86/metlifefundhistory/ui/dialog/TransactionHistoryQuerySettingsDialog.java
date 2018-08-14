package hu.sinap86.metlifefundhistory.ui.dialog;

import hu.sinap86.metlifefundhistory.web.WebRequestManager;

import com.github.lgooddatepicker.components.DatePicker;
import org.apache.commons.collections4.CollectionUtils;

import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.Collection;

import javax.swing.*;

public class TransactionHistoryQuerySettingsDialog extends BaseDialog {

    private final WebRequestManager webRequestManager;
    final JComboBox<String> cbContracts;
    final DatePicker dpFromDate;
    final DatePicker dpToDate;
    private final JTextField tfDirectory;
    private File selectedDirectory;

    public TransactionHistoryQuerySettingsDialog(final Frame owner, final WebRequestManager webRequestManager) {
        super(owner, "Lekérdezés beállítások", true);
        this.webRequestManager = webRequestManager;
        final Collection<String> contracts = webRequestManager.getUserContracts();

        final JPanel topPanel = new JPanel(new GridBagLayout());

        addLabel("Szerződések:", topPanel, 0, 0);

        cbContracts = new JComboBox<>(contracts.toArray(new String[]{}));
        cbContracts.setSelectedIndex(0);
        cbContracts.setEnabled(CollectionUtils.size(contracts) > 1);
        cbContracts.setPreferredSize(new Dimension(100, 26));
        addComponent(cbContracts, topPanel, 0, 1, 2);

        addLabel("Kezdő dátum:", topPanel, 1, 0);

        dpFromDate = createDatePicker();
        addComponent(dpFromDate, topPanel, 1, 1, 2);

        addLabel("Végdátum:", topPanel, 2, 0);

        dpToDate = createDatePicker();
        dpToDate.setDateToToday();
        addComponent(dpToDate, topPanel, 2, 1, 2);

        addLabel("Adatok mentése ide:", topPanel, 3, 0);

        tfDirectory = addTextField(20, topPanel, 3, 1);
        tfDirectory.setEnabled(false);
        tfDirectory.setPreferredSize(new Dimension(100, 26));

        final JButton btnChooseDirectory = new JButton("...");
        btnChooseDirectory.setPreferredSize(new Dimension(26, 26));
        btnChooseDirectory.addActionListener(event -> {
            showDirectoryChooser();
        });
        addComponent(btnChooseDirectory, topPanel, 3, 2);

        final JButton btnQuery = new JButton("Lekérdez");
        btnQuery.addActionListener(event -> {
            if (validateUserInput()) {
                // TODO create query settings object
                setVisible(false);
                dispose();
            }
        });

        final JButton btnCancel = new JButton("Mégse");
        btnCancel.addActionListener(event -> {
            setVisible(false);
            dispose();
        });

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnQuery);
        buttonPanel.add(btnCancel);

        getContentPane().add(topPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        postConstruct(owner);
    }

    private void showDirectoryChooser() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File("."));
        chooser.setDialogTitle("Könyvtár megnyitása");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedDirectory = chooser.getSelectedFile();
            tfDirectory.setText(selectedDirectory.getAbsolutePath());
        } else {
            tfDirectory.setText("");
        }
    }

    private boolean validateUserInput() {
        if (cbContracts.getSelectedItem() == null) {
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

}
