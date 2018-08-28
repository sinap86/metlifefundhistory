package hu.sinap86.metlifefundhistory.ui.component;

import static hu.sinap86.metlifefundhistory.util.UIUtils.addLabel;

import hu.sinap86.metlifefundhistory.config.Constants;
import hu.sinap86.metlifefundhistory.web.MetLifeWebSessionManager;

import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.List;

import javax.swing.*;

public class LoginInfoPanel extends JPanel {

    private final MetLifeWebSessionManager webSessionManager;

    public LoginInfoPanel(final MetLifeWebSessionManager webSessionManager) {
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
        final MetLifeWebSessionManager.User user = webSessionManager.getUser();

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

        final List<MetLifeWebSessionManager.Contract> userContracts = webSessionManager.getUserContracts();
        for (int i = 0; i < userContracts.size(); i++) {
            contractsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            contractsPanel.add(createContractPanel(userContracts.get(i)));
        }

        final JScrollPane scrollPane = new JScrollPane(contractsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private JPanel createContractPanel(final MetLifeWebSessionManager.Contract contract) {
        final String currency = contract.getCurrency();

        final JPanel contractPanel = new JPanel(new GridBagLayout());

        addLabel("Szerződés aktuális értéke:", contractPanel, 0, 0);
        addLabel(amount(contract.getActualValue(), currency), contractPanel, 0, 1);

        addLabel("Visszavásárlási érték:", contractPanel, 1, 0);
        addLabel(amount(contract.getSurrenderValue(), currency), contractPanel, 1, 1);

        addLabel("Díjelmaradás:", contractPanel, 2, 0);
        addLabel(contract.getDueAmount() == 0 ? "nincs" : amount(contract.getDueAmount(), currency), contractPanel, 2, 1);

        addLabel("Díjfedezet lejárata:", contractPanel, 3, 0);
        addLabel(contract.getPaidToDate(), contractPanel, 3, 1);

        contractPanel.setBorder(BorderFactory.createTitledBorder(getContractName(contract)));
        return contractPanel;
    }

    private String getContractName(final MetLifeWebSessionManager.Contract contract) {
        final StringBuilder sb = new StringBuilder();
        sb.append(contract.getId());
        sb.append(" ");
        sb.append(contract.getName());
        if (StringUtils.isNotEmpty(contract.getContractTypeNumber())) {
            sb.append(" ");
            sb.append(contract.getContractTypeNumber());
        }
        if (StringUtils.isNotEmpty(contract.getContractTypeName())) {
            sb.append(" ");
            sb.append(contract.getContractTypeName());
        }
        return sb.toString();
    }

    private String amount(final double amount, final String currency) {
        return String.format("%s %s", Constants.UI_AMOUNT_FORMAT.format(amount), currency);
    }
}
