package com.finanzas.ui;

import com.finanzas.data.DataManager;
import com.finanzas.data.ExportService;
import com.finanzas.data.NotificationItem;
import com.finanzas.model.FinancialCategory;
import com.finanzas.model.Usuario;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.AppIcons;
import com.finanzas.ui.components.AvatarView;
import com.finanzas.ui.components.CardPanel;
import com.finanzas.ui.components.FormSupport;
import com.finanzas.ui.components.PageHeader;
import com.finanzas.ui.components.RoundedButton;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class ConfiguracionPanel extends JPanel {
    private static final String SECTION_PROFILE = "perfil";
    private static final String SECTION_NOTIFICATIONS = "notificaciones";
    private static final String SECTION_CATEGORIES = "categorias";
    private static final String SECTION_CURRENCY = "moneda";
    private static final String SECTION_SECURITY = "seguridad";
    private static final String SECTION_EXPORT = "exportar";
    private static final String SECTION_ABOUT = "acerca";

    private final DataManager data = DataManager.getInstance();
    private final ExportService exportService = ExportService.getInstance();
    private String activeSection = SECTION_PROFILE;

    public ConfiguracionPanel() {
        setBackground(AppColors.MAIN_BG);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        removeAll();
        add(new PageHeader("Configuracion", "Personaliza tu experiencia en la aplicacion"), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMenu(), buildContent(activeSection));
        splitPane.setDividerLocation(230);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);
        splitPane.setBackground(AppColors.MAIN_BG);
        add(splitPane, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JPanel buildMenu() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        addMenuItem(panel, AppIcons.HOME, "Perfil de usuario", SECTION_PROFILE);
        addMenuItem(panel, AppIcons.ALERT, "Notificaciones", SECTION_NOTIFICATIONS);
        addMenuItem(panel, AppIcons.BUDGET, "Categorias", SECTION_CATEGORIES);
        addMenuItem(panel, AppIcons.BUDGET, "Moneda y region", SECTION_CURRENCY);
        addMenuItem(panel, AppIcons.INFO, "Seguridad", SECTION_SECURITY);
        addMenuItem(panel, AppIcons.EXPORT, "Exportar datos", SECTION_EXPORT);
        addMenuItem(panel, AppIcons.APP, "Acerca de", SECTION_ABOUT);
        return panel;
    }

    private void addMenuItem(JPanel parent, String iconText, String labelText, String section) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        boolean active = activeSection.equals(section);
        row.setBackground(active ? new Color(0xEBF3FE) : Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(iconText);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(active ? AppColors.ACCENT_BLUE : AppColors.TEXT_PRIMARY);

        row.add(icon);
        row.add(label);
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                activeSection = section;
                buildUI();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                row.setBackground(new Color(0xF5F8FF));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                row.setBackground(activeSection.equals(section) ? new Color(0xEBF3FE) : Color.WHITE);
            }
        });

        parent.add(row);
        parent.add(Box.createVerticalStrut(2));
    }

    private JPanel buildContent(String section) {
        switch (section) {
            case SECTION_PROFILE:
                return buildPerfilPanel();
            case SECTION_NOTIFICATIONS:
                return buildNotificacionesPanel();
            case SECTION_CATEGORIES:
                return buildCategoriasPanel();
            case SECTION_CURRENCY:
                return buildMonedaPanel();
            case SECTION_SECURITY:
                return buildSeguridadPanel();
            case SECTION_EXPORT:
                return buildExportarPanel();
            default:
                return buildAcercaPanel();
        }
    }

    private JPanel buildPerfilPanel() {
        Usuario user = data.getUsuario();
        JPanel panel = createCard();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = FormSupport.baseConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buildAvatarSection(user), gbc);
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField nombreField = new JTextField(user.getNombre());
        JTextField apellidoField = new JTextField(user.getApellido());
        JTextField emailField = new JTextField(user.getEmail());
        JTextField ciudadField = new JTextField(user.getCiudad());
        JTextField paisField = new JTextField(user.getPais());

        FormSupport.addFormRow(panel, gbc, 1, "Nombre:", nombreField);
        FormSupport.addFormRow(panel, gbc, 2, "Apellido:", apellidoField);
        FormSupport.addFormRow(panel, gbc, 3, "Correo:", emailField);
        FormSupport.addFormRow(panel, gbc, 4, "Ciudad:", ciudadField);
        FormSupport.addFormRow(panel, gbc, 5, "Pais:", paisField);

        RoundedButton saveButton = new RoundedButton("Guardar perfil", AppColors.ACCENT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        panel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            try {
                data.updateProfile(
                        nombreField.getText(),
                        apellidoField.getText(),
                        emailField.getText(),
                        ciudadField.getText(),
                        paisField.getText());
                JOptionPane.showMessageDialog(this, "Perfil actualizado correctamente.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                buildUI();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return wrapScroll(panel);
    }

    private JComponent buildAvatarSection(Usuario user) {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        AvatarView avatar = new AvatarView(user, 88, AppColors.ACCENT_BLUE, Color.WHITE, 24);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel helper = new JLabel("Imagen de perfil");
        helper.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        helper.setForeground(AppColors.TEXT_MUTED);
        helper.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        buttons.setOpaque(false);

        RoundedButton uploadButton = new RoundedButton("Subir imagen", AppColors.ACCENT_BLUE);
        RoundedButton removeButton = new RoundedButton("Quitar", AppColors.TEXT_MUTED);
        uploadButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        removeButton.setFont(new Font("Segoe UI", Font.BOLD, 11));

        uploadButton.addActionListener(e -> seleccionarImagenPerfil());
        removeButton.addActionListener(e -> {
            data.clearProfileImage();
            buildUI();
        });

        buttons.add(uploadButton);
        buttons.add(removeButton);
        container.add(avatar);
        container.add(Box.createVerticalStrut(8));
        container.add(helper);
        container.add(buttons);
        return container;
    }

    private JPanel buildNotificacionesPanel() {
        Usuario user = data.getUsuario();
        JPanel panel = createCard();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = sectionTitle("Preferencias de notificaciones");
        panel.add(title);
        panel.add(Box.createVerticalStrut(16));

        JCheckBox presupuesto = createToggle(panel, AppIcons.ALERT + " Alertas de presupuesto", "Notifica cuando te acercas al limite", user.isNotifPresupuesto());
        JCheckBox metas = createToggle(panel, AppIcons.GOALS + " Progreso de metas", "Resumen semanal de ahorro", user.isNotifMetas());
        JCheckBox consejos = createToggle(panel, AppIcons.IDEA + " Consejos financieros", "Recomendaciones personalizadas", user.isNotifConsejos());

        RoundedButton saveButton = new RoundedButton("Guardar preferencias", AppColors.ACCENT_BLUE);
        saveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveButton.addActionListener(e -> {
            user.setNotifPresupuesto(presupuesto.isSelected());
            user.setNotifMetas(metas.isSelected());
            user.setNotifConsejos(consejos.isSelected());
            data.notifyListeners();
            JOptionPane.showMessageDialog(this, "Preferencias guardadas.", "Exito", JOptionPane.INFORMATION_MESSAGE);
        });

        panel.add(Box.createVerticalStrut(16));
        panel.add(saveButton);
        panel.add(Box.createVerticalStrut(20));
        panel.add(sectionTitle("Eventos activos"));
        panel.add(Box.createVerticalStrut(8));
        addNotificationPreview(panel, data.getNotifications());
        return wrapScroll(panel);
    }

    private void addNotificationPreview(JPanel panel, List<NotificationItem> notifications) {
        if (notifications.isEmpty()) {
            JLabel empty = new JLabel("No hay eventos financieros que requieran atencion.");
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            empty.setForeground(AppColors.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(empty);
            return;
        }
        for (int i = 0; i < Math.min(5, notifications.size()); i++) {
            NotificationItem item = notifications.get(i);
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
            row.setBackground(Color.WHITE);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER),
                    BorderFactory.createEmptyBorder(8, 0, 8, 0)));

            JLabel title = new JLabel(item.getTitle());
            title.setFont(new Font("Segoe UI", Font.BOLD, 12));
            title.setForeground(notificationPreviewColor(item));
            JLabel message = new JLabel(item.getMessage());
            message.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            message.setForeground(AppColors.TEXT_MUTED);
            row.add(title);
            row.add(message);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(row);
        }
    }

    private Color notificationPreviewColor(NotificationItem item) {
        if (item.getSeverity() == NotificationItem.Severity.CRITICAL) {
            return AppColors.ACCENT_RED;
        }
        if (item.getSeverity() == NotificationItem.Severity.WARNING) {
            return new Color(0xFF9800);
        }
        if (item.getSeverity() == NotificationItem.Severity.SUCCESS) {
            return AppColors.ACCENT_GREEN;
        }
        return AppColors.ACCENT_BLUE;
    }

    private JCheckBox createToggle(JPanel parent, String titleText, String description, boolean selected) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(Color.WHITE);

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subtitle = new JLabel(description);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(AppColors.TEXT_MUTED);

        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(selected);
        checkBox.setBackground(Color.WHITE);

        text.add(title);
        text.add(subtitle);
        row.add(text, BorderLayout.CENTER);
        row.add(checkBox, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(row);
        parent.add(Box.createVerticalStrut(2));
        return checkBox;
    }

    private JPanel buildCategoriasPanel() {
        JPanel panel = createCard();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(sectionTitle("Categorias personalizadas"));
        panel.add(Box.createVerticalStrut(12));
        panel.add(buildNewCategoryForm());
        panel.add(Box.createVerticalStrut(18));

        JPanel filters = new JPanel(new GridLayout(2, 4, 8, 6));
        filters.setOpaque(false);
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        JTextField searchField = new JTextField();
        JComboBox<String> kindFilter = new JComboBox<String>(new String[]{"Todas", "Ingreso", "Gasto", "Hogar"});
        JComboBox<String> stateFilter = new JComboBox<String>(new String[]{"Activas", "Archivadas", "Todas"});
        RoundedButton clear = new RoundedButton("Limpiar", AppColors.TEXT_MUTED);
        filters.add(labelSmall("Buscar"));
        filters.add(labelSmall("Tipo"));
        filters.add(labelSmall("Estado"));
        filters.add(new JLabel(""));
        filters.add(searchField);
        filters.add(kindFilter);
        filters.add(stateFilter);
        filters.add(clear);
        panel.add(filters);
        panel.add(Box.createVerticalStrut(12));

        JPanel categoryList = new JPanel();
        categoryList.setOpaque(false);
        categoryList.setLayout(new BoxLayout(categoryList, BoxLayout.Y_AXIS));
        categoryList.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(categoryList);

        Runnable refresh = () -> rebuildCategoryList(
                categoryList,
                kindFilterFromLabel((String) kindFilter.getSelectedItem()),
                (String) stateFilter.getSelectedItem(),
                searchField.getText());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh.run(); }
            public void removeUpdate(DocumentEvent e) { refresh.run(); }
            public void changedUpdate(DocumentEvent e) { refresh.run(); }
        });
        kindFilter.addActionListener(e -> refresh.run());
        stateFilter.addActionListener(e -> refresh.run());
        clear.addActionListener(e -> {
            searchField.setText("");
            kindFilter.setSelectedIndex(0);
            stateFilter.setSelectedIndex(0);
            refresh.run();
        });
        refresh.run();
        return wrapScroll(panel);
    }

    private JPanel buildNewCategoryForm() {
        JPanel form = new JPanel(new GridLayout(2, 4, 8, 6));
        form.setOpaque(false);
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JComboBox<String> kindBox = new JComboBox<String>(new String[]{"Ingreso", "Gasto", "Hogar"});
        JTextField nameField = new JTextField();
        JTextField iconField = new JTextField();
        JTextField colorField = new JTextField("#1a73e8");
        JButton colorButton = colorButton(colorField);
        RoundedButton addButton = new RoundedButton("Crear", AppColors.ACCENT_BLUE);

        form.add(labelSmall("Tipo"));
        form.add(labelSmall("Nombre"));
        form.add(labelSmall("Icono"));
        form.add(labelSmall("Color"));
        form.add(kindBox);
        form.add(nameField);
        form.add(iconField);
        form.add(colorFieldWithButton(colorField, colorButton));

        JPanel wrapper = new JPanel(new BorderLayout(10, 0));
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(form, BorderLayout.CENTER);
        wrapper.add(addButton, BorderLayout.EAST);

        addButton.addActionListener(e -> {
            try {
                data.addCategory(kindFromLabel((String) kindBox.getSelectedItem()), nameField.getText(), iconField.getText(), colorField.getText());
                buildUI();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return wrapper;
    }

    private void rebuildCategoryList(JPanel panel, FinancialCategory.Kind kindFilter, String stateFilter, String query) {
        panel.removeAll();
        int before = panel.getComponentCount();
        if (kindFilter == null) {
            addCategoryGroup(panel, FinancialCategory.Kind.INCOME, stateFilter, query);
            addCategoryGroup(panel, FinancialCategory.Kind.EXPENSE, stateFilter, query);
            addCategoryGroup(panel, FinancialCategory.Kind.HOUSEHOLD, stateFilter, query);
        } else {
            addCategoryGroup(panel, kindFilter, stateFilter, query);
        }
        if (panel.getComponentCount() == before) {
            JLabel empty = new JLabel("Sin resultados");
            empty.setForeground(AppColors.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(empty);
        }
        panel.revalidate();
        panel.repaint();
    }

    private void addCategoryGroup(JPanel panel, FinancialCategory.Kind kind, String stateFilter, String query) {
        JLabel heading = sectionTitle(categoryKindLabel(kind));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        List<FinancialCategory> categories = data.getCategories(kind, true);
        int rows = 0;
        for (FinancialCategory category : categories) {
            if (!matchesCategoryFilter(category, stateFilter, query)) {
                continue;
            }
            if (rows == 0) {
                panel.add(heading);
                panel.add(Box.createVerticalStrut(6));
            }
            panel.add(categoryRow(category));
            panel.add(Box.createVerticalStrut(4));
            rows++;
        }
        if (rows > 0) {
            panel.add(Box.createVerticalStrut(12));
        }
    }

    private JPanel categoryRow(FinancialCategory category) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)));

        String suffix = category.isArchived() ? " (archivada)" : "";
        JLabel title = new JLabel(category.getName() + suffix);
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(category.isArchived() ? AppColors.TEXT_MUTED : AppColors.TEXT_PRIMARY);
        JLabel detail = new JLabel(category.getIcon() + "  " + category.getColor());
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detail.setForeground(AppColors.TEXT_MUTED);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(title);
        text.add(detail);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        JButton edit = new JButton("Editar");
        JButton archive = new JButton(category.isArchived() ? "Restaurar" : "Archivar");
        edit.addActionListener(e -> editCategory(category));
        archive.addActionListener(e -> {
            if (category.isArchived()) {
                data.addCategory(category.getKind(), category.getName(), category.getIcon(), category.getColor());
            } else {
                data.archiveCategory(category);
            }
            buildUI();
        });
        actions.add(edit);
        actions.add(archive);

        row.add(text, BorderLayout.CENTER);
        row.add(actions, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private void editCategory(FinancialCategory category) {
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        JTextField nameField = new JTextField(category.getName());
        JTextField iconField = new JTextField(category.getIcon());
        JTextField colorField = new JTextField(category.getColor());
        JButton colorButton = colorButton(colorField);
        form.add(new JLabel("Nombre:"));
        form.add(nameField);
        form.add(new JLabel("Icono:"));
        form.add(iconField);
        form.add(new JLabel("Color:"));
        form.add(colorFieldWithButton(colorField, colorButton));

        int option = JOptionPane.showConfirmDialog(this, form, "Editar categoria", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            try {
                data.updateCategory(category, nameField.getText(), iconField.getText(), colorField.getText());
                buildUI();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JLabel labelSmall(String value) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(AppColors.TEXT_MUTED);
        return label;
    }

    private JPanel colorFieldWithButton(JTextField colorField, JButton colorButton) {
        JPanel panel = new JPanel(new BorderLayout(4, 0));
        panel.setOpaque(false);
        panel.add(colorField, BorderLayout.CENTER);
        panel.add(colorButton, BorderLayout.EAST);
        return panel;
    }

    private JButton colorButton(JTextField colorField) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(34, 28));
        button.setToolTipText("Elegir color");
        updateColorButton(button, colorField.getText());
        button.addActionListener(e -> {
            Color selected = JColorChooser.showDialog(this, "Elegir color", parseColor(colorField.getText()));
            if (selected != null) {
                String hex = String.format("#%02x%02x%02x", selected.getRed(), selected.getGreen(), selected.getBlue());
                colorField.setText(hex);
                updateColorButton(button, hex);
            }
        });
        colorField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateColorButton(button, colorField.getText()); }
            public void removeUpdate(DocumentEvent e) { updateColorButton(button, colorField.getText()); }
            public void changedUpdate(DocumentEvent e) { updateColorButton(button, colorField.getText()); }
        });
        return button;
    }

    private void updateColorButton(JButton button, String value) {
        Color color = parseColor(value);
        button.setBackground(color);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(contrastColor(color), 1));
    }

    private Color parseColor(String value) {
        try {
            String color = value == null || value.trim().isEmpty() ? "#1a73e8" : value.trim();
            if (!color.matches("^#[0-9a-fA-F]{6}$")) {
                return AppColors.ACCENT_BLUE;
            }
            return Color.decode(color);
        } catch (Exception ex) {
            return AppColors.ACCENT_BLUE;
        }
    }

    private Color contrastColor(Color color) {
        double luminance = (0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue()) / 255.0;
        return luminance > 0.55 ? AppColors.TEXT_PRIMARY : Color.WHITE;
    }

    private boolean matchesCategoryFilter(FinancialCategory category, String stateFilter, String query) {
        boolean stateMatches = "Todas".equals(stateFilter)
                || ("Archivadas".equals(stateFilter) && category.isArchived())
                || ("Activas".equals(stateFilter) && !category.isArchived());
        String normalizedQuery = normalizeSearch(query);
        boolean queryMatches = normalizedQuery.isEmpty()
                || normalizeSearch(category.getName()).contains(normalizedQuery)
                || normalizeSearch(category.getIcon()).contains(normalizedQuery);
        return stateMatches && queryMatches;
    }

    private FinancialCategory.Kind kindFilterFromLabel(String label) {
        if ("Ingreso".equals(label)) {
            return FinancialCategory.Kind.INCOME;
        }
        if ("Gasto".equals(label)) {
            return FinancialCategory.Kind.EXPENSE;
        }
        if ("Hogar".equals(label)) {
            return FinancialCategory.Kind.HOUSEHOLD;
        }
        return null;
    }

    private String normalizeSearch(String value) {
        String trimmed = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("\\s+", " ");
    }

    private FinancialCategory.Kind kindFromLabel(String label) {
        if ("Ingreso".equals(label)) {
            return FinancialCategory.Kind.INCOME;
        }
        if ("Hogar".equals(label)) {
            return FinancialCategory.Kind.HOUSEHOLD;
        }
        return FinancialCategory.Kind.EXPENSE;
    }

    private String categoryKindLabel(FinancialCategory.Kind kind) {
        if (kind == FinancialCategory.Kind.INCOME) {
            return "Ingresos";
        }
        if (kind == FinancialCategory.Kind.HOUSEHOLD) {
            return "Hogar";
        }
        return "Gastos";
    }

    private JPanel buildMonedaPanel() {
        JPanel panel = createCard();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = FormSupport.baseConstraints();

        JComboBox<String> monedaBox = new JComboBox<String>(new String[]{
                "COP - Peso Colombiano",
                "USD - Dolar Americano",
                "EUR - Euro",
                "MXN - Peso Mexicano"
        });
        selectMoneda(monedaBox, data.getUsuario().getMoneda());

        JComboBox<String> formatBox = new JComboBox<String>(new String[]{
                "$1.000.000,00",
                "$1,000,000.00",
                "1 000 000 COP"
        });

        FormSupport.addFormRow(panel, gbc, 0, "Moneda:", monedaBox);
        FormSupport.addFormRow(panel, gbc, 1, "Formato:", formatBox);

        RoundedButton saveButton = new RoundedButton("Guardar", AppColors.ACCENT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            String selected = (String) monedaBox.getSelectedItem();
            if (selected != null) {
                data.getUsuario().setMoneda(selected.substring(0, 3));
            }
            data.notifyListeners();
            JOptionPane.showMessageDialog(this, "Configuracion de moneda guardada.", "Exito", JOptionPane.INFORMATION_MESSAGE);
        });

        return wrapScroll(panel);
    }

    private void selectMoneda(JComboBox<String> monedaBox, String moneda) {
        if ("USD".equals(moneda)) {
            monedaBox.setSelectedItem("USD - Dolar Americano");
            return;
        }
        if ("EUR".equals(moneda)) {
            monedaBox.setSelectedItem("EUR - Euro");
            return;
        }
        if ("MXN".equals(moneda)) {
            monedaBox.setSelectedItem("MXN - Peso Mexicano");
            return;
        }
        monedaBox.setSelectedItem("COP - Peso Colombiano");
    }

    private JPanel buildSeguridadPanel() {
        JPanel panel = createCard();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = FormSupport.baseConstraints();

        JPasswordField actualField = new JPasswordField();
        JPasswordField nuevaField = new JPasswordField();
        JPasswordField confirmarField = new JPasswordField();

        FormSupport.addFormRow(panel, gbc, 0, "Contrasena actual:", actualField);
        FormSupport.addFormRow(panel, gbc, 1, "Nueva contrasena:", nuevaField);
        FormSupport.addFormRow(panel, gbc, 2, "Confirmar nueva:", confirmarField);

        RoundedButton saveButton = new RoundedButton("Cambiar contrasena", AppColors.ACCENT_BLUE);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            String actual = new String(actualField.getPassword());
            String nueva = new String(nuevaField.getPassword());
            String confirmar = new String(confirmarField.getPassword());
            if (actual.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingresa tu contrasena actual.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (nueva.length() < 6) {
                JOptionPane.showMessageDialog(this, "La contrasena debe tener al menos 6 caracteres.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!nueva.equals(confirmar)) {
                JOptionPane.showMessageDialog(this, "Las contrasenas no coinciden.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!data.changePassword(actual, nueva)) {
                JOptionPane.showMessageDialog(this, "La contrasena actual no es valida.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "Contrasena actualizada correctamente.", "Exito", JOptionPane.INFORMATION_MESSAGE);
        });

        return wrapScroll(panel);
    }

    private JPanel buildExportarPanel() {
        JPanel panel = createCard();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(sectionTitle("Importar y exportar datos"));
        panel.add(Box.createVerticalStrut(12));
        panel.add(createActionRow(AppIcons.BUDGET + " Exportar a Excel (.xls)", "Genera una hoja de calculo con resumen, transacciones, metas y presupuestos", "Exportar", this::exportarExcel));
        panel.add(createActionRow(AppIcons.EXPORT + " Exportar a PDF", "Genera un reporte financiero en PDF listo para compartir", "Exportar", this::exportarPdf));
        panel.add(createActionRow(AppIcons.REPORTS + " Exportar a CSV", "Exporta las transacciones en formato separado por comas", "Exportar", this::exportarCsv));
        panel.add(createDualActionRow(AppIcons.EXPORT + " Respaldo completo", "Guarda o restaura el estado persistido de la aplicacion, incluida la imagen de perfil", "Respaldar", this::crearRespaldoCompleto, "Restaurar", this::restaurarRespaldoCompleto));
        panel.add(createDualActionRow(AppIcons.PLUS + " Importar transacciones CSV", "Carga fecha, tipo, categoria, descripcion y monto", "Plantilla", this::descargarPlantillaTransacciones, "Importar", this::importarTransaccionesCsv));
        panel.add(createDualActionRow(AppIcons.GOALS + " Importar metas CSV", "Carga nombre, icono, monto actual, monto objetivo, progreso y fecha limite", "Plantilla", this::descargarPlantillaMetas, "Importar", this::importarMetasCsv));
        panel.add(createDualActionRow(AppIcons.BUDGET + " Importar presupuestos CSV", "Carga categoria, monto presupuestado y monto gastado", "Plantilla", this::descargarPlantillaPresupuestos, "Importar", this::importarPresupuestosCsv));
        return wrapScroll(panel);
    }

    private JPanel createActionRow(String titleText, String description, String buttonText, Runnable action) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(Color.WHITE);

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("<html><div style='width:340px'>" + description + "</div></html>");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(AppColors.TEXT_MUTED);

        RoundedButton button = new RoundedButton(buttonText, AppColors.ACCENT_BLUE);
        button.setPreferredSize(new Dimension(92, 30));
        button.setFont(new Font("Segoe UI", Font.BOLD, 10));
        button.addActionListener(e -> action.run());

        text.add(title);
        text.add(subtitle);
        row.add(text, BorderLayout.CENTER);
        row.add(button, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private JPanel createDualActionRow(String titleText, String description, String leftButtonText, Runnable leftAction, String rightButtonText, Runnable rightAction) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppColors.BORDER),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(Color.WHITE);

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("<html><div style='width:320px'>" + description + "</div></html>");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(AppColors.TEXT_MUTED);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        RoundedButton leftButton = new RoundedButton(leftButtonText, AppColors.TEXT_MUTED);
        leftButton.setPreferredSize(new Dimension(92, 30));
        leftButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
        leftButton.addActionListener(e -> leftAction.run());

        RoundedButton rightButton = new RoundedButton(rightButtonText, AppColors.ACCENT_BLUE);
        rightButton.setPreferredSize(new Dimension(92, 30));
        rightButton.setFont(new Font("Segoe UI", Font.BOLD, 10));
        rightButton.addActionListener(e -> rightAction.run());

        actions.add(leftButton);
        actions.add(rightButton);
        text.add(title);
        text.add(subtitle);
        row.add(text, BorderLayout.CENTER);
        row.add(actions, BorderLayout.EAST);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private void exportarCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar CSV");
        chooser.setSelectedFile(new File("transacciones.csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                exportService.exportCsv(chooser.getSelectedFile(), data);
                JOptionPane.showMessageDialog(this, "Datos exportados a CSV con exito.", "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al exportar CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar reporte Excel");
        chooser.setSelectedFile(new File("reporte_financiero.xls"));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo Excel (*.xls)", "xls"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File exported = exportService.exportExcel(chooser.getSelectedFile(), data, "Vista general");
                JOptionPane.showMessageDialog(this, "Reporte Excel exportado en:\n" + exported.getAbsolutePath(), "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al exportar Excel: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar reporte PDF");
        chooser.setSelectedFile(new File("reporte_financiero.pdf"));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo PDF (*.pdf)", "pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File exported = exportService.exportPdf(chooser.getSelectedFile(), data, "Vista general");
                JOptionPane.showMessageDialog(this, "Reporte PDF exportado en:\n" + exported.getAbsolutePath(), "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al exportar PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importarTransaccionesCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar archivo CSV");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ExportService.TransactionPreviewResult preview = exportService.previewTransactionsCsv(chooser.getSelectedFile(), data);
                if (!confirmarImportacionTransacciones(preview)) {
                    return;
                }
                ExportService.ImportResult result = exportService.importTransactionsCsv(chooser.getSelectedFile(), data);
                showImportResult("transacciones", result);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al importar CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void descargarPlantillaTransacciones() {
        guardarPlantilla("plantilla_transacciones.csv", "Guardar plantilla de transacciones", file -> exportService.exportTransactionsTemplate(file));
    }

    private void descargarPlantillaMetas() {
        guardarPlantilla("plantilla_metas.csv", "Guardar plantilla de metas", file -> exportService.exportGoalsTemplate(file));
    }

    private void descargarPlantillaPresupuestos() {
        guardarPlantilla("plantilla_presupuestos.csv", "Guardar plantilla de presupuestos", file -> exportService.exportBudgetsTemplate(file));
    }

    private void crearRespaldoCompleto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar respaldo completo");
        chooser.setSelectedFile(new File("respaldo_finanzas.zip"));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo ZIP (*.zip)", "zip"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (data.createBackup(chooser.getSelectedFile())) {
                JOptionPane.showMessageDialog(this, "Respaldo creado correctamente.", "Exito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "No fue posible crear el respaldo: " + data.getLastErrorMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void restaurarRespaldoCompleto() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Restaurar un respaldo reemplazara el estado actual cargado en la aplicacion. Deseas continuar?",
                "Restaurar respaldo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar respaldo");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo ZIP (*.zip)", "zip"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            if (data.restoreBackup(chooser.getSelectedFile())) {
                JOptionPane.showMessageDialog(this, "Respaldo restaurado correctamente. Inicia sesion nuevamente para continuar.", "Exito", JOptionPane.INFORMATION_MESSAGE);
                buildUI();
            } else {
                JOptionPane.showMessageDialog(this, "No fue posible restaurar el respaldo: " + data.getLastErrorMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importarMetasCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar archivo CSV de metas");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ExportService.GoalPreviewResult preview = exportService.previewGoalsCsv(chooser.getSelectedFile(), data);
                if (!confirmarImportacionMetas(preview)) {
                    return;
                }
                ExportService.ImportResult result = exportService.importGoalsCsv(chooser.getSelectedFile(), data);
                showImportResult("metas", result);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al importar metas: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importarPresupuestosCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar archivo CSV de presupuestos");
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ExportService.BudgetPreviewResult preview = exportService.previewBudgetsCsv(chooser.getSelectedFile(), data);
                if (!confirmarImportacionPresupuestos(preview)) {
                    return;
                }
                ExportService.ImportResult result = exportService.importBudgetsCsv(chooser.getSelectedFile(), data);
                showImportResult("presupuestos", result);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al importar presupuestos: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showImportResult(String itemName, ExportService.ImportResult result) {
        JOptionPane.showMessageDialog(
                this,
                "Importacion completada.\n"
                        + "Registros importados de " + itemName + ": " + result.getImported() + "\n"
                        + "Registros omitidos: " + result.getSkipped(),
                "Exito",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean confirmarImportacionTransacciones(ExportService.TransactionPreviewResult preview) {
        long validas = preview.countByStatus("VALIDA");
        long duplicadas = preview.countByStatus("DUPLICADA");
        long invalidas = preview.countByStatus("INVALIDA");

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Estado", "Fecha", "Tipo", "Categoria", "Descripcion", "Monto", "Detalle"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int shown = 0;
        for (ExportService.TransactionPreviewItem item : preview.getItems()) {
            if (shown >= 12) {
                break;
            }
            String fecha = "";
            String tipo = "";
            String categoria = "";
            String descripcion = "";
            String monto = "";
            if (item.getTransaccion() != null) {
                fecha = item.getTransaccion().getFecha().toString();
                tipo = item.getTransaccion().getTipo().name();
                categoria = item.getTransaccion().getCategoria();
                descripcion = item.getTransaccion().getDescripcion();
                monto = String.format(java.util.Locale.US, "%.2f", item.getTransaccion().getMonto());
            }
            model.addRow(new Object[]{item.getStatus(), fecha, tipo, categoria, descripcion, monto, item.getDetail()});
            shown++;
        }

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel("<html><b>Vista previa de importacion</b><br>"
                + "Validas: " + validas
                + " | Duplicadas: " + duplicadas
                + " | Invalidas: " + invalidas
                + "<br>Solo se importaran las filas validas.</html>"), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(760, 320));

        int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Confirmar importacion de transacciones",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        return option == JOptionPane.OK_OPTION && validas > 0;
    }

    private boolean confirmarImportacionMetas(ExportService.GoalPreviewResult preview) {
        long validas = preview.countByStatus("VALIDA");
        long duplicadas = preview.countByStatus("DUPLICADA");
        long invalidas = preview.countByStatus("INVALIDA");

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Estado", "Meta", "Icono", "Actual", "Objetivo", "Fecha limite", "Detalle"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int shown = 0;
        for (ExportService.GoalPreviewItem item : preview.getItems()) {
            if (shown >= 12) {
                break;
            }
            String nombre = "";
            String icono = "";
            String actual = "";
            String objetivo = "";
            String fecha = "";
            if (item.getMeta() != null) {
                nombre = item.getMeta().getNombre();
                icono = item.getMeta().getIcono();
                actual = String.format(java.util.Locale.US, "%.2f", item.getMeta().getMontoActual());
                objetivo = String.format(java.util.Locale.US, "%.2f", item.getMeta().getMontoMeta());
                fecha = item.getMeta().getFechaLimite().toString();
            }
            model.addRow(new Object[]{item.getStatus(), nombre, icono, actual, objetivo, fecha, item.getDetail()});
            shown++;
        }

        return confirmarPreviewGenerico(
                "Confirmar importacion de metas",
                "Vista previa de importacion",
                "Validas: " + validas + " | Duplicadas: " + duplicadas + " | Invalidas: " + invalidas + "<br>Solo se importaran las filas validas.",
                model,
                validas);
    }

    private boolean confirmarImportacionPresupuestos(ExportService.BudgetPreviewResult preview) {
        long validas = preview.countByStatus("VALIDA");
        long duplicadas = preview.countByStatus("DUPLICADA");
        long invalidas = preview.countByStatus("INVALIDA");

        DefaultTableModel model = new DefaultTableModel(new Object[]{"Estado", "Categoria", "Presupuestado", "Gastado", "Detalle"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int shown = 0;
        for (ExportService.BudgetPreviewItem item : preview.getItems()) {
            if (shown >= 12) {
                break;
            }
            String categoria = "";
            String presupuestado = "";
            String gastado = "";
            if (item.getPresupuesto() != null) {
                categoria = item.getPresupuesto().getCategoria();
                presupuestado = String.format(java.util.Locale.US, "%.2f", item.getPresupuesto().getMontoPresupuestado());
                gastado = String.format(java.util.Locale.US, "%.2f", item.getPresupuesto().getMontoGastado());
            }
            model.addRow(new Object[]{item.getStatus(), categoria, presupuestado, gastado, item.getDetail()});
            shown++;
        }

        return confirmarPreviewGenerico(
                "Confirmar importacion de presupuestos",
                "Vista previa de importacion",
                "Validas: " + validas + " | Duplicadas: " + duplicadas + " | Invalidas: " + invalidas + "<br>Solo se importaran las filas validas.",
                model,
                validas);
    }

    private boolean confirmarPreviewGenerico(String title, String heading, String summaryHtml, DefaultTableModel model, long validCount) {
        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JLabel("<html><b>" + heading + "</b><br>" + summaryHtml + "</html>"), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(760, 320));

        int option = JOptionPane.showConfirmDialog(
                this,
                panel,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        return option == JOptionPane.OK_OPTION && validCount > 0;
    }

    private void guardarPlantilla(String fileName, String dialogTitle, FileExporter exporter) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogTitle);
        chooser.setSelectedFile(new File(fileName));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivo CSV (*.csv)", "csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File exported = exporter.export(chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Plantilla guardada en:\n" + exported.getAbsolutePath(), "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar plantilla: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private interface FileExporter {
        File export(File file) throws Exception;
    }

    private void seleccionarImagenPerfil() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Seleccionar imagen de perfil");
        chooser.setFileFilter(new FileNameExtensionFilter("Imagenes PNG/JPG", "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (data.updateProfileImage(selectedFile)) {
                buildUI();
            } else {
                JOptionPane.showMessageDialog(this, "No fue posible guardar la imagen de perfil: " + data.getLastErrorMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel buildAcercaPanel() {
        JPanel panel = createCard();
        panel.setLayout(new GridBagLayout());

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel icon = new JLabel(AppIcons.APP);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 54));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel("FinanzasApp");
        name.setFont(new Font("Segoe UI", Font.BOLD, 22));
        name.setForeground(AppColors.TEXT_PRIMARY);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel version = new JLabel("Version 1.0.0");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        version.setForeground(AppColors.TEXT_MUTED);
        version.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel desc = new JLabel("<html><div style='text-align:center;width:260px;color:#6b7280;'>Gestion inteligente de tus finanzas personales y del hogar. Desarrollado con Java Swing.</div></html>");
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(icon);
        center.add(Box.createVerticalStrut(8));
        center.add(name);
        center.add(version);
        center.add(Box.createVerticalStrut(8));
        center.add(desc);
        panel.add(center);
        return wrapScroll(panel);
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(AppColors.TEXT_PRIMARY);
        return label;
    }

    private JPanel wrapScroll(JPanel inner) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(AppColors.MAIN_BG);
        outer.setBorder(BorderFactory.createEmptyBorder(8, 20, 20, 20));

        JScrollPane scrollPane = new JScrollPane(inner);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        outer.add(scrollPane, BorderLayout.CENTER);
        return outer;
    }

    private JPanel createCard() {
        JPanel card = new CardPanel(12, false);
        card.setOpaque(false);
        return card;
    }
}
