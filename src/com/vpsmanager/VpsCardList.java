package com.vpsmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Scrollable vertical list of VpsCard panels.
 */
public class VpsCardList extends JPanel {

    // ── Design tokens ─────────────────────────────────────────────────────────
    static final Color BG           = new Color(0xF5F7FA);
    static final Color CARD_NORMAL  = new Color(0xFFFFFF);
    static final Color CARD_HOVER   = new Color(0xEEF2FF);
    static final Color SHADOW_COLOR = new Color(0, 0, 0, 18);
    static final Color NAME_COLOR   = new Color(0x1F2937);
    static final Color HOST_COLOR   = new Color(0x6B7280);
    static final Color ICON_NORMAL  = new Color(0xADB5BD);
    static final Color ICON_HOVER   = new Color(0x4F6EF7);
    static final Color DELETE_HOVER = new Color(0xEF4444);
    static final Color CONFIRM_BG   = new Color(0xFFF5F5);
    static final Color CONFIRM_TEXT = new Color(0xC0392B);
    static final int   RADIUS       = 14;
    static final int   CARD_PAD_H   = 20;
    static final int   CARD_PAD_V   = 13;

    private final MainFrame owner;
    private final List<Vps> vpsList = new ArrayList<>();
    private final JPanel cardsContainer;
    private final JLabel emptyLabel;
    private VpsCard draggedCard;
    private boolean dragging;
    private int dropInsertionIndex = -1;

    public VpsCardList(MainFrame owner) {
        this.owner = owner;
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(14, 14, 14, 14));

        cardsContainer = new JPanel();
        cardsContainer.setLayout(new BoxLayout(cardsContainer, BoxLayout.Y_AXIS));
        cardsContainer.setOpaque(false);

        emptyLabel = new JLabel(
                "<html><center>"
                + "<span style='font-size:14px; color:#9CA3AF;'>No VPS entries yet</span><br><br>"
                + "<span style='font-size:11px; color:#C4CAD4;'>Click <b>+ Add</b> to create one</span>"
                + "</center></html>");
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        emptyLabel.setVerticalAlignment(SwingConstants.TOP);
        emptyLabel.setBorder(new EmptyBorder(70, 0, 0, 0));

        add(cardsContainer, BorderLayout.NORTH);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setVpsList(List<Vps> list) {
        vpsList.clear();
        vpsList.addAll(list);
        rebuild();
    }

    public List<Vps> getVpsList() { return new ArrayList<>(vpsList); }

    public void addVps(Vps v)              { vpsList.add(v);          rebuild(); }
    public void replaceVps(int i, Vps v)   { vpsList.set(i, v);       rebuild(); }
    public void removeVps(int i)           { vpsList.remove(i);        rebuild(); }

