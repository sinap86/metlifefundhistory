package hu.sinap86.metlifefundhistory.config;

import hu.sinap86.metlifefundhistory.util.Constants;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.prefs.Preferences;

@Slf4j
public class ApplicationConfig {

    private static final String CONFIG_USE_PROXY = "CONFIG_USE_PROXY";
    private static final String CONFIG_PROXY_HOST = "CONFIG_PROXY_HOST";
    private static final String CONFIG_PROXY_PORT = "CONFIG_PROXY_PORT";
    private static final String CONFIG_PROXY_SCHEME = "CONFIG_PROXY_SCHEME";

    private static ApplicationConfig applicationConfig;

    @Synchronized
    public static ApplicationConfig getInstance() {
        if (applicationConfig == null) {
            applicationConfig = new ApplicationConfig();
        }
        return applicationConfig;
    }

    private final Preferences preferences = Preferences.userNodeForPackage(ApplicationConfig.class);

    public boolean isUseProxy() {
        return preferences.getBoolean(CONFIG_USE_PROXY, false);
    }

    public void setUseProxy(final boolean useProxy) {
        log.debug("Setting '{}' to '{}'", CONFIG_USE_PROXY, useProxy);
        preferences.putBoolean(CONFIG_USE_PROXY, useProxy);
    }

    public String getProxyHost() {
        return preferences.get(CONFIG_PROXY_HOST, StringUtils.EMPTY);
    }

    public void setProxyHost(final String proxyHost) {
        log.debug("Setting '{}' to '{}'", CONFIG_PROXY_HOST, proxyHost);
        preferences.put(CONFIG_PROXY_HOST, proxyHost);
    }

    public Long getProxyPort() {
        return preferences.getLong(CONFIG_PROXY_PORT, 80);
    }

    public void setProxyPort(final Long proxyPort) {
        log.debug("Setting '{}' to '{}'", CONFIG_PROXY_PORT, proxyPort);
        preferences.putLong(CONFIG_PROXY_PORT, proxyPort);
    }

    public String getProxyScheme() {
        return preferences.get(CONFIG_PROXY_SCHEME, Constants.SUPPORTED_PROXY_SCHEMES[0]);
    }

    public void setProxyScheme(final String proxyScheme) {
        log.debug("Setting '{}' to '{}'", CONFIG_PROXY_SCHEME, proxyScheme);
        preferences.put(CONFIG_PROXY_SCHEME, proxyScheme);
    }
}
