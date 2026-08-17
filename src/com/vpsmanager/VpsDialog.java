package com.vpsmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modal dialog to create or edit a single Vps entry.
 * Unchanged in logic; minor sizing tweaks for FlatLaf.
 */
public class VpsDialog extends JDialog {

    private final JTextField nameField    = new JTextField(22);
    private final JTextField hostField    = new JTextField(22);
    private final JTextField userField    = new JTextField(22);
    private final JPasswordField passField = new JPasswordField(22);
    private final JComboBox<Vps.Shell> shellBox = new JComboBox<>(Vps.Shell.values());
    private final JComboBox<TerminalLauncher.Option> terminalBox = new JComboBox<>();
    private final JTextArea commandArea   = new JTextArea(4, 22);

    private boolean confirmed = false;
    private Vps result;

    public VpsDialog(Frame owner, Vps existing) {
        super(owner, existing == null ? "Add VPS" : "Edit VPS", true);
        setResizable(true);
        setMinimumSize(new Dimension(440, 380));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 18, 8, 18));
        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(6, 6, 6, 6);
        c.fill    = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(form, c, row++, "Name:",      nameField);
        addRow(form, c, row++, "Host / IP:", hostField);
        addRow(form, c, row++, "Username:",  userField);
        addRow(form, c, row++, "Password:",  passField);
        populateTerminalOptions(existing);
        addRow(form, c, row++, "Terminal:",  terminalBox);
        addRow(form, c, row++, "Shell:",     shellBox);

        // Command (multi-line)
        c.gridx = 0; c.gridy = row; c.weightx = 0; c.gridwidth = 1;
        c.fill  = GridBagConstraints.HORIZONTAL;
        form.add(new JLabel("Command:"), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1;
        c.fill  = GridBagConstraints.BOTH; c.weighty = 1;
        commandArea.setLineWrap(true);
        commandArea.setWrapStyleWord(true);
        form.add(new JScrollPane(commandArea), c);
        row++;
        c.weighty = 0;

        // Hint
        JLabel hint = new JLabel("<html><i style='color:#6B7280'>Tip: use {host}, {username}, {password} — e.g. <b>ssh {username}@{host}</b></i></html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.fill  = GridBagConstraints.HORIZONTAL;
        form.add(hint, c);

        // Buttons
        JButton okBtn     = new JButton("Save");
        JButton cancelBtn = new JButton("Cancel");
        okBtn.setPreferredSize(new Dimension(90, 32));
        cancelBtn.setPreferredSize(new Dimension(90, 32));
        okBtn.addActionListener(e -> onSave());
        cancelBtn.addActionListener(e -> dispose());

        // Make Save the default button (Enter triggers it)
        getRootPane().setDefaultButton(okBtn);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE5E7EB)));
        buttons.add(cancelBtn);
        buttons.add(okBtn);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form,    BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        // Pre-fill if editing
        if (existing != null) {
            nameField.setText(existing.getName());
            hostField.setText(existing.getHost());
            userField.setText(existing.getUsername());
            passField.setText(existing.getPassword());
            selectTerminal(existing.getTerminal());
            shellBox.setSelectedItem(existing.getShell());
            commandArea.setText(existing.getCommand());
        } else {
            commandArea.setText("ssh {username}@{host}");
        }

        pack();
        setLocationRelativeTo(owner);
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.weightx = 0; c.gridwidth = 1;
        c.fill  = GridBagConstraints.HORIZONTAL;
        form.add(new JLabel(label), c);
        c.gridx = 1; c.gridy = row; c.weightx = 1;
        form.add(field, c);
    }

    private void onSave() {
        if (nameField.getText().trim().isEmpty() || hostField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Name and Host are required.",
                    "Missing info", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocusInWindow();
            return;
        }
        result = new Vps(
                nameField.getText().trim(),
                hostField.getText().trim(),
                userField.getText().trim(),
                new String(passField.getPassword()),
                (Vps.Shell) shellBox.getSelectedItem(),
                ((TerminalLauncher.Option) terminalBox.getSelectedItem()).id(),
                commandArea.getText().trim()
        );
        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() { return confirmed; }
    public Vps getResult()       { return result; }

    private void populateTerminalOptions(Vps existing) {
        for (TerminalLauncher.Option option : TerminalLauncher.availableOptions()) {
            terminalBox.addItem(option);
        }
        selectTerminal(existing == null ? AppSettings.getPreferredTerminal() : existing.getTerminal());
    }

    private void selectTerminal(String terminalId) {
        for (int i = 0; i < terminalBox.getItemCount(); i++) {
            if (terminalBox.getItemAt(i).id().equals(terminalId)) {
                terminalBox.setSelectedIndex(i);
                return;
            }
        }
        terminalBox.setSelectedIndex(0);
    }
}