    private void beginDrag(VpsCard card) {
        draggedCard = card;
        dragging = true;
        cardsContainer.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    private void updateDropTarget(Point pointInContainer) {
        int newInsertionIndex = vpsList.size();
        for (Component component : cardsContainer.getComponents()) {
            if (component instanceof VpsCard card
                    && pointInContainer.y < card.getY() + card.getHeight() / 2) {
                newInsertionIndex = card.index;
                break;
            }
        }
        if (dropInsertionIndex != newInsertionIndex) {
            dropInsertionIndex = newInsertionIndex;
            cardsContainer.repaint();
        }
    }

    private void finishDrag() {
        if (!dragging || draggedCard == null) return;

        int sourceIndex = draggedCard.index;
        int destinationIndex = dropInsertionIndex;
        if (destinationIndex > sourceIndex) destinationIndex--;

        if (destinationIndex != sourceIndex) {
            Vps moved = vpsList.remove(sourceIndex);
            vpsList.add(destinationIndex, moved);
            rebuild();
            owner.saveData();
        }

        draggedCard = null;
        dragging = false;
        dropInsertionIndex = -1;
        cardsContainer.setCursor(Cursor.getDefaultCursor());
        cardsContainer.repaint();
    }

    // ── Rebuild ───────────────────────────────────────────────────────────────

    private void rebuild() {
        cardsContainer.removeAll();
        remove(emptyLabel);

        if (vpsList.isEmpty()) {
            add(emptyLabel, BorderLayout.CENTER);
        } else {
            for (int i = 0; i < vpsList.size(); i++) {
                VpsCard card = new VpsCard(vpsList.get(i), i);
                card.setAlignmentX(LEFT_ALIGNMENT);
                cardsContainer.add(card);
                if (i < vpsList.size() - 1) {
                    cardsContainer.add(Box.createVerticalStrut(10));
                }
            }
        }
        revalidate();
        repaint();
    }

    // =========================================================================
    //  VpsCard inner class
    // =========================================================================

    class VpsCard extends JPanel {

        private final Vps vps;
        private final int index;

        // Hover + confirm state
        private boolean hovered    = false;
        private boolean confirming = false;
        private Point dragStart;
        private boolean suppressClick;

        // Right-side panels switched via CardLayout
        private static final String CARD_ICONS   = "icons";
        private static final String CARD_CONFIRM = "confirm";

        private final JPanel     rightPanel;   // CardLayout host
        private final JButton    editBtn;
        private final JButton    deleteBtn;
        private final JPanel     iconRow;
        private final JPanel     confirmRow;

        VpsCard(Vps vps, int index) {
            this.vps   = vps;
            this.index = index;

            setLayout(new BorderLayout(10, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(CARD_PAD_V, CARD_PAD_H, CARD_PAD_V, CARD_PAD_H));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            setPreferredSize(new Dimension(100, 72));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // ── Left text ─────────────────────────────────────────────────────
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(vps.getName());
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nameLabel.setForeground(NAME_COLOR);
            nameLabel.setAlignmentX(LEFT_ALIGNMENT);

            JLabel hostLabel = new JLabel(buildSubtitle());
            hostLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            hostLabel.setForeground(HOST_COLOR);
            hostLabel.setAlignmentX(LEFT_ALIGNMENT);

            textPanel.add(nameLabel);
            textPanel.add(Box.createVerticalStrut(3));
            textPanel.add(hostLabel);

            // ── Icon buttons ──────────────────────────────────────────────────
            editBtn   = makeIconButton("\u270F", ICON_NORMAL, ICON_HOVER, "Edit");
            deleteBtn = makeIconButton("\uD83D\uDDD1", ICON_NORMAL, DELETE_HOVER, "Delete");

            iconRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            iconRow.setOpaque(false);
            iconRow.add(editBtn);
            iconRow.add(deleteBtn);

            // ── Inline confirm row ────────────────────────────────────────────
            confirmRow = buildConfirmRow();

            // ── Right panel (CardLayout) ───────────────────────────────────────
            rightPanel = new JPanel(new CardLayout());
            rightPanel.setOpaque(false);
            rightPanel.setPreferredSize(new Dimension(140, 36));
            rightPanel.add(iconRow,    CARD_ICONS);
            rightPanel.add(confirmRow, CARD_CONFIRM);
            // Start with icons hidden (we're not hovered yet)
            showCard(CARD_ICONS);
            iconRow.setVisible(false);  // hidden until hover

            add(textPanel,  BorderLayout.CENTER);
            add(rightPanel, BorderLayout.EAST);

            // ── Action listeners ──────────────────────────────────────────────
            editBtn.addActionListener(e -> owner.editVps(vps, index));
            deleteBtn.addActionListener(e -> enterConfirmState());

            // ── Mouse tracking ────────────────────────────────────────────────
            attachHoverTracking();
            attachClickToConnect();
            attachDragToReorder();
        }

        // ── Build helpers ─────────────────────────────────────────────────────

        private String buildSubtitle() {
            String user  = vps.getUsername();
            String host  = vps.getHost();
            String shell = "[" + vps.getShell().name() + "]";
            if (user != null && !user.isEmpty()) return user + "@" + host + "  " + shell;
            return host + "  " + shell;
        }

        private JPanel buildConfirmRow() {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            p.setOpaque(false);

            JLabel lbl = new JLabel("Delete?");
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setForeground(CONFIRM_TEXT);

            JButton yes = makeSmallBtn("Yes", new Color(0xEF4444), Color.WHITE);
            JButton no  = makeSmallBtn("No", new Color(0xE9ECEF), new Color(0x374151));

            yes.addActionListener(e -> owner.deleteVps(index));
            no.addActionListener(e -> exitConfirmState());

            p.add(lbl);
            p.add(yes);
            p.add(no);
            return p;
        }

        // ── State transitions ─────────────────────────────────────────────────

        private void showCard(String name) {
            ((CardLayout) rightPanel.getLayout()).show(rightPanel, name);
        }

        private void enterConfirmState() {
            confirming = true;
            showCard(CARD_CONFIRM);
            repaint();
        }

        private void exitConfirmState() {
            confirming = false;
            if (hovered) {
                showCard(CARD_ICONS);
                iconRow.setVisible(true);
            } else {
                iconRow.setVisible(false);
                showCard(CARD_ICONS);
            }
            repaint();
        }

        private void setHovered(boolean h) {
            if (hovered == h) return;
            hovered = h;
            if (!confirming) {
                iconRow.setVisible(h);
            }
            repaint();
        }

        // ── Hover tracking ────────────────────────────────────────────────────

        /**
         * Installs mouse-entered/exited listeners on EVERY component in this card's
         * hierarchy (including JButtons) so that hover is detected regardless of
         * which child the cursor enters first.
         *
         * MouseListener enter/exit events do NOT interfere with JButton click behaviour.
         */
        private void attachHoverTracking() {
            installOnComponent(this);
        }

        private void installOnComponent(Component c) {
            c.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    setHovered(true);
                }
                @Override public void mouseExited(MouseEvent e) {
                    // Convert exit point to VpsCard coordinates.
                    // If it's still inside the card (moved to a sibling child), ignore.
                    Point p = SwingUtilities.convertPoint(c, e.getPoint(), VpsCard.this);
                    if (!VpsCard.this.contains(p)) {
                        setHovered(false);
                    }
                }
            });
            if (c instanceof Container ct) {
                for (Component child : ct.getComponents()) {
                    installOnComponent(child);   // recurse into ALL children, buttons included
                }
            }
        }

