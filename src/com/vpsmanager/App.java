package com.vpsmanager;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;

public class App {
    public static void main(String[] args) {
        // FlatLaf light theme — must be set before any Swing component is created
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
