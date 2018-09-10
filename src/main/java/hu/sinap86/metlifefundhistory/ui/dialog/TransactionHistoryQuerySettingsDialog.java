package hu.sinap86.metlifefundhistory.ui.dialog;

import static hu.sinap86.metlifefundhistory.util.UIUtils.addComponent;
import static hu.sinap86.metlifefundhistory.util.UIUtils.addLabel;
import static hu.sinap86.metlifefundhistory.util.UIUtils.addTextField;
import static hu.sinap86.metlifefundhistory.util.UIUtils.createDatePicker;
import static hu.sinap86.metlifefundhistory.util.UIUtils.showErrorDialog;
import static hu.sinap86.metlifefundhistory.util.UIUtils.showFileChooser;

import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.model.Contract;
import hu.sinap86.metlifefundhistory.ui.component.RateSettingsPanel;
import hu.sinap86.metlifefundhistory.web.session.WebSessionManager;

import com.github.lgooddatepicker.components.DatePicker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.Set;

import javax.swing.*;

public class TransactionHistoryQuerySettingsDialog extends BaseDialog {

    private TransactionHistoryQuerySettings querySettings;
    private File transactionHistoryDirectory;

    private final RateSettingsPanel rateSettingsPanel;
    private JComboBox<String> cbContracts;
    private DatePicker dpFromDate;
    private DatePicker dpToDate;
    private JTextField tfSelectedDirectory;

    public TransactionHistoryQuerySettingsDialog(final Frame owner, final WebSessionManager webSessionManager) {
        super(owner, "Lekérdezés beállítások", true);

        final Set<Contract> contracts = webSessionManager.getUserContracts();

        rateSettingsPanel = new RateSettingsPanel();

        getContentPane().add(createQuerySettingsPanel(contracts), BorderLayout.NORTH);
        getContentPane().add(rateSettingsPanel, BorderLayout.CENTER);
        getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);

        postConstruct(owner);
    }

    private JPanel createQuerySettingsPanel(final Set<Contract> contracts) {
        final JPanel querySettingsPanel = new JPanel(new GridBagLayout());

        addLabel("Szerződések:", querySettingsPanel, 0, 0);

        final String[] contractIds = contracts.stream().map(Contract::getId).toArray(String[]::new);
        cbContracts = new JComboBox<>(contractIds);
        cbContracts.setSelectedIndex(0);
        cbContracts.setEnabled(CollectionUtils.size(contracts) > 1);
        cbContracts.setPreferredSize(new Dimension(100, 26));
        addComponent(cbContracts, querySettingsPanel, 0, 1, 2);

        addLabel("Kezdő dátum:", querySettingsPanel, 1, 0);

        dpFromDate = createDatePicker();
        addComponent(dpFromDate, querySettingsPanel, 1, 1, 2);

        addLabel("Végdátum:", querySettingsPanel, 2, 0);

        dpToDate = createDatePicker();
        dpToDate.setDateToToday();
        addComponent(dpToDate, querySettingsPanel, 2, 1, 2);

        addLabel("Adatok mentése ide:", querySettingsPanel, 3, 0);

        tfSelectedDirectory = addTextField(20, querySettingsPanel, 3, 1);
        tfSelectedDirectory.setEnabled(false);
        tfSelectedDirectory.setPreferredSize(new Dimension(100, 26));

        final JButton btnChooseDirectory = new JButton("...");
        btnChooseDirectory.setPreferredSize(new Dimension(26, 26));
        btnChooseDirectory.addActionListener(event -> {
            final File selectedDirectory = showFileChooser(this, "Könyvtár megnyitása", JFileChooser.DIRECTORIES_ONLY);
            if (selectedDirectory != null) {
                transactionHistoryDirectory = selectedDirectory;
                tfSelectedDirectory.setText(transactionHistoryDirectory.getAbsolutePath());
            }
        });
        addComponent(btnChooseDirectory, querySettingsPanel, 3, 2);

        querySettingsPanel.setBorder(BorderFactory.createTitledBorder("Lekérdezés beállítások"));
        return querySettingsPanel;
    }

    private JPanel createButtonPanel() {
        final JButton btnQuery = new JButton("Lekérdez");
        btnQuery.addActionListener(event -> {
            if (validateUserInput()) {
                querySettings = TransactionHistoryQuerySettings.builder()
                        .contract((String) cbContracts.getSelectedItem())
                        .fromDate(dpFromDate.getDate())
                        .toDate(dpToDate.getDate())
                        .useOnlineRates(rateSettingsPanel.useOnlineRates())
                        .rateFile(rateSettingsPanel.getRateFile())
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
        return buttonPanel;
    }

    private boolean validateUserInput() {
        if (cbContracts.getSelectedItem() == null) {
            showErrorDialog(this, "Nincs szerződés kiválasztva!");
            return false;
        }
        final LocalDate fromDate = dpFromDate.getDate();
        if (fromDate == null) {
            showErrorDialog(this, "Nincs kezdő dátum kiválasztva!");
            return false;
        }
        final LocalDate toDate = dpToDate.getDate();
        if (toDate == null) {
            showErrorDialog(this, "Nincs végdátum kiválasztva!");
            return false;
        }
        if (fromDate.isAfter(toDate)) {
            showErrorDialog(this, "Vég dátum nem lehet a kezdő dátum előtti!");
            return false;
        }
        if (transactionHistoryDirectory == null) {
            showErrorDialog(this, "Nincs adat mentési könyvtár kiválasztva!");
            return false;
        }
        if (!transactionHistoryDirectory.canWrite()) {
            showErrorDialog(this, "A kiválasztott adat mentési könyvtár nem írható!");
            return false;
        }
        final String rateSettingsErrorMessage = rateSettingsPanel.validateUserInputAndGetErrorMessage();
        if (StringUtils.isNotEmpty(rateSettingsErrorMessage)) {
            showErrorDialog(this, rateSettingsErrorMessage);
            return false;
        }
        return true;
    }

    public TransactionHistoryQuerySettings getSettings() {
        return querySettings;
    }
}
