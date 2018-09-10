package hu.sinap86.metlifefundhistory.ui.component;

import static hu.sinap86.metlifefundhistory.util.UIUtils.addLabel;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.model.Contract;
import hu.sinap86.metlifefundhistory.web.session.WebSessionManager;

import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.math.BigDecimal;
import java.util.Set;

import javax.swing.*;

public class LoginInfoPanel extends JPanel {

    private final WebSessionManager webSessionManager;

    public LoginInfoPanel(final WebSessionManager webSessionManager) {
        this.webSessionManager = webSessionManager;
        if (!webSessionManager.isAuthenticated()) {
            throw new IllegalStateException("User not logged in!");
        }

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(createUserInfoPanel());
        add(createContractsInfoPanel());
        // create empty space to preserve right vertical space between components
        if (webSessionManager.getUserContracts().size() < 2) {
            add(Box.createRigidArea(new Dimension(0, 150)));
        }
    }

    private JPanel createUserInfoPanel() {
        final WebSessionManager.User user = webSessionManager.getUser();

        final JPanel userInfoPanel = new JPanel(new GridBagLayout());

        addLabel("Belépve: ", userInfoPanel, 0, 0);
        addLabel(user.getName(), userInfoPanel, 0, 1);

        addLabel("Utolsó bejelentkezés: ", userInfoPanel, 1, 0);
        addLabel(user.getLastLogin(), userInfoPanel, 1, 1);

        return userInfoPanel;
    }

    public JComponent createContractsInfoPanel() {
        final JPanel contractsPanel = new JPanel();
        contractsPanel.setLayout(new BoxLayout(contractsPanel, BoxLayout.Y_AXIS));

        final Set<Contract> userContracts = webSessionManager.getUserContracts();
        for (Contract userContract : userContracts) {
            contractsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            contractsPanel.add(createContractPanel(userContract));
        }

        final JScrollPane scrollPane = new JScrollPane(contractsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private JPanel createContractPanel(final Contract contract) {
        final String currency = contract.getCurrency();

        final JPanel contractPanel = new JPanel(new GridBagLayout());

        addLabel("Szerződés aktuális értéke:", contractPanel, 0, 0);
        addLabel(amount(contract.getActualValue(), currency), contractPanel, 0, 1);

        addLabel("Visszavásárlási érték:", contractPanel, 1, 0);
        addLabel(amount(contract.getSurrenderValue(), currency), contractPanel, 1, 1);

        addLabel("Díjelmaradás:", contractPanel, 2, 0);
        addLabel(BigDecimal.ZERO.equals(contract.getDueAmount()) ? "nincs" : amount(contract.getDueAmount(), currency), contractPanel, 2, 1);

        addLabel("Díjfedezet lejárata:", contractPanel, 3, 0);
        addLabel(contract.getPaidToDate(), contractPanel, 3, 1);

        contractPanel.setBorder(BorderFactory.createTitledBorder(getContractName(contract)));
        return contractPanel;
    }

    private String getContractName(final Contract contract) {
        final StringBuilder sb = new StringBuilder();
        sb.append(contract.getId());
        sb.append(" ");
        sb.append(contract.getName());
        if (StringUtils.isNotEmpty(contract.getType())) {
            sb.append(" ");
            sb.append(contract.getType());
        }
        if (StringUtils.isNotEmpty(contract.getTypeName())) {
            sb.append(" ");
            sb.append(contract.getTypeName());
        }
        return sb.toString();
    }

    private String amount(final BigDecimal amount, final String currency) {
        return amount == null ? StringUtils.EMPTY : String.format("%s %s", Constants.UI_AMOUNT_FORMAT.format(amount), currency);
    }
}
