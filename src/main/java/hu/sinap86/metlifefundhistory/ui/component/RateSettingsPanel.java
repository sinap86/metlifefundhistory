package hu.sinap86.metlifefundhistory.ui.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;

import static hu.sinap86.metlifefundhistory.util.UIUtils.*;

public class RateSettingsPanel extends JPanel {

    private final JCheckBox chkUseOnlineRates;
    private final JButton btnChooseRateFile;

    private File rateFile;

    public RateSettingsPanel() {
        super(new GridBagLayout());

        chkUseOnlineRates = new JCheckBox("Online árfolyamok aktív alapoknál");
        addComponent(chkUseOnlineRates, this, 0, 0, 3);

        final JLabel lblRateFile = addLabel("Árfolyamok betöltése innen:", this, 1, 0);
        lblRateFile.setPreferredSize(new Dimension(180, 26));

        final JTextField tfRateFile = addTextField(20, this, 1, 1);
        tfRateFile.setEnabled(false);

        btnChooseRateFile = new JButton("...");
        btnChooseRateFile.addActionListener(event -> {
            final File selectedFile = showFileChooser(this, "Fájl megnyitása", JFileChooser.FILES_ONLY, XML_FILE_NAME_FILTER);
            if (selectedFile != null) {
                rateFile = selectedFile;
                tfRateFile.setText(rateFile.getAbsolutePath());
            }
        });
        addComponent(btnChooseRateFile, this, 1, 2);

        chkUseOnlineRates.addItemListener((ItemEvent event) -> {
            btnChooseRateFile.setEnabled(event.getStateChange() == ItemEvent.DESELECTED);
            repaint();
        });

        setBorder(BorderFactory.createTitledBorder("Árfolyam beállítások"));
    }

    public String validateUserInputAndGetErrorMessage() {
        if (!chkUseOnlineRates.isSelected() && rateFile == null) {
            btnChooseRateFile.requestFocus();
            return "Nincs árfolyam fájl kiválasztva!";
        }
        if (rateFile != null && !rateFile.canRead()) {
            btnChooseRateFile.requestFocus();
            return "A kiválasztott árfolyam fájl nem olvasható!";
        }
        return null;
    }

    public File getRateFile() {
        return rateFile;
    }


    public boolean useOnlineRates() {
        return chkUseOnlineRates.isSelected();
    }
}
