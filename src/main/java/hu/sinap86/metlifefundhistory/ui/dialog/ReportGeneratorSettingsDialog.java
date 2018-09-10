package hu.sinap86.metlifefundhistory.ui.dialog;

import hu.sinap86.metlifefundhistory.config.ReportGeneratorSettings;
import hu.sinap86.metlifefundhistory.ui.component.RateSettingsPanel;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import static hu.sinap86.metlifefundhistory.util.UIUtils.*;

public class ReportGeneratorSettingsDialog extends BaseDialog {

    private ReportGeneratorSettings querySettings;
    private File transactionHistoryDirectory;

    private final RateSettingsPanel rateSettingsPanel;
    private JButton btnChooseTransactionHistoryDirectory;

    public ReportGeneratorSettingsDialog(final JFrame owner) {
        super(owner, "Riport beállítások", true);

        rateSettingsPanel = new RateSettingsPanel();

        getContentPane().add(rateSettingsPanel, BorderLayout.NORTH);
        getContentPane().add(createHistoryDataPanel(), BorderLayout.CENTER);
        getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);

        postConstruct(owner);
    }

    private JPanel createHistoryDataPanel() {
        final JPanel historyDataPanel = new JPanel(new GridBagLayout());

        final JLabel lblWorkDirectory = addLabel("Adatok betöltése innen:", historyDataPanel, 0, 0);
        lblWorkDirectory.setPreferredSize(new Dimension(180, 26));

        final JTextField tfSelectedDirectory = addTextField(20, historyDataPanel, 0, 1);
        tfSelectedDirectory.setEnabled(false);

        btnChooseTransactionHistoryDirectory = new JButton("...");
        btnChooseTransactionHistoryDirectory.setEnabled(transactionHistoryDirectory == null);
        btnChooseTransactionHistoryDirectory.addActionListener(event -> {
            final File selectedDirectory = showFileChooser(this, "Könyvtár megnyitása", JFileChooser.DIRECTORIES_ONLY);
            if (selectedDirectory != null) {
                transactionHistoryDirectory = selectedDirectory;
                tfSelectedDirectory.setText(transactionHistoryDirectory.getAbsolutePath());
            }
        });
        addComponent(btnChooseTransactionHistoryDirectory, historyDataPanel, 0, 2);

        historyDataPanel.setBorder(BorderFactory.createTitledBorder("Befektetési alap tranzakció történet beállítások"));
        return historyDataPanel;
    }

    private JPanel createButtonPanel() {
        final JButton btnGenerate = new JButton("Generálás");
        btnGenerate.addActionListener(event -> {
            if (validateUserInput()) {
                querySettings = ReportGeneratorSettings.builder()
                        .useOnlineRates(rateSettingsPanel.useOnlineRates())
                        .rateDate(rateSettingsPanel.useOnlineRates() ? rateSettingsPanel.getRateDate() : null)
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
        buttonPanel.add(btnGenerate);
        buttonPanel.add(btnCancel);
        return buttonPanel;
    }

    private boolean validateUserInput() {
        final String rateSettingsErrorMessage = rateSettingsPanel.validateUserInputAndGetErrorMessage();
        if (StringUtils.isNotEmpty(rateSettingsErrorMessage)) {
            showErrorDialog(this, rateSettingsErrorMessage);
            return false;
        }
        if (transactionHistoryDirectory == null) {
            showErrorDialog(this, "Nincs tranzakciós adatokat tartalmazó könyvtár kiválasztva!");
            btnChooseTransactionHistoryDirectory.requestFocus();
            return false;
        }
        if (!transactionHistoryDirectory.canRead()) {
            showErrorDialog(this, "A kiválasztott tranzakciós adatokat tartalmazó könyvtár nem olvasható!");
            btnChooseTransactionHistoryDirectory.requestFocus();
            return false;
        }
        return true;
    }

    public ReportGeneratorSettings getSettings() {
        return querySettings;
    }
}
