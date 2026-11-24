package chat.cliente.registro;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Formulario de registro de un usuario nuevo.
 * <p>
 * Dueño: Integrante 05 · Registro (cliente).
 * <p>
 * Llena las columnas nombre_usuario, email y psw de la tabla usuarios.
 * Las columnas intentos_fallidos y bloqueado las maneja 06 · Login.
 * <p>
 * Se puede abrir desde el login (06 · Login) o sola, cuando el login manda al
 * usuario a registrarse después de 3 intentos con un usuario que no existe.
 * <p>
 * Por ahora no se conecta al servidor: cuando exista {@code Conexion} (01 ·
 * Arquitectura) se completa el método {@link #enviarRegistro}.
 */
public class VentanaRegistro extends JFrame {

    /** Mínimo de caracteres de la contraseña. */
    public static final int MIN_CONTRASENA = 6;

    /** Máximo de caracteres de nombre_usuario y email (varchar(50) en la tabla). */
    public static final int MAX_TEXTO = 50;

    private static final int ANCHO_CAMPOS = 320;

    private final JTextField txtUsuario = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JPasswordField txtContrasena = new JPasswordField();
    private final JPasswordField txtConfirmar = new JPasswordField();
    private final JButton btnRegistrar = new JButton("Registrarme");
    private final JButton btnCancelar = new JButton("Cancelar");

    public VentanaRegistro() {
        super("Registro de usuario");
        construirInterfaz();
        configurarEventos();

        // Cerrar esta ventana no cierra el programa (el login puede seguir abierto)
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }

    private void construirInterfaz() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(crearEncabezado(), BorderLayout.NORTH);
        getContentPane().add(crearFormulario(), BorderLayout.CENTER);
        getContentPane().add(crearBotones(), BorderLayout.SOUTH);

        // Enter en cualquier campo equivale a dar clic en "Registrarme"
        getRootPane().setDefaultButton(btnRegistrar);
    }

    private JPanel crearEncabezado() {
        JPanel encabezado = new JPanel();
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
        encabezado.setBorder(BorderFactory.createEmptyBorder(18, 25, 0, 25));

        JLabel titulo = new JLabel("Crear cuenta");
        JLabel subtitulo = new JLabel("Llena tus datos para registrarte en el chat");

        encabezado.add(titulo);
        encabezado.add(Box.createVerticalStrut(4));
        encabezado.add(subtitulo);
        return encabezado;
    }

    private JPanel crearFormulario() {
        JPanel formulario = new JPanel();
        formulario.setLayout(new BoxLayout(formulario, BoxLayout.Y_AXIS));
        formulario.setBorder(BorderFactory.createEmptyBorder(18, 25, 5, 25));

        agregarCampo(formulario, "Nombre de usuario", txtUsuario, null);
        agregarCampo(formulario, "Correo electrónico", txtEmail, null);
        agregarCampo(formulario, "Contraseña", txtContrasena,
                "Mínimo " + MIN_CONTRASENA + " caracteres");
        agregarCampo(formulario, "Confirmar contraseña", txtConfirmar, null);
        return formulario;
    }

    /** Agrega una etiqueta, debajo su campo y, si se indica, una nota de ayuda. */
    private void agregarCampo(JPanel panel, String texto, JComponent campo, String ayuda) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);

        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension tamano = new Dimension(ANCHO_CAMPOS, 30);
        campo.setPreferredSize(tamano);
        campo.setMaximumSize(tamano);

        panel.add(etiqueta);
        panel.add(Box.createVerticalStrut(4));
        panel.add(campo);

        if (ayuda != null) {
            JLabel nota = new JLabel(ayuda);
            nota.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(Box.createVerticalStrut(3));
            panel.add(nota);
        }
        panel.add(Box.createVerticalStrut(12));
    }

    private JPanel crearBotones() {
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botones.setBorder(BorderFactory.createEmptyBorder(5, 25, 20, 15));

        Dimension tamano = new Dimension(120, 30);
        btnCancelar.setPreferredSize(tamano);
        btnRegistrar.setPreferredSize(tamano);

        botones.add(btnCancelar);
        botones.add(btnRegistrar);
        return botones;
    }

    private void configurarEventos() {
        btnRegistrar.addActionListener(e -> registrar());
        btnCancelar.addActionListener(e -> dispose());
    }

    /** Se ejecuta al dar clic en "Registrarme". */
    private void registrar() {
        String usuario = txtUsuario.getText().trim();
        String email = txtEmail.getText().trim();
        String contrasena = new String(txtContrasena.getPassword());
        String confirmar = new String(txtConfirmar.getPassword());

        String error = validar(usuario, email, contrasena, confirmar);
        if (error != null) {
            mostrarError(error);
            return;
        }

        enviarRegistro(usuario, email, contrasena);
    }

    /**
     * Validaciones del lado del cliente. El servidor las vuelve a hacer.
     *
     * @return el mensaje de error, o {@code null} si todo está bien
     */
    public static String validar(String usuario, String email,
            String contrasena, String confirmar) {
        if (usuario.isEmpty() || email.isEmpty()
                || contrasena.isEmpty() || confirmar.isEmpty()) {
            return "Llena todos los campos.";
        }
        if (usuario.length() > MAX_TEXTO) {
            return "El nombre de usuario puede tener máximo " + MAX_TEXTO + " caracteres.";
        }
        if (email.length() > MAX_TEXTO) {
            return "El correo puede tener máximo " + MAX_TEXTO + " caracteres.";
        }
        // Formato básico: algo@algo.algo, sin espacios
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            return "Escribe un correo válido, por ejemplo nombre@correo.com.";
        }
        if (contrasena.length() < MIN_CONTRASENA) {
            return "La contraseña debe tener al menos " + MIN_CONTRASENA + " caracteres.";
        }
        if (!contrasena.equals(confirmar)) {
            return "Las contraseñas no coinciden.";
        }
        return null;
    }

    /**
     * Envía los datos al servidor.
     * <p>
     * PENDIENTE: cuando existan {@code Paquete}, {@code Tipo} y {@code Conexion}
     * (01 · Arquitectura), aquí se envía un Paquete de tipo REGISTRO y se
     * escucha la respuesta OK o ERROR para llamar a {@link #mostrarExito()} o
     * {@link #mostrarError(String)}.
     */
    private void enviarRegistro(String usuario, String email, String contrasena) {
        // Mientras no hay conexión, solo se confirma que las validaciones pasaron.
        JOptionPane.showMessageDialog(this,
                "Los datos son válidos.\n(Todavía no hay conexión con el servidor.)",
                "Registro", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Se llama cuando el servidor responde OK. */
    public void mostrarExito() {
        JOptionPane.showMessageDialog(this,
                "Tu cuenta se creó correctamente. Ya puedes iniciar sesión.",
                "Registro", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    /** Se llama cuando falla una validación o el servidor responde ERROR. */
    public void mostrarError(String motivo) {
        JOptionPane.showMessageDialog(this, motivo,
                "Registro", JOptionPane.ERROR_MESSAGE);
    }

    /** Para probar la ventana sola, sin el resto del programa. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaRegistro ventana = new VentanaRegistro();
            ventana.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            ventana.setVisible(true);
        });
    }
}
