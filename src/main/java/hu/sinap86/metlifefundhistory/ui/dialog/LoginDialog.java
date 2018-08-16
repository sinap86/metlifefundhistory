package hu.sinap86.metlifefundhistory.ui.dialog;

import hu.sinap86.metlifefundhistory.web.WebRequestManager;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.EtchedBorder;

public class LoginDialog extends BaseDialog {

    private final WebRequestManager webRequestManager;
    private final JTextField tfName;
    private final JPasswordField pfPassword;
    private final JTextField tfSmsOtp;
    private final JButton btnLogin;

    public LoginDialog(final JFrame owner, final WebRequestManager webRequestManager) {
        super(owner, "Bejelentkezés", true);
        this.webRequestManager = webRequestManager;
        final boolean authenticationWithPasswordSucceeded = webRequestManager.isAuthenticationWithPasswordSucceeded();

        final JPanel topPanel = new JPanel(new GridBagLayout());

        addLabel("Felhasználó név:", topPanel, 0, 0);

        tfName = addTextField(20, topPanel, 0, 1);
        tfName.setEnabled(!authenticationWithPasswordSucceeded);

        addLabel("Jelszó:", topPanel, 1, 0);

        pfPassword = new JPasswordField(20);
        pfPassword.setEnabled(!authenticationWithPasswordSucceeded);
        addComponent(pfPassword, topPanel, 1, 1);

        addLabel("SMS-ben kapott jelszó:", topPanel, 2, 0);

        tfSmsOtp = addTextField(20, topPanel, 2, 1);
        tfSmsOtp.setEnabled(authenticationWithPasswordSucceeded);

        btnLogin = new JButton(authenticationWithPasswordSucceeded ? "Bejelentkezés" : "Tovább a második lépéshez");
        btnLogin.addActionListener(new LoginButtonActionListener());

        final JButton btnCancel = new JButton("Mégse");
        btnCancel.addActionListener(event -> {
            setVisible(false);
            dispose();
        });

        topPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnCancel);

        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(buttonPanel, BorderLayout.CENTER);

        postConstruct(owner);
    }

    private class LoginButtonActionListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (webRequestManager.isAuthenticationWithPasswordSucceeded()) {
                // authentication step 1 (user name + password) succeeded
                authenticateWithSmsOtp();
            } else {
                // authentication step 1 (user name + password) failed or has not been performed
                authenticateWithUserNameAndPassword();
            }
        }

        private void authenticateWithUserNameAndPassword() {
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

        private void authenticateWithSmsOtp() {
            tfSmsOtp.requestFocus();
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
        }
    }
}
