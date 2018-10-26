package hu.sinap86.metlifefundhistory.ui;

import static hu.sinap86.metlifefundhistory.util.UIUtils.addComponent;
import static hu.sinap86.metlifefundhistory.util.UIUtils.addLabel;
import static hu.sinap86.metlifefundhistory.util.UIUtils.showErrorDialog;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.config.ReportGeneratorSettings;
import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.report.FundReportGenerator;
import hu.sinap86.metlifefundhistory.ui.component.IndeterminateProgressMonitor;
import hu.sinap86.metlifefundhistory.ui.component.LoginInfoPanel;
import hu.sinap86.metlifefundhistory.ui.dialog.LoginDialog;
import hu.sinap86.metlifefundhistory.ui.dialog.ReportGeneratorSettingsDialog;
import hu.sinap86.metlifefundhistory.ui.dialog.SettingsDialog;
import hu.sinap86.metlifefundhistory.ui.dialog.TransactionHistoryQuerySettingsDialog;
import hu.sinap86.metlifefundhistory.web.TransactionDataDownloader;
import hu.sinap86.metlifefundhistory.web.session.MetLifeWebSessionManager;
import hu.sinap86.metlifefundhistory.web.session.WebSessionManager;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

@Slf4j
public class ApplicationFrame extends JFrame {

    private final WebSessionManager webSessionManager = new MetLifeWebSessionManager();

    private JComponent usageDescriptionPanel;

    public ApplicationFrame() {
        initUI();
    }

