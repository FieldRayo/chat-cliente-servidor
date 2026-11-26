package chat.cliente.login;

import chat.cliente.app.Conexion;
import chat.cliente.app.VentanaPrincipal;
import chat.cliente.registro.VentanaRegistro;
import chat.comun.protocolo.Paquete;
import chat.comun.protocolo.Tipo;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Ventana Swing de inicio de sesión.
 * Dueño: Integrante 06 · Inicio de sesión (cliente).
 */
public class VentanaLogin extends JFrame {

    /** Debe ser igual a LoginServicio.IR_REGISTRO (el cliente no puede usar código del servidor). */
    private static final String IR_REGISTRO = "IR_REGISTRO";

    private final transient Conexion conexion;
    private final transient Consumer<Paquete> alResponder = this::recibirRespuesta;

    private final JTextField txtUsuario = new JTextField(15);
    private final JPasswordField txtPassword = new JPasswordField(15);
    private final JButton btnEntrar = new JButton("Iniciar sesión");
    private final JButton btnRegistro = new JButton("Crear cuenta");
    private final JButton btnOlvide = new JButton("¿Olvidaste tu acceso?");
    private final JLabel lblMensaje = new JLabel(" ", SwingConstants.CENTER);

    private String usuarioIntento;

    public VentanaLogin(Conexion conexion) {
        super("Chat - Inicio de sesión");
        this.conexion = conexion;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(380, 280);
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

        JPanel olvide = new JPanel(new FlowLayout());
        olvide.add(btnOlvide);

        lblMensaje.setForeground(Color.RED);

        add(form, BorderLayout.CENTER);
        JPanel sur = new JPanel(new GridLayout(0, 1));
        sur.add(botones);
        sur.add(olvide);
        sur.add(lblMensaje);
        add(sur, BorderLayout.SOUTH);

        btnEntrar.addActionListener(e -> iniciarSesion());
        txtPassword.addActionListener(e -> iniciarSesion());
        btnRegistro.addActionListener(e -> abrirRegistro());
        btnOlvide.addActionListener(e -> abrirRecuperar());
        getRootPane().setDefaultButton(btnEntrar);

        if (conexion != null) {
            conexion.alRecibir(Tipo.LOGIN, alResponder);
        }
    }

    private void iniciarSesion() {
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (usuario.isEmpty() || password.isEmpty()) {
            mostrarError("Llena usuario y contraseña");
            return;
        }
        if (conexion == null) {
            mostrarAviso("Prueba sin servidor: no se envió nada");
            return;
        }

        usuarioIntento = usuario;
        btnEntrar.setEnabled(false);
        mostrarAviso("Verificando...");
        conexion.enviar(new Paquete(Tipo.LOGIN, usuario, null, password));
    }

    /** Respuesta del servidor (ya en el hilo de Swing). */
    private void recibirRespuesta(Paquete p) {
        if (!p.esOk() && !p.esError()) return;
        btnEntrar.setEnabled(true);
        txtPassword.setText("");

        if (p.esError()) {
            mostrarError((String) p.getDatos());
        } else if (IR_REGISTRO.equals(p.getDatos())) {
            mostrarError("Ese usuario no existe. Regístrate para entrar.");
            abrirRegistro();
        } else {
            conexion.quitar(Tipo.LOGIN, alResponder);
            conexion.setUsuario(usuarioIntento);
            new VentanaPrincipal(conexion).setVisible(true);
            dispose();
        }
    }

    private void abrirRegistro() {
        VentanaRegistro registro = new VentanaRegistro(conexion);
        registro.setVisible(true);
    }

    /** La ventana de recuperar acceso la hace 07 · Cuenta; se abre por reflexión para no depender de ella. */
    private void abrirRecuperar() {
        try {
            JFrame ventana = (JFrame) Class.forName("chat.cliente.cuenta.VentanaRecuperar")
                    .getConstructor(Conexion.class).newInstance(conexion);
            ventana.setVisible(true);
        } catch (ReflectiveOperationException | ClassCastException e) {
            mostrarError("La recuperación de acceso todavía no está lista.");
        }
    }

    public String getUsuario() {
        return txtUsuario.getText().trim();
    }

    public void mostrarError(String mensaje) {
        lblMensaje.setForeground(Color.RED);
        lblMensaje.setText(mensaje);
    }

    private void mostrarAviso(String mensaje) {
        lblMensaje.setForeground(new Color(0, 130, 0));
        lblMensaje.setText(mensaje);
    }

    // Para probar la ventana sola (sin servidor)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaLogin(null).setVisible(true));
    }
}
