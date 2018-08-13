package hu.sinap86.metlifefundhistory.web;

import lombok.extern.slf4j.Slf4j;

// TODO call MetLife through REST api
@Slf4j
public class WebRequestManager {

    private boolean authenticationWithPasswordSucceeded;
    private boolean authenticationWithSmsOtpSucceeded;

    public boolean isAuthenticated() {
        return authenticationWithPasswordSucceeded && authenticationWithSmsOtpSucceeded;
    }

    public boolean isAuthenticationWithPasswordSucceeded() {
        return authenticationWithPasswordSucceeded;
    }

    public boolean isAuthenticationWithSmsOtpSucceeded() {
        return authenticationWithSmsOtpSucceeded;
    }

    public boolean authenticate(final String userName, final char[] password) {
        authenticationWithPasswordSucceeded = "1".equals(userName);
        log.debug("Authenticate with '{}' user and password {}", userName, (authenticationWithPasswordSucceeded ? "succeeded." : "NOT succeeded."));
        return authenticationWithPasswordSucceeded;
    }

    public boolean authenticate(final String smsOtp) {
        authenticationWithSmsOtpSucceeded = "1".equals(smsOtp);
        log.debug("Authenticate with sms OTP {}", (authenticationWithSmsOtpSucceeded ? "succeeded." : "NOT succeeded."));
        return authenticationWithSmsOtpSucceeded;
    }
}
