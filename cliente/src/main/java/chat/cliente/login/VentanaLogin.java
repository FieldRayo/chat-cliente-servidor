package chat.cliente.login;

import javax.swing.*;
import java.awt.*;

/**
 * Ventana Swing de inicio de sesión.
 */
public class VentanaLogin extends JFrame {

    private final JTextField txtUsuario = new JTextField(15);
    private final JPasswordField txtPassword = new JPasswordField(15);
    private final JButton btnEntrar = new JButton("Iniciar sesión");
    private final JButton btnRegistro = new JButton("Crear cuenta");
    private final JLabel lblMensaje = new JLabel(" ", SwingConstants.CENTER);

    public VentanaLogin() {
        super("Chat - Inicio de sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(350, 250);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        form.add(new JLabel("Usuario:"), c);
        c.gridx = 1;
        form.add(txtUsuario, c);

        c.gridx = 0; c.gridy = 1;
        form.add(new JLabel("Contraseña:"), c);
        c.gridx = 1;
        form.add(txtPassword, c);

        JPanel botones = new JPanel(new FlowLayout());
        botones.add(btnEntrar);
        botones.add(btnRegistro);

        lblMensaje.setForeground(Color.RED);

        add(form, BorderLayout.CENTER);
        JPanel sur = new JPanel(new BorderLayout());
        sur.add(botones, BorderLayout.CENTER);
        sur.add(lblMensaje, BorderLayout.SOUTH);
        add(sur, BorderLayout.SOUTH);

        btnEntrar.addActionListener(e -> iniciarSesion());
        txtPassword.addActionListener(e -> iniciarSesion());
        getRootPane().setDefaultButton(btnEntrar);
    }

    private void iniciarSesion() {
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (usuario.isEmpty() || password.isEmpty()) {
            lblMensaje.setText("Llena usuario y contraseña");
            return;
        }

        // TODO: enviar usuario y contraseña al servidor y esperar respuesta.
        lblMensaje.setForeground(new Color(0, 130, 0));
        lblMensaje.setText("Enviando credenciales de: " + usuario);
    }

    public String getUsuario() {
        return txtUsuario.getText().trim();
    }

    public void mostrarError(String mensaje) {
        lblMensaje.setForeground(Color.RED);
        lblMensaje.setText(mensaje);
    }

    public JButton getBtnRegistro() {
        return btnRegistro;
    }

    // Para probar la ventana sola
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaLogin().setVisible(true));
    }
}