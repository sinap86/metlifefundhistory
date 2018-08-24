package hu.sinap86.metlifefundhistory.ui;

import hu.sinap86.metlifefundhistory.config.ReportGeneratorSettings;
import hu.sinap86.metlifefundhistory.config.TransactionHistoryQuerySettings;
import hu.sinap86.metlifefundhistory.report.FundReportGenerator;
import hu.sinap86.metlifefundhistory.ui.dialog.LoginDialog;
import hu.sinap86.metlifefundhistory.ui.dialog.ReportGeneratorSettingsDialog;
import hu.sinap86.metlifefundhistory.ui.dialog.SettingsDialog;
import hu.sinap86.metlifefundhistory.ui.dialog.TransactionHistoryQuerySettingsDialog;
import hu.sinap86.metlifefundhistory.util.Constants;
import hu.sinap86.metlifefundhistory.web.WebRequestManager;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

@Slf4j
public class ApplicationFrame extends JFrame {

    private final WebRequestManager webRequestManager = new WebRequestManager();

    public ApplicationFrame() {
        initUI();
    }

    private void initUI() {
        createMenuBar();
        createUsageDescription();

        setTitle("MetLife tranzakció történet elemző");
        setSize(500, 400);
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
            if (webRequestManager.isAuthenticated()) {
                webRequestManager.logout();
            }
            System.exit(0);
        }
    }

    private void createUsageDescription() {
        HTMLEditorKit kit = new HTMLEditorKit();
        Document doc = kit.createDefaultDocument();

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

        JScrollPane scrollPanel = new JScrollPane(descriptionEditorPane);
        getContentPane().add(scrollPanel, BorderLayout.CENTER);
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
                showReportGeneratorSettingsDialog(null);
            } catch (final Exception e) {
                showErrorDialog("Végzetes hiba történt!");
            }
        });

        JMenuItem queryAndCreateReportMenuItem = addMenuItem(reportMenu, "Online adatlekérdezés és riport generálás", KeyEvent.VK_O);
        queryAndCreateReportMenuItem.setToolTipText("Befektetési alap tranzakciós adatok lekérdezése, majd mentése a MetLife renszeréből valamint Excel riport készítése");
        queryAndCreateReportMenuItem.addActionListener(event -> {
            try {
                if (webRequestManager.isAuthenticated()) {
                    showTransactionHistoryQueryAndReportGeneratorSettingsDialog();
                } else {
                    final LoginDialog loginDialog = new LoginDialog(this, webRequestManager);
                    loginDialog.setVisible(true);

                    if (webRequestManager.isAuthenticated()) {
                        // TODO show user and contract information instead of UsageDescription

                        showTransactionHistoryQueryAndReportGeneratorSettingsDialog();
                    }
                }
            } catch (final Exception e) {
                showErrorDialog("Végzetes hiba történt!");
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

    private void showTransactionHistoryQueryAndReportGeneratorSettingsDialog() {
        final TransactionHistoryQuerySettingsDialog transactionHistoryQuerySettingsDialog = new TransactionHistoryQuerySettingsDialog(this, webRequestManager);
        transactionHistoryQuerySettingsDialog.setVisible(true);

        final TransactionHistoryQuerySettings querySettings = transactionHistoryQuerySettingsDialog.getSettings();
        if (querySettings != null) {
            // TODO download transaction history

            showReportGeneratorSettingsDialog(querySettings);
        }
    }

    private void showReportGeneratorSettingsDialog(final TransactionHistoryQuerySettings historyQuerySettings) {
        log.debug("historyQuerySettings: {}", historyQuerySettings);

        final ReportGeneratorSettingsDialog reportGeneratorSettingsDialog = new ReportGeneratorSettingsDialog(this, historyQuerySettings);
        reportGeneratorSettingsDialog.setVisible(true);
        final ReportGeneratorSettings reportGeneratorSettings = reportGeneratorSettingsDialog.getSettings();
        log.debug("reportGeneratorSettings: {}", reportGeneratorSettings);

        if (reportGeneratorSettings != null) {
            try {
                // TODO show process dialog
                final FundReportGenerator reportGenerator = new FundReportGenerator(reportGeneratorSettings);
                final File reportFile = reportGenerator.generate();

                showReportGeneratorResult(reportFile);
            } catch (IOException e) {
                log.error("Cannot generate fund report:", e);
                showErrorDialog("Hiba történt riport generálás során!");
            }
        }
    }

    private void showReportGeneratorResult(final File reportFile) throws IOException {
        if (reportFile == null) {
            showErrorDialog("Hiba történt riport generálás során!");
        }

        final String title = "Siker";
        final String message = String.format("Sikeres riport generálás: %s", reportFile.getAbsolutePath());

        if (Desktop.isDesktopSupported()) {
            final Object[] options = { "Megnyitás", "Ok" };
            final int result = JOptionPane.showOptionDialog(this,
                                                            message,
                                                            title,
                                                            JOptionPane.YES_NO_OPTION,
                                                            JOptionPane.INFORMATION_MESSAGE,
                                                            null,
                                                            options,
                                                            null);
            if (result == JOptionPane.YES_OPTION) {
                Desktop.getDesktop().open(reportFile);
            }
        } else {
            log.warn("Desktop not supported!");
            JOptionPane.showMessageDialog(this,
                                          message,
                                          title,
                                          JOptionPane.INFORMATION_MESSAGE);
        }
    }

    protected void showErrorDialog(final String text) {
        JOptionPane.showMessageDialog(this, text, "Hiba", JOptionPane.ERROR_MESSAGE);
    }

    private JMenuItem addMenuItem(final JMenu menu, final String text, final int mnemonic) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setMnemonic(mnemonic);
        menu.add(menuItem);
        return menuItem;
    }

}
