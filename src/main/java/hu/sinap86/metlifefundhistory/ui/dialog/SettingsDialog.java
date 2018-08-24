package hu.sinap86.metlifefundhistory.ui.dialog;

import hu.sinap86.metlifefundhistory.config.ApplicationConfig;
import hu.sinap86.metlifefundhistory.ui.component.JNumberTextField;
import hu.sinap86.metlifefundhistory.util.Constants;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;

public class SettingsDialog extends BaseDialog {

    private JCheckBox chkUseProxy;
    private JTextField tfProxyHost;
    private JNumberTextField ntfProxyPort;
    private JComboBox<String> cbProxyScheme;

    public SettingsDialog(final Frame owner) {
        super(owner, "Beállítások", true);

        final boolean hasSavedProxySettings = ApplicationConfig.getInstance().isUseProxy();

        getContentPane().add(createProxyPanel(hasSavedProxySettings), BorderLayout.NORTH);
        getContentPane().add(createButtonPanel(), BorderLayout.CENTER);

        postConstruct(owner);
    }

    private JPanel createProxyPanel(final boolean hasSavedProxySettings) {
        final JPanel proxyPanel = new JPanel(new GridBagLayout());

        chkUseProxy = new JCheckBox("Proxy használat");
        chkUseProxy.setSelected(hasSavedProxySettings);
        addComponent(chkUseProxy, proxyPanel, 0, 0);

        addLabel("Hoszt:", proxyPanel, 1, 0);

        tfProxyHost = addTextField(20, proxyPanel, 1, 1);
        tfProxyHost.setEnabled(hasSavedProxySettings);
        tfProxyHost.setText(ApplicationConfig.getInstance().getProxyHost());
        addFocusTraversalKey(tfProxyHost, KeyEvent.VK_ENTER);

        addLabel("Port:", proxyPanel, 2, 0);

        ntfProxyPort = new JNumberTextField();
        ntfProxyPort.setEnabled(hasSavedProxySettings);
        ntfProxyPort.setNumber(ApplicationConfig.getInstance().getProxyPort());
        addFocusTraversalKey(ntfProxyPort, KeyEvent.VK_ENTER);
        addComponent(ntfProxyPort, proxyPanel, 2, 1);

        addLabel("Séma:", proxyPanel, 3, 0);

        cbProxyScheme = new JComboBox<>(Constants.SUPPORTED_PROXY_SCHEMES);
        cbProxyScheme.setSelectedIndex(0);
        cbProxyScheme.setEnabled(hasSavedProxySettings);
        cbProxyScheme.setSelectedItem(ApplicationConfig.getInstance().getProxyScheme());
        addFocusTraversalKey(cbProxyScheme, KeyEvent.VK_ENTER);
        addComponent(cbProxyScheme, proxyPanel, 3, 1);

        proxyPanel.setBorder(BorderFactory.createTitledBorder("Proxy beállítások"));

        chkUseProxy.addItemListener((ItemEvent event) -> {
            tfProxyHost.setEnabled(event.getStateChange() == ItemEvent.SELECTED);
            ntfProxyPort.setEnabled(event.getStateChange() == ItemEvent.SELECTED);
            cbProxyScheme.setEnabled(event.getStateChange() == ItemEvent.SELECTED);
            repaint();
        });

        return proxyPanel;
    }

    private JPanel createButtonPanel() {
        final JButton btnSave = new JButton("Mentés");
        btnSave.addActionListener(event -> {
            try {
                final boolean useProxy = chkUseProxy.isSelected();
                ApplicationConfig.getInstance().setUseProxy(useProxy);
                if (useProxy) {
                    ApplicationConfig.getInstance().setProxyHost(tfProxyHost.getText());
                    ApplicationConfig.getInstance().setProxyPort(ntfProxyPort.getNumber());
                    ApplicationConfig.getInstance().setProxyScheme((String) cbProxyScheme.getSelectedItem());
                }
                JOptionPane.showMessageDialog(SettingsDialog.this,
                                              "Sikeres mentés!",
                                              "Beállítások",
                                              JOptionPane.INFORMATION_MESSAGE);
            } catch (final Exception e) {
                showErrorDialog("Hiba történt a beállítások mentése során!");
            }
            setVisible(false);
            dispose();
        });

        final JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnSave);
        return buttonPanel;
    }

}
