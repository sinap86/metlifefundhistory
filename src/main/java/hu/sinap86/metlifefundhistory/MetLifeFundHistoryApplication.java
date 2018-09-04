package hu.sinap86.metlifefundhistory;

import hu.sinap86.metlifefundhistory.ui.ApplicationFrame;
import hu.sinap86.metlifefundhistory.util.UIUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

@Slf4j
public class MetLifeFundHistoryApplication {

    public static void main(String[] args) {
        final String lookAndFeelProperty = System.getProperty("swing.defaultlaf");
        if (StringUtils.isEmpty(lookAndFeelProperty)) {
            log.info("No look and feel runtime settings, using system default.");
            UIUtils.setSystemLookAndFeel();
        } else {
            log.info("Look and feel runtime settings: {}", lookAndFeelProperty);
        }
        EventQueue.invokeLater(() -> {
            new ApplicationFrame().setVisible(true);
        });
    }

}
