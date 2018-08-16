package hu.sinap86.metlifefundhistory.ui.dialog;

import hu.sinap86.metlifefundhistory.config.ReportGeneratorSettings;
import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;

import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;

import javax.swing.*;

public class ReportGeneratorSettingsDialog extends BaseDialog {

    private ReportGeneratorSettings querySettings;

    private File rateFile;
    private File transactionHistoryDirectory;

    private JCheckBox chkUseOnlineRates;
    private JButton btnChooseTransactionHistoryDirectory;
    private JButton btnChooseRateFile;

    public ReportGeneratorSettingsDialog(final JFrame owner, final TransactionHistoryQuerySettings historyQuerySettings) {
        super(owner, "Riport beállítások", true);

        getContentPane().add(createRatePanel(), BorderLayout.NORTH);
        if (historyQuerySettings == null) {
            getContentPane().add(createHistoryDataPanel(), BorderLayout.CENTER);
        } else {
            // TODO esetleg + subdirectory, bár ez a mentéstől függ majd
            transactionHistoryDirectory = historyQuerySettings.getTransactionHistoryDirectory();
        }
        getContentPane().add(createButtonPanel(), BorderLayout.SOUTH);

        postConstruct(owner);
    }

    private JPanel createRatePanel() {
        final JPanel ratePanel = new JPanel(new GridBagLayout());

        chkUseOnlineRates = new JCheckBox("Online árfolyamok aktív alapoknál");
        addComponent(chkUseOnlineRates, ratePanel, 0, 0, 3);

        final JLabel lblRateFile = addLabel("Árfolyamok betöltése innen:", ratePanel, 1, 0);
        lblRateFile.setPreferredSize(new Dimension(180, 26));

        final JTextField tfRateFile = addTextField(20, ratePanel, 1, 1);
        tfRateFile.setEnabled(false);

        btnChooseRateFile = new JButton("...");
        btnChooseRateFile.addActionListener(event -> {
            rateFile = showFileChooser("Fájl megnyitása", JFileChooser.FILES_ONLY);
            if (rateFile != null) {
                tfRateFile.setText(rateFile.getAbsolutePath());
            } else {
                tfRateFile.setText(StringUtils.EMPTY);
            }
        });
        addComponent(btnChooseRateFile, ratePanel, 1, 2);

        chkUseOnlineRates.addItemListener((ItemEvent event) -> {
            btnChooseRateFile.setEnabled(event.getStateChange() == ItemEvent.DESELECTED);
            repaint();
        });

        ratePanel.setBorder(BorderFactory.createTitledBorder("Árfolyam beállítások"));
        return ratePanel;
    }

    private JPanel createHistoryDataPanel() {
        final JPanel historyDataPanel = new JPanel(new GridBagLayout());

        final JLabel lblWorkDirectory = addLabel("Adatok betöltése innen:", historyDataPanel, 0, 0);
        lblWorkDirectory.setPreferredSize(new Dimension(180, 26));

        final JTextField tfSelectedDirectory = addTextField(20, historyDataPanel, 0, 1);
        tfSelectedDirectory.setEnabled(false);
        if (transactionHistoryDirectory != null) {
            tfSelectedDirectory.setText(transactionHistoryDirectory.getAbsolutePath());
        }

        btnChooseTransactionHistoryDirectory = new JButton("...");
        btnChooseTransactionHistoryDirectory.setEnabled(transactionHistoryDirectory == null);
        btnChooseTransactionHistoryDirectory.addActionListener(event -> {
            transactionHistoryDirectory = showFileChooser("Könyvtár megnyitása", JFileChooser.DIRECTORIES_ONLY);
            if (transactionHistoryDirectory != null) {
                tfSelectedDirectory.setText(transactionHistoryDirectory.getAbsolutePath());
            } else {
                tfSelectedDirectory.setText(StringUtils.EMPTY);
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
                        .useOnlineRates(chkUseOnlineRates.isSelected())
                        .rateFile(rateFile)
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
        if (!chkUseOnlineRates.isSelected() && rateFile == null) {
            showErrorDialog("Nincs árfolyam fájl kiválasztva!");
            btnChooseRateFile.requestFocus();
            return false;
        }
        if (rateFile != null && !rateFile.canRead()) {
            showErrorDialog("A kiválasztott árfolyam fájl nem olvasható!");
            btnChooseRateFile.requestFocus();
            return false;
        }
        if (transactionHistoryDirectory == null) {
            showErrorDialog("Nincs tranzakciós adatokat tartalmazó könyvtár kiválasztva!");
            btnChooseTransactionHistoryDirectory.requestFocus();
            return false;
        }
        if (!transactionHistoryDirectory.canRead()) {
            showErrorDialog("A kiválasztott tranzakciós adatokat tartalmazó könyvtár nem olvasható!");
            btnChooseTransactionHistoryDirectory.requestFocus();
            return false;
        }
        return true;
    }

    public ReportGeneratorSettings getSettings() {
        return querySettings;
    }
}
