package com.finanzas.ui;

import com.finanzas.api.BackendInvitation;
import com.finanzas.api.BackendWorkspace;
import com.finanzas.data.DataManager;
import com.finanzas.data.MemberOption;
import com.finanzas.model.FinancialCategory;
import com.finanzas.model.GastoHogar;
import com.finanzas.model.Money;
import com.finanzas.model.Settlement;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.AppIcons;
import com.finanzas.ui.components.RoundedButton;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FinanzasHogarPanel extends JPanel {
    private final DataManager data = DataManager.getInstance();
    private final NumberFormat nf = NumberFormat.getInstance(data.getDisplayLocale());
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private JPanel debtCards;
    private JPanel membersListPanel;
    private JPanel invitationsPanel;
    private DefaultTableModel tableModel;
    private List<GastoHogar> currentList;
    private boolean backendActionRunning;

    public FinanzasHogarPanel() {
        setBackground(AppColors.MAIN_BG);
        setLayout(new BorderLayout());
        buildUI();
        data.addListener(() -> SwingUtilities.invokeLater(this::refresh));
    }

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildTopSection(), buildTableSection());
        split.setResizeWeight(0.42);
        split.setBorder(null);
        split.setDividerSize(6);
        split.setBackground(AppColors.MAIN_BG);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppColors.MAIN_BG);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));

        JLabel title = new JLabel("Finanzas del Hogar");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Gestiona los gastos compartidos con tu hogar");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(AppColors.TEXT_SECONDARY);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(AppColors.MAIN_BG);
        left.add(title);
        left.add(subtitle);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        RoundedButton settlementButton = new RoundedButton("Registrar liquidacion", AppColors.TEXT_MUTED);
        settlementButton.addActionListener(e -> showSettlementDialog());
        RoundedButton addButton = new RoundedButton(AppIcons.PLUS + " Nuevo Gasto Hogar", AppColors.CARD_AHORROS);
        addButton.addActionListener(e -> showGastoDialog(null));
        actions.add(settlementButton);
        actions.add(addButton);
        header.add(left, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildTopSection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 16, 0));
        panel.setBackground(AppColors.MAIN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 20, 8, 20));
        panel.add(buildMembersCard());
        panel.add(buildDebtsCard());
        return panel;
    }

    private JPanel buildMembersCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 8));
        JLabel title = new JLabel("👥 Miembros del hogar");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(AppColors.TEXT_PRIMARY);

        JPanel membersList = new JPanel();
        membersList.setLayout(new BoxLayout(membersList, BoxLayout.Y_AXIS));
        membersList.setOpaque(false);
        membersListPanel = membersList;
        refreshMembersList(membersListPanel);

        invitationsPanel = new JPanel();
        invitationsPanel.setLayout(new BoxLayout(invitationsPanel, BoxLayout.Y_AXIS));
        invitationsPanel.setOpaque(false);
        refreshInvitationsPanel();

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(membersListPanel);
        content.add(Box.createVerticalStrut(8));
        content.add(invitationsPanel);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttonRow.setOpaque(false);
        boolean backendSession = data.isBackendSessionActive();
        boolean canManageWorkspace = !backendSession || data.canManageActiveBackendWorkspace();
        RoundedButton addMember = new RoundedButton(AppIcons.PLUS + (backendSession ? " Invitar" : " Agregar"), AppColors.ACCENT_BLUE);
        addMember.setPreferredSize(new Dimension(120, 28));
        addMember.setFont(new Font("Segoe UI", Font.BOLD, 11));
        addMember.setEnabled(!backendActionRunning && canManageWorkspace);
        addMember.setToolTipText(canManageWorkspace ? "" : "Solo OWNER o ADMIN pueden invitar miembros.");
        addMember.addActionListener(e -> {
            if (backendSession) {
                showInviteMemberDialog();
                return;
            }
            String name = JOptionPane.showInputDialog(this, "Nombre del nuevo miembro:", "Agregar miembro", JOptionPane.QUESTION_MESSAGE);
            if (name != null && !name.trim().isEmpty()) {
                data.addMiembro(name.trim());
            }
        });
        buttonRow.add(addMember);
        if (backendSession) {
            RoundedButton refreshButton = new RoundedButton("Actualizar", AppColors.TEXT_MUTED);
            refreshButton.setPreferredSize(new Dimension(110, 28));
            refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
            refreshButton.setEnabled(!backendActionRunning);
            refreshButton.addActionListener(e -> runBackendHouseholdAction("", () -> data.refreshBackendInvitations()));
            buttonRow.add(refreshButton);

            BackendWorkspace active = data.getActiveBackendWorkspace();
            boolean isOwner = active != null && "OWNER".equalsIgnoreCase(active.getRole());
            RoundedButton transferOwner = new RoundedButton("Transferir", AppColors.TEXT_MUTED);
            transferOwner.setPreferredSize(new Dimension(105, 28));
            transferOwner.setFont(new Font("Segoe UI", Font.BOLD, 11));
            transferOwner.setEnabled(!backendActionRunning && isOwner && data.getBackendMemberOptions().size() > 1);
            transferOwner.addActionListener(e -> showTransferOwnerDialog());
            buttonRow.add(transferOwner);

            RoundedButton leaveWorkspace = new RoundedButton("Abandonar", AppColors.ACCENT_RED);
            leaveWorkspace.setPreferredSize(new Dimension(115, 28));
            leaveWorkspace.setFont(new Font("Segoe UI", Font.BOLD, 11));
            leaveWorkspace.setEnabled(!backendActionRunning);
            leaveWorkspace.addActionListener(e -> leaveActiveWorkspace());
            buttonRow.add(leaveWorkspace);
        }

        card.add(title, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        card.add(buttonRow, BorderLayout.SOUTH);
        return card;
    }

    private void showInviteMemberDialog() {
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        JTextField emailField = new JTextField();
        emailField.setColumns(18);
        JComboBox<String> roleBox = new JComboBox<String>(new String[]{"MEMBER", "VIEWER", "ADMIN"});
        form.add(new JLabel("Email:"));
        form.add(emailField);
        form.add(new JLabel("Rol:"));
        form.add(roleBox);

        int result = JOptionPane.showConfirmDialog(this, form, "Invitar miembro al workspace", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        runBackendHouseholdAction(
                "Invitacion enviada.",
                () -> data.inviteBackendMember(emailField.getText().trim(), (String) roleBox.getSelectedItem()));
    }

    private void showTransferOwnerDialog() {
        String currentUserId = data.getCurrentBackendUserId();
        List<MemberOption> candidates = new ArrayList<MemberOption>();
        for (MemberOption option : data.getBackendMemberOptions()) {
            if (!option.getUserId().equals(currentUserId)) {
                candidates.add(option);
            }
        }
        if (candidates.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay otro miembro para recibir la propiedad.", "Transferir", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JComboBox<MemberOption> memberBox = new JComboBox<MemberOption>(candidates.toArray(new MemberOption[0]));
        int result = JOptionPane.showConfirmDialog(
                this,
                memberBox,
                "Transferir propiedad",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        MemberOption selected = (MemberOption) memberBox.getSelectedItem();
        if (selected == null) {
            return;
        }
        runBackendHouseholdAction(
                "Propiedad transferida.",
                () -> data.transferBackendOwnership(selected.getUserId()));
    }

    private void leaveActiveWorkspace() {
        BackendWorkspace active = data.getActiveBackendWorkspace();
        String workspaceName = active == null ? "este workspace" : active.getNombre();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Deseas abandonar " + workspaceName + "?",
                "Abandonar workspace",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        runBackendHouseholdAction(
                "Workspace abandonado.",
                () -> data.leaveBackendWorkspace());
    }

    private void refreshMembersList(JPanel panel) {
        panel.removeAll();
        List<Map.Entry<String, Double>> balances = new ArrayList<>(data.calcularDeudas().entrySet());
        for (Map.Entry<String, Double> entry : balances) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

            JLabel name = new JLabel("👤 " + entry.getKey());
            name.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            name.setForeground(AppColors.TEXT_PRIMARY);

            double balance = entry.getValue();
            JLabel balanceLabel = new JLabel(balance >= 0
                    ? "+$" + nf.format((long) balance) + " le deben"
                    : "-$" + nf.format((long) Math.abs(balance)) + " debe");
            balanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            balanceLabel.setForeground(balance >= 0 ? AppColors.ACCENT_GREEN : AppColors.ACCENT_RED);

            MemberOption memberOption = memberOptionByLabel(entry.getKey());
            String memberUserId = memberOption == null ? "" : memberOption.getUserId();
            String role = memberOption == null ? data.getBackendMemberRole(entry.getKey()) : memberOption.getRole();
            boolean self = data.isBackendSessionActive() && memberUserId.equals(data.getCurrentBackendUserId());
            boolean owner = "OWNER".equalsIgnoreCase(role);

            JButton delete = new JButton("X");
            delete.setFont(new Font("Segoe UI", Font.BOLD, 9));
            delete.setBorderPainted(false);
            delete.setContentAreaFilled(false);
            delete.setForeground(AppColors.TEXT_MUTED);
            delete.setEnabled(!backendActionRunning
                    && (!data.isBackendSessionActive() || (data.canManageActiveBackendWorkspace() && !self && !owner)));
            if (data.isBackendSessionActive() && !data.canManageActiveBackendWorkspace()) {
                delete.setToolTipText("Solo OWNER o ADMIN pueden eliminar miembros.");
            } else if (self) {
                delete.setToolTipText("Usa Abandonar para salir del workspace.");
            } else if (owner) {
                delete.setToolTipText("Transfiere la propiedad antes de eliminar al OWNER.");
            }
            delete.addActionListener(e -> {
                if (data.isBackendSessionActive()) {
                    int confirm = JOptionPane.showConfirmDialog(this, "Eliminar a " + entry.getKey() + " del workspace?", "Confirmar", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        runBackendHouseholdAction("", () -> data.removeBackendMemberById(memberUserId));
                    }
                    return;
                }
                data.removeMiembro(entry.getKey());
                refreshMembersList(panel);
                panel.revalidate();
                panel.repaint();
            });

            JPanel memberActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            memberActions.setOpaque(false);
            if (data.isBackendSessionActive()) {
                memberActions.add(roleComponent(memberOption, entry.getKey()));
            }
            memberActions.add(delete);

            row.add(name, BorderLayout.WEST);
            row.add(balanceLabel, BorderLayout.CENTER);
            row.add(memberActions, BorderLayout.EAST);
            panel.add(row);
            panel.add(Box.createVerticalStrut(4));
        }
        panel.revalidate();
        panel.repaint();
    }

    private JComponent roleComponent(MemberOption option, String memberName) {
        String memberUserId = option == null ? "" : option.getUserId();
        String role = normalizedRole(option == null ? data.getBackendMemberRole(memberName) : option.getRole());
        BackendWorkspace workspace = data.getActiveBackendWorkspace();
        boolean actorIsOwner = workspace != null && "OWNER".equalsIgnoreCase(workspace.getRole());
        boolean canChange = data.canManageActiveBackendWorkspace()
                && !memberUserId.equals(data.getCurrentBackendUserId())
                && !"OWNER".equals(role)
                && (actorIsOwner || !"ADMIN".equals(role));
        if (!canChange) {
            return roleLabel(role);
        }
        JComboBox<String> roleBox = new JComboBox<String>(actorIsOwner
                ? new String[]{"ADMIN", "MEMBER", "VIEWER"}
                : new String[]{"MEMBER", "VIEWER"});
        roleBox.setSelectedItem(role);
        roleBox.setPreferredSize(new Dimension(92, 24));
        roleBox.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        roleBox.setFocusable(false);
        roleBox.setEnabled(!backendActionRunning);
        roleBox.addActionListener(e -> {
            String selected = (String) roleBox.getSelectedItem();
            if (selected != null && !selected.equals(role)) {
                runBackendHouseholdAction("", () -> memberUserId.isEmpty()
                        ? data.changeBackendMemberRole(memberName, selected)
                        : data.changeBackendMemberRoleById(memberUserId, selected));
            }
        });
        return roleBox;
    }

    private MemberOption memberOptionByLabel(String label) {
        for (MemberOption option : data.getBackendMemberOptions()) {
            if (option.getLabel().equals(label) || option.getDisplayName().equals(label)) {
                return option;
            }
        }
        return null;
    }

    private List<String> labelsFromMemberOptions(List<MemberOption> options) {
        List<String> labels = new ArrayList<String>();
        for (MemberOption option : options) {
            labels.add(option.getLabel());
        }
        return labels;
    }

    private void selectMemberOption(JComboBox<MemberOption> comboBox, String userId, String label) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            MemberOption option = comboBox.getItemAt(i);
            if ((!option.getUserId().isEmpty() && option.getUserId().equals(userId))
                    || option.getLabel().equals(label)
                    || option.getDisplayName().equals(label)) {
                comboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    private JLabel roleLabel(String role) {
        JLabel label = new JLabel(role);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(AppColors.TEXT_MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        return label;
    }

    private String normalizedRole(String role) {
        return role == null || role.trim().isEmpty() ? "MEMBER" : role.trim().toUpperCase(Locale.ROOT);
    }

    private void refreshInvitationsPanel() {
        if (invitationsPanel == null) {
            return;
        }
        invitationsPanel.removeAll();
        if (!data.isBackendSessionActive()) {
            invitationsPanel.revalidate();
            invitationsPanel.repaint();
            return;
        }

        boolean hasRows = false;
        List<BackendInvitation> received = pendingOnly(data.getBackendReceivedInvitations());
        if (!received.isEmpty()) {
            addSmallSectionTitle(invitationsPanel, "Invitaciones recibidas");
            for (BackendInvitation invitation : received) {
                invitationsPanel.add(invitationRow(
                        invitation.getWorkspaceName() + " - " + invitation.getRole(),
                        "Aceptar",
                        () -> data.acceptBackendInvitation(invitation.getId()),
                        "Rechazar",
                        () -> data.rejectBackendInvitation(invitation.getId())));
                invitationsPanel.add(Box.createVerticalStrut(4));
            }
            hasRows = true;
        }

        List<BackendInvitation> workspaceInvitations = data.getBackendWorkspaceInvitations();
        if (!workspaceInvitations.isEmpty()) {
            invitationsPanel.add(Box.createVerticalStrut(4));
            addSmallSectionTitle(invitationsPanel, "Invitaciones del workspace");
            for (BackendInvitation invitation : workspaceInvitations) {
                String detail = invitation.getInvitedEmail() + " - " + invitation.getRole() + " - " + invitation.getStatus();
                if (invitation.isPending()) {
                    invitationsPanel.add(invitationRow(
                            detail,
                            "Cancelar",
                            () -> data.cancelBackendInvitation(invitation.getId()),
                            "",
                            null));
                } else {
                    invitationsPanel.add(readOnlyInvitationRow(detail));
                }
                invitationsPanel.add(Box.createVerticalStrut(4));
            }
            hasRows = true;
        }

        if (!hasRows) {
            JLabel empty = new JLabel("Sin invitaciones pendientes.");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            empty.setForeground(AppColors.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            invitationsPanel.add(empty);
        }
        invitationsPanel.revalidate();
        invitationsPanel.repaint();
    }

    private List<BackendInvitation> pendingOnly(List<BackendInvitation> invitations) {
        List<BackendInvitation> result = new ArrayList<BackendInvitation>();
        for (BackendInvitation invitation : invitations) {
            if (invitation.isPending()) {
                result.add(invitation);
            }
        }
        return result;
    }

    private void addSmallSectionTitle(JPanel panel, String text) {
        JLabel title = new JLabel(text);
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(AppColors.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(4));
    }

    private JComponent readOnlyInvitationRow(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(AppColors.TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JComponent invitationRow(String text, String primaryLabel, BackendAction primary,
                                     String secondaryLabel, BackendAction secondary) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(AppColors.TEXT_SECONDARY);
        row.add(label, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);
        if (primary != null && primaryLabel != null && !primaryLabel.trim().isEmpty()) {
            actions.add(compactActionButton(primaryLabel, primary));
        }
        if (secondary != null && secondaryLabel != null && !secondaryLabel.trim().isEmpty()) {
            actions.add(compactActionButton(secondaryLabel, secondary));
        }
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private JButton compactActionButton(String label, BackendAction action) {
        JButton button = new JButton(label);
        button.setFont(new Font("Segoe UI", Font.BOLD, 10));
        button.setFocusPainted(false);
        button.setEnabled(!backendActionRunning);
        button.addActionListener(e -> runBackendHouseholdAction("", action));
        return button;
    }

    private JPanel buildDebtsCard() {
        JPanel card = card();
        card.setLayout(new BorderLayout(0, 8));
        JLabel title = new JLabel("💳 Resumen de deudas compartidas");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(AppColors.TEXT_PRIMARY);

        debtCards = new JPanel();
        debtCards.setLayout(new BoxLayout(debtCards, BoxLayout.Y_AXIS));
        debtCards.setOpaque(false);
        refreshDebtCards();

        card.add(title, BorderLayout.NORTH);
        card.add(debtCards, BorderLayout.CENTER);
        return card;
    }

    private void refreshDebtCards() {
        if (debtCards == null) {
            return;
        }
        debtCards.removeAll();
        Map<String, Double> balances = data.calcularDeudas();
        double totalExpenses = data.getGastosHogar().stream()
                .filter(GastoHogar::isDividido)
                .mapToDouble(GastoHogar::getMonto)
                .sum();

        JLabel total = new JLabel("Total gastos compartidos: $" + nf.format((long) totalExpenses));
        total.setFont(new Font("Segoe UI", Font.BOLD, 12));
        total.setForeground(AppColors.TEXT_PRIMARY);
        total.setAlignmentX(Component.LEFT_ALIGNMENT);
        debtCards.add(total);
        debtCards.add(Box.createVerticalStrut(8));

        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

            JLabel name = new JLabel(entry.getKey() + ":");
            name.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            name.setForeground(AppColors.TEXT_SECONDARY);

            double value = entry.getValue();
            String message = value >= 0 ? "le deben $" + nf.format((long) value) : "debe $" + nf.format((long) Math.abs(value));
            JLabel status = new JLabel(message);
            status.setFont(new Font("Segoe UI", Font.BOLD, 11));
            status.setForeground(value >= 0 ? AppColors.ACCENT_GREEN : AppColors.ACCENT_RED);

            row.add(name, BorderLayout.WEST);
            row.add(status, BorderLayout.EAST);
            debtCards.add(row);
            debtCards.add(Box.createVerticalStrut(4));
        }
        if (!data.getSettlements().isEmpty()) {
            debtCards.add(Box.createVerticalStrut(8));
            JLabel historyTitle = new JLabel("Ultimas liquidaciones");
            historyTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
            historyTitle.setForeground(AppColors.TEXT_PRIMARY);
            historyTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            debtCards.add(historyTitle);
            debtCards.add(Box.createVerticalStrut(4));
            for (int i = 0; i < Math.min(3, data.getSettlements().size()); i++) {
                Settlement settlement = data.getSettlements().get(i);
                JLabel row = new JLabel(settlement.getFromMember() + " -> " + settlement.getToMember()
                        + " $" + nf.format((long) settlement.getAmount())
                        + " (" + settlement.getDate().format(dtf) + ")");
                row.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                row.setForeground(AppColors.TEXT_MUTED);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                debtCards.add(row);
            }
        }
        debtCards.revalidate();
        debtCards.repaint();
    }

    private JPanel buildTableSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppColors.MAIN_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 20, 16, 20));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel("Historial de gastos del hogar");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(AppColors.TEXT_PRIMARY);
        titleRow.add(title, BorderLayout.WEST);

        String[] columns = {"Descripcion", "Categoria", "Monto", "Pagado por", "Fecha", "Dividido", "Acciones"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0xEBF3FE));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(0xf8fafc));
        table.getTableHeader().setForeground(AppColors.TEXT_SECONDARY);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(6).setMaxWidth(80);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setBackground(selected ? new Color(0xEBF3FE) : (row % 2 == 0 ? Color.WHITE : new Color(0xf9fafb)));
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (column == 2) {
                    setForeground(AppColors.ACCENT_RED);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else if (column == 5) {
                    setForeground("Si".equals(value) ? AppColors.ACCENT_GREEN : AppColors.TEXT_MUTED);
                    setFont(new Font("Segoe UI", Font.BOLD, 11));
                } else {
                    setForeground(AppColors.TEXT_SECONDARY);
                    setFont(new Font("Segoe UI", Font.PLAIN, 12));
                }
                return this;
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 6 && currentList != null && row < currentList.size()) {
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem edit = new JMenuItem("Editar");
                    JMenuItem delete = new JMenuItem("Eliminar");
                    edit.addActionListener(ev -> showGastoDialog(currentList.get(row)));
                    delete.addActionListener(ev -> {
                        int result = JOptionPane.showConfirmDialog(FinanzasHogarPanel.this, "Eliminar este gasto?", "Confirmar", JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) {
                            data.removeGastoHogar(currentList.get(row));
                        }
                    });
                    menu.add(edit);
                    menu.add(delete);
                    menu.show(table, e.getX(), e.getY());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(titleRow, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        refreshTable();
        return panel;
    }

    private void refreshTable() {
        if (tableModel == null) {
            return;
        }
        currentList = data.getGastosHogar();
        tableModel.setRowCount(0);
        for (GastoHogar gasto : currentList) {
            tableModel.addRow(new Object[]{
                    gasto.getDescripcion(),
                    gasto.getCategoria(),
                    "$" + nf.format((long) gasto.getMonto()),
                    gasto.getPagadoPor(),
                    gasto.getFecha().format(dtf),
                    gasto.isDividido() ? "Si" : "No",
                    "⋮"
            });
        }
    }

    private void runBackendHouseholdAction(String successMessage, BackendAction action) {
        if (backendActionRunning || action == null) {
            return;
        }
        backendActionRunning = true;
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        refresh();
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return action.run();
            }

            @Override
            protected void done() {
                backendActionRunning = false;
                setCursor(Cursor.getDefaultCursor());
                boolean ok = false;
                try {
                    ok = get();
                } catch (Exception ex) {
                    ok = false;
                }
                if (ok && successMessage != null && !successMessage.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(FinanzasHogarPanel.this, successMessage, "Hogar", JOptionPane.INFORMATION_MESSAGE);
                } else if (!ok) {
                    String message = data.getLastErrorMessage();
                    JOptionPane.showMessageDialog(FinanzasHogarPanel.this,
                            message == null || message.trim().isEmpty() ? "No fue posible completar la accion." : message,
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                refresh();
            }
        }.execute();
    }

    private void refresh() {
        if (membersListPanel != null) {
            refreshMembersList(membersListPanel);
        }
        refreshInvitationsPanel();
        refreshDebtCards();
        refreshTable();
    }

    private interface BackendAction {
        boolean run();
    }

    public void openNewHouseholdExpenseDialog() {
        showGastoDialog(null);
    }

    private void showGastoDialog(GastoHogar existing) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this) instanceof JFrame ? (JFrame) SwingUtilities.getWindowAncestor(this) : null,
                isEdit ? "Editar gasto" : "Nuevo gasto del hogar",
                true);
        dialog.setSize(520, 460);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 4, 7, 4);

        JTextField descField = new JTextField(isEdit ? existing.getDescripcion() : "");
        JComboBox<String> categoryBox = new JComboBox<String>(data.getCategoryNames(FinancialCategory.Kind.HOUSEHOLD));
        if (isEdit) {
            categoryBox.setSelectedItem(existing.getCategoria());
        }
        JTextField amountField = new JTextField(isEdit ? String.valueOf((long) existing.getMonto()) : "");
        boolean backendSession = data.isBackendSessionActive();
        List<MemberOption> memberOptions = data.getBackendMemberOptions();
        List<String> members = backendSession ? labelsFromMemberOptions(memberOptions) : data.getMiembrosHogar();
        if (members.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Agrega al menos un miembro del hogar antes de registrar gastos compartidos.", "Hogar sin miembros", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<?> paidByBox;
        if (backendSession) {
            JComboBox<MemberOption> backendPaidByBox = new JComboBox<MemberOption>(memberOptions.toArray(new MemberOption[0]));
            if (isEdit) {
                selectMemberOption(backendPaidByBox, existing.getBackendPaidByUserId(), existing.getPagadoPor());
            }
            paidByBox = backendPaidByBox;
        } else {
            JComboBox<String> localPaidByBox = new JComboBox<String>(members.toArray(new String[0]));
            if (isEdit) {
                localPaidByBox.setSelectedItem(existing.getPagadoPor());
            }
            paidByBox = localPaidByBox;
        }
        JTextField dateField = new JTextField(isEdit ? existing.getFecha().format(dtf) : LocalDate.now().format(dtf));
        JCheckBox split = new JCheckBox("Dividir entre todos");
        split.setBackground(Color.WHITE);
        if (isEdit) {
            split.setSelected(existing.isDividido());
        }
        JComboBox<GastoHogar.SplitMethod> splitMethodBox = new JComboBox<GastoHogar.SplitMethod>(GastoHogar.SplitMethod.values());
        if (isEdit) {
            splitMethodBox.setSelectedItem(existing.getSplitMethod());
        }
        JTextArea splitValues = new JTextArea(defaultSplitValues(existing, members), 4, 22);
        splitValues.setLineWrap(true);
        splitValues.setWrapStyleWord(true);
        JScrollPane splitScroll = new JScrollPane(splitValues);

        Runnable refreshSplitControls = () -> {
            boolean customSplit = split.isSelected() && splitMethodBox.getSelectedItem() != GastoHogar.SplitMethod.EQUAL;
            splitMethodBox.setEnabled(split.isSelected());
            splitValues.setEnabled(customSplit);
            splitValues.setBackground(customSplit ? Color.WHITE : new Color(0xf3f4f6));
        };
        split.addActionListener(e -> refreshSplitControls.run());
        splitMethodBox.addActionListener(e -> refreshSplitControls.run());
        refreshSplitControls.run();

        addFormRow(panel, gbc, 0, "Descripcion:", descField);
        addFormRow(panel, gbc, 1, "Categoria:", categoryBox);
        addFormRow(panel, gbc, 2, "Monto ($):", amountField);
        addFormRow(panel, gbc, 3, "Pagado por:", paidByBox);
        addFormRow(panel, gbc, 4, "Fecha:", dateField);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.4;
        panel.add(new JLabel(""), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        panel.add(split, gbc);
        addFormRow(panel, gbc, 6, "Metodo:", splitMethodBox);
        addFormRow(panel, gbc, 7, "Valores por miembro:", splitScroll);

        RoundedButton save = new RoundedButton(isEdit ? "Guardar cambios" : "Agregar gasto", AppColors.CARD_AHORROS);
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 2;
        panel.add(save, gbc);
        save.addActionListener(e -> {
            try {
                String desc = descField.getText().trim();
                if (desc.isEmpty()) {
                    throw new IllegalArgumentException();
                }
                BigDecimal amount = Money.parseFlexible(amountField.getText());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException();
                }
                Object payer = paidByBox.getSelectedItem();
                String paidByName = String.valueOf(payer);
                String paidByUserId = "";
                if (payer instanceof MemberOption) {
                    MemberOption option = (MemberOption) payer;
                    paidByName = option.getLabel();
                    paidByUserId = option.getUserId();
                }
                GastoHogar nuevo = new GastoHogar(
                        desc,
                        (String) categoryBox.getSelectedItem(),
                        amount,
                        paidByName,
                        LocalDate.parse(dateField.getText().trim(), dtf),
                        split.isSelected());
                nuevo.setBackendPaidByUserId(paidByUserId);
                applySplit(nuevo, members, splitValues.getText(), (GastoHogar.SplitMethod) splitMethodBox.getSelectedItem());
                if (isEdit) {
                    data.removeGastoHogar(existing);
                }
                data.addGastoHogar(nuevo);
                dialog.dispose();
            } catch (Exception ex) {
                String message = ex.getMessage() == null || ex.getMessage().trim().isEmpty() ? "Datos invalidos." : ex.getMessage();
                JOptionPane.showMessageDialog(dialog, message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void showSettlementDialog() {
        boolean backendSession = data.isBackendSessionActive();
        List<MemberOption> memberOptions = data.getBackendMemberOptions();
        List<String> members = backendSession ? labelsFromMemberOptions(memberOptions) : data.getMiembrosHogar();
        if (members.size() < 2) {
            JOptionPane.showMessageDialog(this, "Agrega al menos dos miembros para registrar liquidaciones.", "Hogar incompleto", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this) instanceof JFrame ? (JFrame) SwingUtilities.getWindowAncestor(this) : null,
                "Registrar liquidacion",
                true);
        dialog.setSize(420, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(7, 4, 7, 4);

        JComboBox<?> fromBox;
        JComboBox<?> toBox;
        if (backendSession) {
            fromBox = new JComboBox<MemberOption>(memberOptions.toArray(new MemberOption[0]));
            toBox = new JComboBox<MemberOption>(memberOptions.toArray(new MemberOption[0]));
        } else {
            fromBox = new JComboBox<String>(members.toArray(new String[0]));
            toBox = new JComboBox<String>(members.toArray(new String[0]));
        }
        JTextField amountField = new JTextField();
        JTextField dateField = new JTextField(LocalDate.now().format(dtf));
        JTextField noteField = new JTextField();

        addFormRow(panel, gbc, 0, "Paga:", fromBox);
        addFormRow(panel, gbc, 1, "Recibe:", toBox);
        addFormRow(panel, gbc, 2, "Monto:", amountField);
        addFormRow(panel, gbc, 3, "Fecha:", dateField);
        addFormRow(panel, gbc, 4, "Nota:", noteField);

        RoundedButton save = new RoundedButton("Guardar liquidacion", AppColors.ACCENT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(save, gbc);

        save.addActionListener(e -> {
            try {
                Object from = fromBox.getSelectedItem();
                Object to = toBox.getSelectedItem();
                String fromName = String.valueOf(from);
                String toName = String.valueOf(to);
                String fromUserId = "";
                String toUserId = "";
                if (from instanceof MemberOption) {
                    MemberOption option = (MemberOption) from;
                    fromName = option.getLabel();
                    fromUserId = option.getUserId();
                }
                if (to instanceof MemberOption) {
                    MemberOption option = (MemberOption) to;
                    toName = option.getLabel();
                    toUserId = option.getUserId();
                }
                Settlement settlement = new Settlement(
                        fromName,
                        toName,
                        Money.parseFlexible(amountField.getText()),
                        LocalDate.parse(dateField.getText().trim(), dtf),
                        noteField.getText());
                settlement.setBackendFromUserId(fromUserId);
                settlement.setBackendToUserId(toUserId);
                data.addSettlement(settlement);
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void applySplit(GastoHogar gasto, List<String> members, String rawValues, GastoHogar.SplitMethod method) {
        if (!gasto.isDividido()) {
            return;
        }
        if (method == GastoHogar.SplitMethod.PERCENTAGE) {
            gasto.definePercentageSplit(parseMemberValues(rawValues, members));
        } else if (method == GastoHogar.SplitMethod.CUSTOM_AMOUNT) {
            gasto.defineCustomAmountSplit(parseMemberValues(rawValues, members));
        } else {
            gasto.defineEqualSplit(members);
        }
    }

    private Map<String, BigDecimal> parseMemberValues(String rawValues, List<String> members) {
        Map<String, BigDecimal> values = new LinkedHashMap<String, BigDecimal>();
        String[] lines = rawValues == null ? new String[0] : rawValues.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("[:=]", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Usa formato Nombre=valor, una linea por miembro.");
            }
            String member = parts[0].trim();
            if (!members.contains(member)) {
                throw new IllegalArgumentException("El miembro no existe: " + member);
            }
            values.put(member, Money.parseFlexible(parts[1].trim()));
        }
        return values;
    }

    private String defaultSplitValues(GastoHogar existing, List<String> members) {
        if (existing != null && !existing.getSplitAmounts().isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, BigDecimal> entry : existing.getSplitAmounts().entrySet()) {
                builder.append(entry.getKey()).append("=").append(entry.getValue().toPlainString()).append("\n");
            }
            return builder.toString();
        }
        StringBuilder builder = new StringBuilder();
        for (String member : members) {
            builder.append(member).append("=0").append("\n");
        }
        return builder.toString();
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.4;
        JLabel view = new JLabel(label);
        view.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(view, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.6;
        panel.add(component, gbc);
    }

    private JPanel card() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(AppColors.BORDER);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        return card;
    }
}
