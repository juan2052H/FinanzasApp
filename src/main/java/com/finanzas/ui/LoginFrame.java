package com.finanzas.ui;

import com.finanzas.api.BackendConfig;
import com.finanzas.data.DataManager;
import com.finanzas.ui.components.AppColors;
import com.finanzas.ui.components.RoundedButton;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

public class LoginFrame extends JFrame {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final DataManager data = DataManager.getInstance();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    public LoginFrame() {
        setTitle("FinanzasApp - Acceso");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 640);
        setLocationRelativeTo(null);
        setResizable(false);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(LoginFrame.class.getName()).fine("No fue posible aplicar look and feel del sistema: " + ex.getMessage());
        }
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(AppColors.MAIN_BG);
        main.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        main.add(buildBrandHeader(), BorderLayout.NORTH);

        cards.setOpaque(false);
        cards.add(buildLoginCard(), "login");
        cards.add(buildRegisterCard(), "register");
        main.add(cards, BorderLayout.CENTER);

        add(main);
        cardLayout.show(cards, "login");
    }

    private JPanel buildBrandHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JLabel title = new JLabel("FinanzasApp");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(AppColors.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Gestion de finanzas personales y del hogar");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(AppColors.TEXT_SECONDARY);

        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);
        return header;
    }

    private JPanel buildLoginCard() {
        JPanel card = formCard();
        GridBagConstraints gbc = baseConstraints();

        JLabel heading = new JLabel("Iniciar sesion");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(AppColors.TEXT_PRIMARY);

        JLabel helper = new JLabel(BackendConfig.isEnabled()
                ? "Conectado al backend: " + BackendConfig.baseUrl()
                : "Ingresa con tu correo y contrasena.");
        helper.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        helper.setForeground(AppColors.TEXT_SECONDARY);

        JTextField emailField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        RoundedButton loginButton = new RoundedButton("INICIAR SESION", AppColors.ACCENT_BLUE);
        RoundedButton socialButton = new RoundedButton("Continuar con Google", Color.WHITE);
        socialButton.setForeground(AppColors.TEXT_PRIMARY);
        socialButton.setBorder(BorderFactory.createLineBorder(AppColors.BORDER));
        String googleClientId = googleClientId();
        if (googleClientId.isEmpty()) {
            socialButton.setText("Google Sign-In no configurado");
            socialButton.setToolTipText("Configura GOOGLE_OAUTH_CLIENT_ID para habilitar Google Sign-In.");
            socialButton.setEnabled(false);
        } else if (!BackendConfig.isEnabled()) {
            socialButton.setText("Google Sign-In requiere backend");
            socialButton.setToolTipText("Activa FINANZAS_API_ENABLED=true para usar Google Sign-In.");
            socialButton.setEnabled(false);
        } else {
            socialButton.setText("Continuar con Google");
            socialButton.setToolTipText("Abrira el navegador para completar Google Sign-In.");
            socialButton.setEnabled(true);
        }

        JButton registerLink = linkButton("No tienes cuenta? Registrate");
        JButton resetPasswordLink = linkButton("Olvidaste tu contrasena?");
        JButton verifyEmailLink = linkButton("Verificar correo");
        resetPasswordLink.setEnabled(BackendConfig.isEnabled());
        verifyEmailLink.setEnabled(BackendConfig.isEnabled());

        loginButton.addActionListener(e -> {
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (data.login(email, password)) {
                openMainFrame();
                return;
            }
            String message = data.getLastErrorMessage() == null || data.getLastErrorMessage().trim().isEmpty()
                    ? "Correo o contrasena invalidos."
                    : data.getLastErrorMessage();
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        });

        socialButton.addActionListener(e -> {
            socialButton.setEnabled(false);
            socialButton.setText("Esperando Google...");
            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return data.loginWithGoogle(googleClientId);
                }

                @Override
                protected void done() {
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            openMainFrame();
                            return;
                        }
                        showError(data.getLastErrorMessage());
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        showError(cause.getMessage() == null || cause.getMessage().trim().isEmpty()
                                ? "No fue posible iniciar sesion con Google."
                                : cause.getMessage());
                    } finally {
                        socialButton.setText("Continuar con Google");
                        socialButton.setEnabled(true);
                    }
                }
            }.execute();
        });

        registerLink.addActionListener(e -> cardLayout.show(cards, "register"));
        resetPasswordLink.addActionListener(e -> showPasswordResetRequestDialog());
        verifyEmailLink.addActionListener(e -> showEmailVerificationDialog());

        addField(card, gbc, 0, heading);
        addField(card, gbc, 1, helper);
        addField(card, gbc, 2, fieldLabel("Correo electronico"));
        addField(card, gbc, 3, emailField);
        addField(card, gbc, 4, fieldLabel("Contrasena"));
        addField(card, gbc, 5, passwordField);
        addField(card, gbc, 6, loginButton);
        addField(card, gbc, 7, separatorLabel("O"));
        addField(card, gbc, 8, socialButton);
        addField(card, gbc, 9, registerLink);
        addField(card, gbc, 10, resetPasswordLink);
        addField(card, gbc, 11, verifyEmailLink);
        return card;
    }

    private JPanel buildRegisterCard() {
        JPanel card = formCard();
        GridBagConstraints gbc = baseConstraints();

        JLabel heading = new JLabel("Unete a FinanzasApp");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(AppColors.TEXT_PRIMARY);

        JLabel helper = new JLabel(BackendConfig.isEnabled()
                ? "La cuenta se creara en el backend configurado."
                : "Crea una cuenta personal o de hogar.");
        helper.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        helper.setForeground(AppColors.TEXT_SECONDARY);

        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmField = new JPasswordField();
        JComboBox<String> currencyBox = new JComboBox<>(new String[]{"COP - Peso colombiano", "USD - Dolar", "EUR - Euro"});

        JRadioButton personalButton = new JRadioButton("Personal", true);
        JRadioButton householdButton = new JRadioButton("Hogar");
        ButtonGroup group = new ButtonGroup();
        group.add(personalButton);
        group.add(householdButton);

        JPanel accountTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        accountTypePanel.setOpaque(false);
        personalButton.setOpaque(false);
        householdButton.setOpaque(false);
        accountTypePanel.add(personalButton);
        accountTypePanel.add(householdButton);

        JCheckBox termsBox = new JCheckBox("Acepto terminos y condiciones");
        termsBox.setOpaque(false);

        RoundedButton registerButton = new RoundedButton("CREAR CUENTA", AppColors.ACCENT_BLUE);
        JButton loginLink = linkButton("Ya tienes cuenta? Inicia sesion");

        registerButton.addActionListener(e -> {
            String nombre = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirm = new String(confirmField.getPassword());
            String tipoCuenta = personalButton.isSelected() ? "Personal" : "Hogar";
            String moneda = ((String) currencyBox.getSelectedItem()).substring(0, 3);

            if (nombre.isEmpty()) {
                showError("Ingresa tu nombre completo.");
                return;
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showError("Ingresa un correo electronico valido.");
                return;
            }
            int minimumPasswordLength = BackendConfig.isEnabled() ? 8 : 6;
            if (password.length() < minimumPasswordLength) {
                showError("La contrasena debe tener al menos " + minimumPasswordLength + " caracteres.");
                return;
            }
            if (!password.equals(confirm)) {
                showError("La confirmacion de contrasena no coincide.");
                return;
            }
            if (!termsBox.isSelected()) {
                showError("Debes aceptar los terminos y condiciones.");
                return;
            }
            if (!data.register(nombre, email, password, moneda, tipoCuenta)) {
                String message = data.getLastErrorMessage() == null || data.getLastErrorMessage().trim().isEmpty()
                        ? "No fue posible crear la cuenta."
                        : data.getLastErrorMessage();
                showError(message);
                return;
            }

            JOptionPane.showMessageDialog(this,
                    BackendConfig.isEnabled()
                            ? "Cuenta creada correctamente. Revisa el email de verificacion y luego inicia sesion."
                            : "Cuenta creada correctamente. Ahora puedes iniciar sesion.",
                    "Registro exitoso",
                    JOptionPane.INFORMATION_MESSAGE);
            nameField.setText("");
            emailField.setText("");
            passwordField.setText("");
            confirmField.setText("");
            termsBox.setSelected(false);
            cardLayout.show(cards, "login");
        });

        loginLink.addActionListener(e -> cardLayout.show(cards, "login"));

        addField(card, gbc, 0, heading);
        addField(card, gbc, 1, helper);
        addField(card, gbc, 2, fieldLabel("Nombre completo"));
        addField(card, gbc, 3, nameField);
        addField(card, gbc, 4, fieldLabel("Correo electronico"));
        addField(card, gbc, 5, emailField);
        addField(card, gbc, 6, fieldLabel("Contrasena"));
        addField(card, gbc, 7, passwordField);
        addField(card, gbc, 8, fieldLabel("Confirmar contrasena"));
        addField(card, gbc, 9, confirmField);
        addField(card, gbc, 10, fieldLabel("Moneda preferida"));
        addField(card, gbc, 11, currencyBox);
        addField(card, gbc, 12, fieldLabel("Tipo de cuenta"));
        addField(card, gbc, 13, accountTypePanel);
        addField(card, gbc, 14, termsBox);
        addField(card, gbc, 15, registerButton);
        addField(card, gbc, 16, loginLink);
        return card;
    }

    private JPanel formCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppColors.BORDER),
                BorderFactory.createEmptyBorder(22, 22, 22, 22)));
        return card;
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 10, 0);
        return gbc;
    }

    private void addField(JPanel panel, GridBagConstraints gbc, int row, Component component) {
        gbc.gridy = row;
        panel.add(component, gbc);
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(AppColors.TEXT_PRIMARY);
        return label;
    }

    private JComponent separatorLabel(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(AppColors.TEXT_SECONDARY);
        panel.add(new JSeparator(), BorderLayout.NORTH);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setForeground(AppColors.ACCENT_BLUE);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        return button;
    }

    private void showPasswordResetRequestDialog() {
        if (!BackendConfig.isEnabled()) {
            showError("Activa FINANZAS_API_ENABLED=true para recuperar contrasena.");
            return;
        }
        JTextField emailField = new JTextField();
        emailField.setColumns(18);
        JPanel form = new JPanel(new GridLayout(1, 2, 8, 8));
        form.add(new JLabel("Correo:"));
        form.add(emailField);
        int result = JOptionPane.showConfirmDialog(this, form, "Recuperar contrasena", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String email = emailField.getText().trim();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Ingresa un correo electronico valido.");
            return;
        }
        runAccountAction(
                () -> data.requestBackendPasswordReset(email),
                "Si el correo existe, se enviaron instrucciones de recuperacion.",
                this::showPasswordResetConfirmDialog);
    }

    private void showPasswordResetConfirmDialog() {
        JPasswordField passwordField = new JPasswordField();
        passwordField.setColumns(18);
        JPasswordField confirmField = new JPasswordField();
        confirmField.setColumns(18);
        JTextField tokenField = new JTextField();
        tokenField.setColumns(18);
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Token:"));
        form.add(tokenField);
        form.add(new JLabel("Nueva contrasena:"));
        form.add(passwordField);
        form.add(new JLabel("Confirmar:"));
        form.add(confirmField);
        int result = JOptionPane.showConfirmDialog(this, form, "Confirmar recuperacion", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String token = tokenField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());
        if (token.isEmpty()) {
            showError("Ingresa el token recibido.");
            return;
        }
        if (password.length() < 8) {
            showError("La contrasena debe tener al menos 8 caracteres.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("La confirmacion de contrasena no coincide.");
            return;
        }
        runAccountAction(
                () -> data.confirmBackendPasswordReset(token, password),
                "Contrasena actualizada. Inicia sesion nuevamente.",
                null);
    }

    private void showEmailVerificationDialog() {
        if (!BackendConfig.isEnabled()) {
            showError("Activa FINANZAS_API_ENABLED=true para verificar correo.");
            return;
        }
        JTextField emailField = new JTextField();
        emailField.setColumns(18);
        JTextField tokenField = new JTextField();
        tokenField.setColumns(18);
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Correo:"));
        form.add(emailField);
        form.add(new JLabel("Token:"));
        form.add(tokenField);
        int result = JOptionPane.showConfirmDialog(this, form, "Verificar correo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String token = tokenField.getText().trim();
        if (token.isEmpty()) {
            String email = emailField.getText().trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                showError("Ingresa un correo electronico valido.");
                return;
            }
            runAccountAction(
                    () -> data.requestBackendEmailVerification(email),
                    "Si el correo existe y esta pendiente, se envio una nueva verificacion.",
                    null);
            return;
        }
        runAccountAction(
                () -> data.confirmBackendEmailVerification(token),
                "Correo verificado correctamente.",
                null);
    }

    private void runAccountAction(AccountAction action, String successMessage, Runnable afterSuccess) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return action.run();
            }

            @Override
            protected void done() {
                setCursor(Cursor.getDefaultCursor());
                boolean ok = false;
                try {
                    ok = get();
                } catch (Exception ex) {
                    ok = false;
                }
                if (!ok) {
                    String message = data.getLastErrorMessage();
                    showError(message == null || message.trim().isEmpty() ? "No fue posible completar la accion." : message);
                    return;
                }
                JOptionPane.showMessageDialog(LoginFrame.this, successMessage, "FinanzasApp", JOptionPane.INFORMATION_MESSAGE);
                if (afterSuccess != null) {
                    afterSuccess.run();
                }
            }
        }.execute();
    }

    private interface AccountAction {
        boolean run();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void openMainFrame() {
        AppColors.applyTheme(data.getUsuario().getTheme());
        new MainFrame();
        dispose();
    }

    private String googleClientId() {
        String clientId = System.getenv("GOOGLE_OAUTH_CLIENT_ID");
        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = System.getProperty("google.oauth.clientId");
        }
        return clientId == null ? "" : clientId.trim();
    }
}
