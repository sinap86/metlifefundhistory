package hu.sinap86.metlifefundhistory.ui.dialog;

import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.web.WebRequestManager;

import com.github.lgooddatepicker.components.DatePicker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.Collection;

import javax.swing.*;

public class TransactionHistoryQuerySettingsDialog extends BaseDialog {

    private TransactionHistoryQuerySettings querySettings;

    final JComboBox<String> cbContracts;
    final DatePicker dpFromDate;
    final DatePicker dpToDate;
    private final JTextField tfSelectedDirectory;
    private File transactionHistoryDirectory;

    public TransactionHistoryQuerySettingsDialog(final Frame owner, final WebRequestManager webRequestManager) {
        super(owner, "Lekérdezés beállítások", true);
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

        tfSelectedDirectory = addTextField(20, topPanel, 3, 1);
        tfSelectedDirectory.setEnabled(false);
        tfSelectedDirectory.setPreferredSize(new Dimension(100, 26));

        final JButton btnChooseDirectory = new JButton("...");
        btnChooseDirectory.setPreferredSize(new Dimension(26, 26));
        btnChooseDirectory.addActionListener(event -> {
            transactionHistoryDirectory = showFileChooser("Könyvtár megnyitása", JFileChooser.DIRECTORIES_ONLY);
            if (transactionHistoryDirectory != null) {
                tfSelectedDirectory.setText(transactionHistoryDirectory.getAbsolutePath());
            } else {
                tfSelectedDirectory.setText(StringUtils.EMPTY);
            }
        });
        addComponent(btnChooseDirectory, topPanel, 3, 2);

        final JButton btnQuery = new JButton("Lekérdez");
        btnQuery.addActionListener(event -> {
            if (validateUserInput()) {
                querySettings = TransactionHistoryQuerySettings.builder()
                        .contract((String) cbContracts.getSelectedItem())
                        .fromDate(dpFromDate.getDate())
                        .toDate(dpToDate.getDate())
                        .transactionHistoryDirectory(transactionHistoryDirectory)
                        .build();

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

        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(buttonPanel, BorderLayout.CENTER);

        postConstruct(owner);
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
        if (transactionHistoryDirectory == null) {
            showErrorDialog("Nincs adat mentési könyvtár kiválasztva!");
            return false;
        }
        if (!transactionHistoryDirectory.canWrite()) {
            showErrorDialog("A kiválasztott adat mentési könyvtár nem írható!");
            return false;
        }
        return true;
    }

    public TransactionHistoryQuerySettings getSettings() {
        return querySettings;
    }
}
