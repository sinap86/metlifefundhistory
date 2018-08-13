package hu.sinap86.metlifefundhistory.ui;

import hu.sinap86.metlifefundhistory.config.ApplicationConfig;
import hu.sinap86.metlifefundhistory.ui.component.JNumberTextField;
import hu.sinap86.metlifefundhistory.util.Constants;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;

import javax.swing.*;

public class SettingsDialog extends JDialog {

    private final JCheckBox chkUseProxy;
    private final JLabel lblProxyHost;
    private final JTextField tfProxyHost;
    private final JLabel lbProxyPort;
    private final JNumberTextField ntfProxyPort;
    private final JLabel lbProxyScheme;
    private final JComboBox<String> cbProxyScheme;
    private final JButton btnSave;

    public SettingsDialog(Frame parent) {
        super(parent, "Beállítások", true);

        final boolean hasSavedProxySettings = ApplicationConfig.getInstance().isUseProxy();

        final JPanel proxyPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;

        chkUseProxy = new JCheckBox("Proxy használat");
        chkUseProxy.setSelected(hasSavedProxySettings);
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 2;
        proxyPanel.add(chkUseProxy, cs);

        lblProxyHost = new JLabel("Hoszt: ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        proxyPanel.add(lblProxyHost, cs);

        tfProxyHost = new JTextField(20);
        tfProxyHost.setEnabled(hasSavedProxySettings);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        proxyPanel.add(tfProxyHost, cs);

        lbProxyPort = new JLabel("Port: ");
        cs.gridx = 0;
        cs.gridy = 2;
        cs.gridwidth = 1;
        proxyPanel.add(lbProxyPort, cs);

        ntfProxyPort = new JNumberTextField();
        ntfProxyPort.setEnabled(hasSavedProxySettings);
        cs.gridx = 1;
        cs.gridy = 2;
        cs.gridwidth = 2;
        proxyPanel.add(ntfProxyPort, cs);

        lbProxyScheme = new JLabel("Séma: ");
        cs.gridx = 0;
        cs.gridy = 3;
        cs.gridwidth = 1;
        proxyPanel.add(lbProxyScheme, cs);

        cbProxyScheme = new JComboBox<>(Constants.SUPPORTED_PROXY_SCHEMES);
        cbProxyScheme.setSelectedIndex(0);
        cbProxyScheme.setEnabled(hasSavedProxySettings);
        cs.gridx = 1;
        cs.gridy = 3;
        cs.gridwidth = 2;
        proxyPanel.add(cbProxyScheme, cs);

        proxyPanel.setBorder(BorderFactory.createTitledBorder("Proxy beállítások"));

        if (hasSavedProxySettings) {
            tfProxyHost.setText(ApplicationConfig.getInstance().getProxyHost());
            ntfProxyPort.setNumber(ApplicationConfig.getInstance().getProxyPort());
            cbProxyScheme.setSelectedItem(ApplicationConfig.getInstance().getProxyScheme());
        }

        chkUseProxy.addItemListener((ItemEvent event) -> {
            tfProxyHost.setEnabled(event.getStateChange() == ItemEvent.SELECTED);
            ntfProxyPort.setEnabled(event.getStateChange() == ItemEvent.SELECTED);
            cbProxyScheme.setEnabled(event.getStateChange() == ItemEvent.SELECTED);
            repaint();
        });

        btnSave = new JButton("Mentés");
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

        getContentPane().add(proxyPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.PAGE_END);

        pack();
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
    }

    // override the createRootPane inherited by the JDialog, to create the rootPane.
    // create functionality to close the window when "Escape" button is pressed
    public JRootPane createRootPane() {
        JRootPane rootPane = new JRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
        Action action = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        };
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", action);
        return rootPane;
    }

}
