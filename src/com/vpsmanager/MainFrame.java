package com.vpsmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main application window.
 * Layout: top-bar (title + "+ Add" button) / scrollable VpsCardList.
 */
public class MainFrame extends JFrame {

    private final VpsStore store = new VpsStore(resolveDataFile());
    private VpsCardList cardList;

    public MainFrame() {
        super("VPS Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 600);
        setMinimumSize(new Dimension(400, 400));
        setLocationRelativeTo(null);

        // ── Top bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5E7EB)),
                new EmptyBorder(12, 18, 12, 18)
        ));

        JLabel title = new JLabel("VPS Manager");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(0x1F2937));

        JButton addBtn = createAddButton();
        addBtn.addActionListener(e -> addVps());

        JButton settingsBtn = new JButton("⚙");
        settingsBtn.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        settingsBtn.setToolTipText("Settings");
        settingsBtn.setFocusPainted(false);
        settingsBtn.setBorderPainted(false);
        settingsBtn.setContentAreaFilled(false);
        settingsBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsBtn.addActionListener(e -> new SettingsDialog(this).setVisible(true));

        JPanel topBarActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topBarActions.setOpaque(false);
        topBarActions.add(settingsBtn);
        topBarActions.add(addBtn);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(topBarActions, BorderLayout.EAST);

        // ── Card list ─────────────────────────────────────────────────────────
        cardList = new VpsCardList(this);

        JScrollPane scroll = new JScrollPane(cardList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBackground(new Color(0xF5F7FA));
        scroll.getViewport().setBackground(new Color(0xF5F7FA));

        // ── Assemble ──────────────────────────────────────────────────────────
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topBar, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);

        loadData();
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    /** Resolves a per-user writable data file for both installed and source runs. */
    private static Path resolveDataFile() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path dataDirectory = (localAppData == null || localAppData.isBlank())
                ? Paths.get(System.getProperty("user.home"), ".vpsmanager", "data")
                : Paths.get(localAppData, "VpsManager", "data");
        Path dataFile = dataDirectory.resolve("vps.txt");
        Path packagedFile = Paths.get("data", "vps.txt");

        try {
            if (!Files.exists(dataFile) && Files.isRegularFile(packagedFile)) {
                Files.createDirectories(dataDirectory);
                Files.copy(packagedFile, dataFile);
            }
        } catch (IOException ignored) {
            // save() reports a useful error if the user data directory is unavailable.
        }
        return dataFile;
    }

    private void loadData() {
        try {
            List<Vps> loaded = store.load();
            cardList.setVpsList(loaded);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not load VPS data:\n" + e.getMessage(),
                    "Load error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void saveData() {
        try {
            store.save(cardList.getVpsList());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not save VPS data:\n" + e.getMessage(),
                    "Save error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void addVps() {
        VpsDialog dlg = new VpsDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            cardList.addVps(dlg.getResult());
            saveData();
        }
    }

    /** Called by VpsCard when the edit icon is clicked. */
    void editVps(Vps existing, int index) {
        VpsDialog dlg = new VpsDialog(this, existing);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            cardList.replaceVps(index, dlg.getResult());
            saveData();
        }
    }

    /** Called by VpsCard after the inline confirm is accepted. */
    void deleteVps(int index) {
        cardList.removeVps(index);
        saveData();
    }

    /** Opens the VPS command in the configured native terminal. */
    void connectVps(Vps v) {
        String cmd = v.buildFinalCommand();
        try {
            TerminalLauncher.launch(v.getShell(), cmd, AppSettings.getPreferredTerminal());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to launch terminal:\n" + e.getMessage() +
                    "\n\nCommand was:\n" + cmd,
                    "Launch error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JButton createAddButton() {
        JButton btn = new JButton("+ Add");
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(0x4F6EF7));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));

        // Hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(0x3B5CE6));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(0x4F6EF7));
            }
        });
        return btn;
    }
}
