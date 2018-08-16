package hu.sinap86.metlifefundhistory.ui.dialog;

import hu.sinap86.metlifefundhistory.web.WebRequestManager;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.*;
import javax.swing.border.EtchedBorder;

public class LoginDialog extends BaseDialog {

    private final WebRequestManager webRequestManager;
    private JTextField tfName;
    private JPasswordField pfPassword;
    private JTextField tfSmsOtp;
    private JButton btnLogin;

    public LoginDialog(final JFrame owner, final WebRequestManager webRequestManager) {
        super(owner, "Bejelentkezés", true);
        this.webRequestManager = webRequestManager;

        final boolean authenticationWithPasswordSucceeded = webRequestManager.isAuthenticationWithPasswordSucceeded();

        getContentPane().add(createPasswordPanel(authenticationWithPasswordSucceeded), BorderLayout.NORTH);
        getContentPane().add(createButtonPanel(authenticationWithPasswordSucceeded), BorderLayout.CENTER);

        postConstruct(owner);
    }

    private JPanel createPasswordPanel(final boolean authenticationWithPasswordSucceeded) {
        final JPanel passwordPanel = new JPanel(new GridBagLayout());

        addLabel("Felhasználó név:", passwordPanel, 0, 0);

        tfName = addTextField(20, passwordPanel, 0, 1);
        tfName.setEnabled(!authenticationWithPasswordSucceeded);
        addFocusTraversalKey(tfName, KeyEvent.VK_ENTER);

        addLabel("Jelszó:", passwordPanel, 1, 0);

        pfPassword = new JPasswordField(20);
        pfPassword.setEnabled(!authenticationWithPasswordSucceeded);
        addFocusTraversalKey(pfPassword, KeyEvent.VK_ENTER);
        addComponent(pfPassword, passwordPanel, 1, 1);

        addLabel("SMS-ben kapott jelszó:", passwordPanel, 2, 0);

        tfSmsOtp = addTextField(20, passwordPanel, 2, 1);
        tfSmsOtp.setEnabled(authenticationWithPasswordSucceeded);
        addFocusTraversalKey(tfSmsOtp, KeyEvent.VK_ENTER);

        passwordPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        return passwordPanel;
    }

    private JPanel createButtonPanel(final boolean authenticationWithPasswordSucceeded) {
        btnLogin = new JButton(authenticationWithPasswordSucceeded ? "Bejelentkezés" : "Tovább a második lépéshez");
        btnLogin.addActionListener(new LoginButtonActionListener());

        final JButton btnCancel = new JButton("Mégse");
        btnCancel.addActionListener(event -> {
            setVisible(false);
            dispose();
        });

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnCancel);
        return buttonPanel;
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
                tfName.requestFocus();
                return;
            }
            if (ArrayUtils.isEmpty(password)) {
                pfPassword.requestFocus();
                showErrorDialog("Nem adott meg jelszót!");
                return;
            }

            final boolean success = webRequestManager.authenticate(userName, password);
            if (success) {
                btnLogin.setText("Bejelentkezés");
                tfName.setEnabled(false);
                pfPassword.setEnabled(false);
                tfSmsOtp.setEnabled(true);
                tfSmsOtp.requestFocus();
            } else {
                showErrorDialog("Hibás felhasználónév vagy jelszó!");
                pfPassword.requestFocus();
            }
        }

        private void authenticateWithSmsOtp() {
            final String smsOtp = tfSmsOtp.getText();
            if (StringUtils.isEmpty(smsOtp)) {
                showErrorDialog("Nem adott meg SMS kódot!");
                tfSmsOtp.requestFocus();
                return;
            }

            final boolean success = webRequestManager.authenticate(smsOtp);
            if (success) {
                setVisible(false);
                dispose();
            } else {
                showErrorDialog("Hibás SMS kód!");
                tfSmsOtp.requestFocus();
            }
        }
    }
}
