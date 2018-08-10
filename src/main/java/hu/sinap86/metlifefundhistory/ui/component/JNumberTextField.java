package hu.sinap86.metlifefundhistory.ui.component;

import org.apache.commons.lang3.StringUtils;

import java.awt.event.KeyEvent;

import javax.swing.*;

/**
 * A {@link JTextField} that skips all non-digit keys. The user is only able to enter numbers.
 */
public class JNumberTextField extends JTextField {

    private static final long serialVersionUID = 1L;

    @Override
    public void processKeyEvent(KeyEvent ev) {
        if (Character.isDigit(ev.getKeyChar())) {
            super.processKeyEvent(ev);
        }
        ev.consume();
        return;
    }

    /**
     * As the user is not even able to enter a dot ("."), only integers (whole numbers) may be entered.
     */
    public Long getNumber() {
        Long result = null;
        String text = getText();
        if (StringUtils.isNotEmpty(text)) {
            result = Long.valueOf(text);
        }
        return result;
    }

    public void setNumber(final Long number) {
        setText(number == null ? null : number.toString());
    }
}