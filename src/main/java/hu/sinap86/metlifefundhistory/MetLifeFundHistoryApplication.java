package hu.sinap86.metlifefundhistory;

import hu.sinap86.metlifefundhistory.ui.ApplicationFrame;

import java.awt.*;

public class MetLifeFundHistoryApplication {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            new ApplicationFrame().setVisible(true);
        });
    }

}