        // ── Click-to-connect ──────────────────────────────────────────────────

        private void attachClickToConnect() {
            MouseAdapter clickHandler = new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (suppressClick) {
                        suppressClick = false;
                        return;
                    }
                    if (confirming) return;
                    // Ignore clicks that landed on a button (edit / delete / confirm)
                    Component hit = SwingUtilities.getDeepestComponentAt(
                            VpsCard.this, e.getX(), e.getY());
                    if (hit instanceof JButton) return;
                    owner.connectVps(vps);
                }
            };
            // Only the card itself and non-button children need the click handler.
            // Buttons have their own ActionListeners and shouldn't trigger connect.
            addMouseListener(clickHandler);
            installClickOnNonButtons(this, clickHandler);
        }

        private void installClickOnNonButtons(Component c, MouseAdapter ma) {
            if (c instanceof Container ct) {
                for (Component child : ct.getComponents()) {
                    if (!(child instanceof JButton)) {
                        child.addMouseListener(ma);
                        installClickOnNonButtons(child, ma);
                    }
                }
            }
        }

        // ── Drag-to-reorder ──────────────────────────────────────────────────

        private void attachDragToReorder() {
            installDragOnNonButtons(this);
        }

        private void installDragOnNonButtons(Component c) {
            MouseAdapter dragHandler = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (confirming || e.getButton() != MouseEvent.BUTTON1) return;
                    dragStart = SwingUtilities.convertPoint(c, e.getPoint(), VpsCard.this);
                }

                @Override public void mouseDragged(MouseEvent e) {
                    if (dragStart == null || confirming) return;
                    Point pointInCard = SwingUtilities.convertPoint(c, e.getPoint(), VpsCard.this);
                    if (!dragging && dragStart.distance(pointInCard) >= 5) {
                        beginDrag(VpsCard.this);
                    }
                    if (dragging && draggedCard == VpsCard.this) {
                        Point pointInContainer = SwingUtilities.convertPoint(
                                c, e.getPoint(), cardsContainer);
                        updateDropTarget(pointInContainer);
                    }
                }

                @Override public void mouseReleased(MouseEvent e) {
                    if (dragging && draggedCard == VpsCard.this) {
                        suppressClick = true;
                        finishDrag();
                    }
                    dragStart = null;
                }
            };
            c.addMouseListener(dragHandler);
            c.addMouseMotionListener(dragHandler);
            if (c instanceof Container ct) {
                for (Component child : ct.getComponents()) {
                    if (!(child instanceof JButton)) {
                        installDragOnNonButtons(child);
                    }
                }
            }
        }

        // ── Painting ──────────────────────────────────────────────────────────

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Soft shadow
            g2.setColor(SHADOW_COLOR);
            g2.fill(new RoundRectangle2D.Float(2, 3, w - 3, h - 3, RADIUS, RADIUS));

            // Card face
            Color face = confirming ? CONFIRM_BG : (hovered ? CARD_HOVER : CARD_NORMAL);
            g2.setColor(face);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 2, h - 2, RADIUS, RADIUS));

            if (dragging && dropInsertionIndex == index) {
                g2.setColor(new Color(0x4F6EF7));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(8, 1, w - 10, 1);
            }

            g2.dispose();
            super.paintComponent(g);
        }

        // ── Button factories ──────────────────────────────────────────────────

        private JButton makeIconButton(String icon, Color normal, Color hoverColor, String tip) {
            JButton btn = new JButton(icon);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
            btn.setForeground(normal);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setToolTipText(tip);
            btn.setPreferredSize(new Dimension(30, 30));
            btn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { btn.setForeground(hoverColor); }
                @Override public void mouseExited(MouseEvent e)  { btn.setForeground(normal); }
            });
            return btn;
        }

        private JButton makeSmallBtn(String label, Color bg, Color fg) {
            JButton btn = new JButton(label);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setBackground(bg);
            btn.setForeground(fg);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setOpaque(true);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(30, 26));
            btn.setBorder(new EmptyBorder(2, 6, 2, 6));
            return btn;
        }
    }
}
