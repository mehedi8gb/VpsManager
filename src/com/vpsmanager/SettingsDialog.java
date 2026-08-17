package com.vpsmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** General application settings. */
public class SettingsDialog extends JDialog {
    private final JComboBox<TerminalLauncher.Option> terminalBox;

    public SettingsDialog(Frame owner) {
        super(owner, "Settings", true);
        setMinimumSize(new Dimension(470, 230));

        List<TerminalLauncher.Option> options = TerminalLauncher.availableOptions();
        terminalBox = new JComboBox<>(options.toArray(TerminalLauncher.Option[]::new));
        selectSavedOption(options);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(18, 20, 10, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        form.add(new JLabel("Terminal:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(terminalBox, c);

        JLabel hint = new JLabel("<html><span style='color:#6B7280'>Automatic detects a suitable terminal on this operating system. "
                + "Only terminals available on this device are listed.</span></html>");
        c.gridx = 0; c.gridy = 1; c.gridwidth = 2; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(hint, c);

        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        save.addActionListener(e -> save());
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(save);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE5E7EB)));
        buttons.add(cancel);
        buttons.add(save);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private void selectSavedOption(List<TerminalLauncher.Option> options) {
        String saved = AppSettings.getPreferredTerminal();
        for (TerminalLauncher.Option option : options) {
            if (option.id().equals(saved)) {
                terminalBox.setSelectedItem(option);
                return;
            }
        }
    }

    private void save() {
        TerminalLauncher.Option selected = (TerminalLauncher.Option) terminalBox.getSelectedItem();
        if (selected != null) AppSettings.setPreferredTerminal(selected.id());
        dispose();
    }
}
