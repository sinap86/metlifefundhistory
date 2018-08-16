package hu.sinap86.metlifefundhistory.ui.dialog;

import hu.sinap86.metlifefundhistory.config.ApplicationConfig;
import hu.sinap86.metlifefundhistory.ui.component.JNumberTextField;
import hu.sinap86.metlifefundhistory.util.Constants;

import java.awt.*;
import java.awt.event.ItemEvent;

import javax.swing.*;

public class SettingsDialog extends BaseDialog {

    public SettingsDialog(final Frame owner) {
        super(owner, "Beállítások", true);

        final boolean hasSavedProxySettings = ApplicationConfig.getInstance().isUseProxy();

        final JPanel proxyPanel = new JPanel(new GridBagLayout());

        final JCheckBox chkUseProxy = new JCheckBox("Proxy használat");
        chkUseProxy.setSelected(hasSavedProxySettings);
        addComponent(chkUseProxy, proxyPanel, 0, 0);

        addLabel("Hoszt:", proxyPanel, 1, 0);

        final JTextField tfProxyHost = addTextField(20, proxyPanel, 1, 1);
        tfProxyHost.setEnabled(hasSavedProxySettings);
        tfProxyHost.setText(ApplicationConfig.getInstance().getProxyHost());

        addLabel("Port:", proxyPanel, 2, 0);

        final JNumberTextField ntfProxyPort = new JNumberTextField();
        ntfProxyPort.setEnabled(hasSavedProxySettings);
        ntfProxyPort.setNumber(ApplicationConfig.getInstance().getProxyPort());
        addComponent(ntfProxyPort, proxyPanel, 2, 1);

        addLabel("Séma:", proxyPanel, 3, 0);

        final JComboBox<String> cbProxyScheme = new JComboBox<>(Constants.SUPPORTED_PROXY_SCHEMES);
        cbProxyScheme.setSelectedIndex(0);
        cbProxyScheme.setEnabled(hasSavedProxySettings);
        cbProxyScheme.setSelectedItem(ApplicationConfig.getInstance().getProxyScheme());
        addComponent(cbProxyScheme, proxyPanel, 3, 1);

        proxyPanel.setBorder(BorderFactory.createTitledBorder("Proxy beállítások"));

        chkUseProxy.addItemListener((ItemEvent event) -> {
            tfProxyHost.setEnabled(event.getStateChange() == ItemEvent.SELECTED);
            ntfProxyPort.setEnabled(event.getStateChange() == ItemEvent.SELECTED);
            cbProxyScheme.setEnabled(event.getStateChange() == ItemEvent.SELECTED);
            repaint();
        });

        final JButton btnSave = new JButton("Mentés");
        btnSave.addActionListener(event -> {
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
            setVisible(false);
            dispose();
        });

        final JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnSave);

        getContentPane().add(proxyPanel, BorderLayout.NORTH);
        getContentPane().add(buttonPanel, BorderLayout.CENTER);

        postConstruct(owner);
    }

}