    private void initUI() {
        createMenuBar();
        usageDescriptionPanel = createUsageDescription();

        setTitle("MetLife tranzakció történet elemző");
        setSize(550, 400);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                logoutAndClose();
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void logoutAndClose() {
        if (JOptionPane.showConfirmDialog(this,
                                          "Biztosan kilép?", "Kilépés",
                                          JOptionPane.YES_NO_OPTION,
                                          JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            try {
                if (webSessionManager.isAuthenticated()) {
                    webSessionManager.logout();
                }
            } catch (Exception e) {
                log.error("Error occured during logout:", e);
            }
            System.exit(0);
        }
    }

    private JComponent createUsageDescription() {
        final HTMLEditorKit kit = new HTMLEditorKit();
        final Document doc = kit.createDefaultDocument();

        final JEditorPane descriptionEditorPane = new JEditorPane();
        descriptionEditorPane.setDocument(doc);
        descriptionEditorPane.setEditorKit(kit);
        try {
            descriptionEditorPane.read(ApplicationFrame.class.getClassLoader().getResourceAsStream(Constants.USAGE_DESCRIPTION_FILE), null);
        } catch (IOException e) {
            log.error("Cannot load usage description from " + Constants.USAGE_DESCRIPTION_FILE, e);
        }
        descriptionEditorPane.setEditable(false);
        descriptionEditorPane.setContentType("text/html");

        final JScrollPane scrollPane = new JScrollPane(descriptionEditorPane);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        return scrollPane;
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Fájl");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem settingsMenuItem = addMenuItem(fileMenu, "Beállítások", KeyEvent.VK_B);
        settingsMenuItem.addActionListener(event -> {
            final SettingsDialog settingsDialog = new SettingsDialog(this);
            settingsDialog.setVisible(true);
        });

        JMenuItem exitMenuItem = addMenuItem(fileMenu, "Kilépés", KeyEvent.VK_K);
        exitMenuItem.addActionListener(event -> {
            logoutAndClose();
        });

        JMenu reportMenu = new JMenu("Riport");
        reportMenu.setMnemonic(KeyEvent.VK_R);

        JMenuItem createReportMenuItem = addMenuItem(reportMenu, "Riport generálás korábbi adatokból", KeyEvent.VK_G);
        createReportMenuItem.setToolTipText("Korábban letöltött befektetési alap tranzakciós adatok feldolgozása és Excel riport készítése");
        createReportMenuItem.addActionListener(event -> {
            try {
                showReportGeneratorSettingsDialog();
            } catch (Exception e) {
                log.error("Fatal error during report generation:", e);
                showErrorDialog(this, "Végzetes hiba történt!");
            }
        });

        JMenuItem queryAndCreateReportMenuItem = addMenuItem(reportMenu, "Online adatlekérdezés és riport generálás", KeyEvent.VK_O);
        queryAndCreateReportMenuItem.setToolTipText("Befektetési alap tranzakciós adatok lekérdezése, majd mentése a MetLife renszeréből valamint Excel riport készítése");
        queryAndCreateReportMenuItem.addActionListener(event -> {
            try {
                if (webSessionManager.isAuthenticated()) {
                    showTransactionHistoryQueryAndReportGeneratorSettingsDialog();
                } else {
                    final LoginDialog loginDialog = new LoginDialog(this, webSessionManager);
                    loginDialog.setVisible(true);

                    if (webSessionManager.isAuthenticated()) {
                        swapUsageDescriptionToLoginInfo();

                        showTransactionHistoryQueryAndReportGeneratorSettingsDialog();
                    }
                }
            } catch (Exception e) {
                log.error("Fatal error during query report data and report generation:", e);
                showErrorDialog(this, "Végzetes hiba történt!");
            }
        });

        JMenu helpMenu = new JMenu("Súgó");
        JMenuItem aboutMenuItem = addMenuItem(helpMenu, "Névjegy", KeyEvent.VK_N);
        aboutMenuItem.addActionListener(event -> {
            JOptionPane optionPane = new JOptionPane();
            optionPane.setMessage("<html>MetLifeFundHistory v1.0<br>Készítette: Sinka László<br>Email: <a href=\"mailto:sinap86@gmail.com\">sinap86@gmail.com</a></html>");
            optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
            final JDialog aboutDialog = optionPane.createDialog(this, "Névjegy");
            aboutDialog.setVisible(true);
        });

        menuBar.add(fileMenu);
        menuBar.add(reportMenu);
        // align Help menu to the right
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void swapUsageDescriptionToLoginInfo() {
        final LoginInfoPanel loginInfoPanel = new LoginInfoPanel(webSessionManager);

        remove(usageDescriptionPanel);
        add(loginInfoPanel);
        invalidate();
        validate();
    }

    private void showTransactionHistoryQueryAndReportGeneratorSettingsDialog() {
        final TransactionHistoryQuerySettingsDialog transactionHistoryQuerySettingsDialog = new TransactionHistoryQuerySettingsDialog(this, webSessionManager);
        transactionHistoryQuerySettingsDialog.setVisible(true);

        final TransactionHistoryQuerySettings querySettings = transactionHistoryQuerySettingsDialog.getSettings();
        log.debug("historyQuerySettings: {}", querySettings);

        if (querySettings == null) {
            return;
        }

        downloadTransactionHistoryAndGenerateReport(querySettings);
    }

    private void downloadTransactionHistoryAndGenerateReport(final TransactionHistoryQuerySettings querySettings) {
        try {
            UIManager.put("ProgressMonitor.progressText", "Online adatletöltés");
            UIManager.put("OptionPane.cancelButtonText", "Mégse");
            final ProgressMonitor progressMonitor = new ProgressMonitor(this, null,
                                                                        "Tranzakciós lista lekérdezése . . .",
                                                                        0, 100);
            progressMonitor.setMillisToDecideToPopup(0);
            progressMonitor.setProgress(0);

            final TransactionDataDownloader downloader = new TransactionDataDownloader(webSessionManager, querySettings);

            final PropertyChangeListener downloadProgressListener = evt -> {
                if (progressMonitor.isCanceled()) {
                    downloader.cancel(true);
                    return;
                }

                final String propertyName = evt.getPropertyName();
                final Object newValue = evt.getNewValue();
                if ("progress".equals(propertyName)) {
                    final int progress = (Integer) newValue;
                    progressMonitor.setProgress(progress);
                    progressMonitor.setNote(String.format("Tranzakciók letöltve: %d%%.\n", progress));
                }
                if ("state".equals(propertyName) && SwingWorker.StateValue.DONE.equals(newValue)) {
                    if (downloader.isSuccess()) {
                        generateReport(querySettings);
                    } else {
                        showErrorDialog(this, "Sikertelen Online adatlekérdezés!");
                    }
                }
            };

            downloader.addPropertyChangeListener(downloadProgressListener);
            downloader.execute();
        } catch (Exception e) {
            log.error("Cannot download transaction data:", e);
            showErrorDialog(this, "Sikertelen Online adatlekérdezés!");
        }
    }

    private void showReportGeneratorSettingsDialog() {
        final ReportGeneratorSettingsDialog reportGeneratorSettingsDialog = new ReportGeneratorSettingsDialog(this);
        reportGeneratorSettingsDialog.setVisible(true);

        final ReportGeneratorSettings reportGeneratorSettings = reportGeneratorSettingsDialog.getSettings();
        log.debug("reportGeneratorSettings: {}", reportGeneratorSettings);

        if (reportGeneratorSettings == null) {
            return;
        }

        generateReport(reportGeneratorSettings);
    }

    private void generateReport(final ReportGeneratorSettings settings) {
        try {
            UIManager.put("ProgressMonitor.progressText", "Riport generálás");
            UIManager.put("OptionPane.cancelButtonText", "Mégse");
            final IndeterminateProgressMonitor progressMonitor = new IndeterminateProgressMonitor(this, null,
                                                                                                  "Tranzakciós adatok feldolgozása . . .");

            final FundReportGenerator reportGenerator = new FundReportGenerator(settings);

            final PropertyChangeListener generatorProgressListener = evt -> {
                if (progressMonitor.isCanceled()) {
                    reportGenerator.cancel(true);
                    return;
                }

                final String propertyName = evt.getPropertyName();
                final Object newValue = evt.getNewValue();
                if ("progress".equals(propertyName)) {
                    final int progress = (Integer) newValue;
                    progressMonitor.setProgress(progress);
                    if (progress == FundReportGenerator.TRANSACTION_LIST_PARSED_PROGRESS) {
                        progressMonitor.setNote(settings.isUseOnlineRates() ? "Online árfolyamok letöltése  . . ."
                                                                            : "Árfolyam fájl feldolgozása  . . .");
                    }
                    if (progress == FundReportGenerator.RATES_PROVIDED_PROGRESS) {
                        progressMonitor.setNote("Riport generálás . . .");
                    }
                }
                if ("state".equals(propertyName) && SwingWorker.StateValue.DONE.equals(newValue)) {
                    showReportGeneratorResult(reportGenerator.getResult());
                }
            };

            reportGenerator.addPropertyChangeListener(generatorProgressListener);
            reportGenerator.execute();
        } catch (Exception e) {
            log.error("Cannot generate fund report:", e);
            showErrorDialog(this, "Hiba történt riport generálás során!");
        }
    }

    private void showReportGeneratorResult(final FundReportGenerator.Result generatorResult) {
        if (generatorResult == null || generatorResult.getReportFile() == null) {
            showErrorDialog(this, "Hiba történt riport generálás során!");
            return;
        }

        final File reportFile = generatorResult.getReportFile();
        final List<String> warnings = generatorResult.getWarnings();

        final Object message;
        final int messageType;
        if (CollectionUtils.isEmpty(warnings)) {
            message = String.format("Sikeres riport generálás: %s", reportFile.getAbsolutePath());
            messageType = JOptionPane.INFORMATION_MESSAGE;
        } else {
            final JPanel customPanel = new JPanel(new GridBagLayout());
            addLabel(String.format("Riport generálás figyelmeztetésekkel lezárult:\n %s",
                                   reportFile.getAbsolutePath()),
                     customPanel, 0, 0);
            final JTextArea textArea = new JTextArea(6, 50);
            textArea.setEditable(false);
            textArea.setFont(textArea.getFont().deriveFont(12f));
            for (String warning : warnings) {
                textArea.append(warning);
                textArea.append("\n");
            }
            addComponent(new JScrollPane(textArea), customPanel, 1, 0);

            message = customPanel;
            messageType = JOptionPane.WARNING_MESSAGE;
        }

        final boolean desktopSupported = Desktop.isDesktopSupported();
        final Object[] options = desktopSupported ? new Object[]{ "Megnyitás", "Ok" } : new Object[]{ "Ok" };
        final int buttonType = desktopSupported ? JOptionPane.YES_NO_OPTION : JOptionPane.YES_OPTION;

        final int result = JOptionPane.showOptionDialog(this,
                                                        message,
                                                        "Riport generálás",
                                                        buttonType,
                                                        messageType,
                                                        null,
                                                        options,
                                                        null);

        if (result == JOptionPane.YES_OPTION && desktopSupported) {
            try {
                Desktop.getDesktop().open(reportFile);
            } catch (IOException e) {
                log.error("Cannot open report file:", e);
            }
        }
    }

    private JMenuItem addMenuItem(final JMenu menu, final String text, final int mnemonic) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setMnemonic(mnemonic);
        menu.add(menuItem);
        return menuItem;
    }

}
