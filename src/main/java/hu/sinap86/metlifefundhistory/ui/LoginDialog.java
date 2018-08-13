package hu.sinap86.metlifefundhistory.ui;

import hu.sinap86.metlifefundhistory.web.WebRequestManager;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.EtchedBorder;

public class LoginDialog extends JDialog {

    private final WebRequestManager webRequestManager;
    private final JTextField tfName;
    private final JPasswordField pfPassword;
    private final JTextField tfSmsOtp;
    private final JButton btnLogin;

    public LoginDialog(final JFrame parent, final WebRequestManager webRequestManager) {
        super(parent, "Bejelentkezés", true);
        this.webRequestManager = webRequestManager;
        final boolean authenticationWithPasswordSucceeded = webRequestManager.isAuthenticationWithPasswordSucceeded();

        JPanel topPanel = new JPanel(new GridBagLayout());

        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblName = new JLabel("Felhasználó név: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        topPanel.add(lblName, cs);

        tfName = new JTextField(20);
        tfName.setEnabled(!authenticationWithPasswordSucceeded);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        topPanel.add(tfName, cs);

        JLabel lblPassword = new JLabel("Jelszó: ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        topPanel.add(lblPassword, cs);

        pfPassword = new JPasswordField(20);
        pfPassword.setEnabled(!authenticationWithPasswordSucceeded);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        topPanel.add(pfPassword, cs);

        JLabel lblSmsOtp = new JLabel("SMS-ben kapott jelszó: ");
        cs.gridx = 0;
        cs.gridy = 2;
        cs.gridwidth = 1;
        topPanel.add(lblSmsOtp, cs);

        tfSmsOtp = new JTextField(20);
        tfSmsOtp.setEnabled(authenticationWithPasswordSucceeded);
        cs.gridx = 1;
        cs.gridy = 2;
        cs.gridwidth = 2;
        topPanel.add(tfSmsOtp, cs);

        btnLogin = new JButton(authenticationWithPasswordSucceeded ? "Bejelentkezés" : "Tovább a második lépéshez");
        btnLogin.addActionListener(new LoginButtonActionListener());

        JButton btnCancel = new JButton("Mégse");
        btnCancel.addActionListener(event -> {
            setVisible(false);
            dispose();
        });

        topPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnCancel);
        getContentPane().add(topPanel);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
    }

    private class LoginButtonActionListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            // authentication step 1 succeeded
            if (webRequestManager.isAuthenticationWithPasswordSucceeded()) {
                final String smsOtp = tfSmsOtp.getText();
                if (StringUtils.isEmpty(smsOtp)) {
                    showErrorDialog("Nem adott meg SMS kódot!");
                    return;
                }

                final boolean success = webRequestManager.authenticate(smsOtp);
                if (success) {
                    setVisible(false);
                    dispose();
                } else {
                    showErrorDialog("Hibás SMS kód!");
                }
                // authentication step 1 failed
            } else {
                final String userName = tfName.getText();
                final char[] password = pfPassword.getPassword();
                if (StringUtils.isEmpty(userName)) {
                    showErrorDialog("Nem adott meg felhasználó nevet!");
                    return;
                }
                if (ArrayUtils.isEmpty(password)) {
                    showErrorDialog("Nem adott meg jelszót!");
                    return;
                }

                final boolean success = webRequestManager.authenticate(userName, password);
                if (success) {
                    btnLogin.setText("Bejelentkezés");
                    tfName.setEnabled(false);
                    pfPassword.setEnabled(false);
                    tfSmsOtp.setEnabled(true);
                } else {
                    showErrorDialog("Hibás felhasználónév vagy jelszó!");
                }

            }
        }

        private void showErrorDialog(final String text) {
            JOptionPane.showMessageDialog(LoginDialog.this, text, "Hiba", JOptionPane.ERROR_MESSAGE);
        }
    }
}
