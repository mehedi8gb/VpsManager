package com.vpsmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main application window.
 * Layout: top-bar (title + "+ Add" button) / scrollable VpsCardList.
 */
public class MainFrame extends JFrame {

    private final VpsStore store = new VpsStore(Paths.get("data", "vps.txt"));
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

        topBar.add(title, BorderLayout.WEST);
        topBar.add(addBtn, BorderLayout.EAST);

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

    private void loadData() {
        try {
            List<Vps> loaded = store.load();
            cardList.setVpsList(loaded);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not load data/vps.txt:\n" + e.getMessage(),
                    "Load error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void saveData() {
        try {
            store.save(cardList.getVpsList());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not save data/vps.txt:\n" + e.getMessage(),
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

    /**
     * Opens a new, visible CMD or PowerShell window and runs the VPS command.
     * Uses "cmd /c start" as a wrapper so a real console window always appears,
     * even when the parent JVM process has no console of its own.
     *
     * Bug fix: plain ProcessBuilder("powershell.exe", ...) inherits the JVM's
     * hidden console on Windows — the window never surfaces. "cmd /c start <exe>"
     * always creates a new top-level window.
     */
    void connectVps(Vps v) {
        String cmd = v.buildFinalCommand();
        try {
            ProcessBuilder pb;
            if (v.getShell() == Vps.Shell.POWERSHELL) {
                // "cmd /c start <program> <args>" always opens a new visible window.
                // Do NOT pass a title string before powershell.exe — start treats the
                // first unquoted arg as the program name, not a title.  The previous
                // code passed "PowerShell" as an unquoted arg, which made start launch
                // PowerShell.exe with "powershell.exe" as its argument (wrong).
                pb = new ProcessBuilder(
                        "cmd.exe", "/c", "start",
                        "powershell.exe", "-NoExit", "-Command", cmd
                );
            } else {
                pb = new ProcessBuilder(
                        "cmd.exe", "/c", "start",
                        "cmd.exe", "/k", cmd
                );
            }
            pb.directory(Paths.get(".").toAbsolutePath().normalize().toFile());
            pb.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to launch shell:\n" + e.getMessage() +
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
