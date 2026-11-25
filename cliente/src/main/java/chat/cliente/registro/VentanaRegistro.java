package chat.cliente.registro;

import chat.cliente.app.Conexion;
import chat.comun.protocolo.Paquete;
import chat.comun.protocolo.Tipo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Formulario de registro de un usuario nuevo.
 * <p>
 * Dueño: Integrante 05 · Registro — Mery Natalia Dávila González (cliente).
 * <p>
 * Llena la tabla usuarios (nombre_usuario, email, psw) y la tabla
 * respuestas_seguridad (id_pregunta, respuesta_hash). Las columnas
 * intentos_fallidos y bloqueado las maneja 06 · Login.
 * <p>
 * Se abre desde el login (06 · Login) con {@code new VentanaRegistro(conexion)},
 * o sola cuando el login manda al usuario a registrarse después de 3 intentos
 * con un usuario que no existe.
 * <p>
 * Envía un Paquete de tipo REGISTRO cuyos datos son un HashMap con las claves
 * de abajo (CAMPO_...). El servidor (RegistroServicio) contesta OK o ERROR con el motivo.
 */
public class VentanaRegistro extends JFrame {

    /** Mínimo de caracteres de la contraseña. */
    public static final int MIN_CONTRASENA = 6;

    /** Máximo de caracteres de nombre_usuario y email (varchar(50) en la tabla). */
    public static final int MAX_TEXTO = 50;

    /**
     * Preguntas de seguridad. Su id en la tabla preguntas_seguridad es su
     * posición + 1 (la primera es id 1, la segunda id 2...). Se tienen que
     * insertar en la BD en este mismo orden.
     */
    public static final String[] PREGUNTAS = {
        "¿Cuál es el nombre de tu primera mascota?",
        "¿En qué ciudad naciste?",
        "¿Cuál es el nombre de tu mejor amigo de la infancia?",
        "¿Cuál es tu comida favorita?",
        "¿Cómo se llamaba tu escuela primaria?"
    };

    // Claves del HashMap que viaja en el Paquete. RegistroServicio usa las mismas.
    public static final String CAMPO_USUARIO = "usuario";
    public static final String CAMPO_EMAIL = "email";
    public static final String CAMPO_CONTRASENA = "contrasena";
    public static final String CAMPO_ID_PREGUNTA = "idPregunta";
    public static final String CAMPO_RESPUESTA = "respuesta";

    private static final int ANCHO_CAMPOS = 320;

    /** Conexión con el servidor. Es null solo al probar la ventana sola (main). */
    private final transient Conexion conexion;
    private final transient Consumer<Paquete> alResponder = this::recibirRespuesta;

    private final JTextField txtUsuario = new JTextField();
    private final JTextField txtEmail = new JTextField();
    private final JPasswordField txtContrasena = new JPasswordField();
    private final JPasswordField txtConfirmar = new JPasswordField();
    private final JComboBox<String> cmbPregunta = new JComboBox<>(PREGUNTAS);
    private final JTextField txtRespuesta = new JTextField();
    private final JButton btnRegistrar = new JButton("Registrarme");
    private final JButton btnCancelar = new JButton("Cancelar");
    /** Línea de mensajes abajo de los botones, igual que en VentanaLogin. */
    private final JLabel lblMensaje = new JLabel(" ", SwingConstants.CENTER);

    public VentanaRegistro(Conexion conexion) {
        super("Chat - Registro");
        this.conexion = conexion;
        construirInterfaz();
        configurarEventos();

        if (conexion != null) {
            // Recibe la respuesta OK o ERROR que el servidor manda a un REGISTRO
            conexion.alRecibir(Tipo.REGISTRO, alResponder);
        }

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
        agregarCampo(formulario, "Pregunta de seguridad", cmbPregunta, null);
        agregarCampo(formulario, "Respuesta", txtRespuesta,
                "Te la pediremos si olvidas tu contraseña");
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

    /** Botones centrados y, abajo, la línea de mensajes (mismo diseño que VentanaLogin). */
    private JPanel crearBotones() {
        JPanel botones = new JPanel(new FlowLayout());
        botones.add(btnRegistrar);
        botones.add(btnCancelar);

        lblMensaje.setForeground(Color.RED);

        JPanel sur = new JPanel(new BorderLayout());
        sur.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        sur.add(botones, BorderLayout.CENTER);
        sur.add(lblMensaje, BorderLayout.SOUTH);
        return sur;
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
        int idPregunta = cmbPregunta.getSelectedIndex() + 1;
        String respuesta = txtRespuesta.getText().trim();

        String error = validar(usuario, email, contrasena, confirmar, respuesta);
        if (error != null) {
            mostrarError(error);
            return;
        }

        enviarRegistro(usuario, email, contrasena, idPregunta, respuesta);
    }

    /**
     * Validaciones del lado del cliente. El servidor las vuelve a hacer.
     *
     * @return el mensaje de error, o {@code null} si todo está bien
     */
    public static String validar(String usuario, String email,
            String contrasena, String confirmar, String respuesta) {
        if (usuario.isEmpty() || email.isEmpty() || contrasena.isEmpty()
                || confirmar.isEmpty() || respuesta.isEmpty()) {
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

    /** Manda los datos al servidor en un Paquete de tipo REGISTRO. */
    private void enviarRegistro(String usuario, String email, String contrasena,
            int idPregunta, String respuesta) {
        if (conexion == null) {
            // Solo pasa al probar la ventana sola, sin servidor
            mostrarAviso("Datos válidos (prueba sin servidor: no se envió nada)");
            return;
        }

        HashMap<String, String> datos = new HashMap<>();
        datos.put(CAMPO_USUARIO, usuario);
        datos.put(CAMPO_EMAIL, email);
        datos.put(CAMPO_CONTRASENA, contrasena);
        datos.put(CAMPO_ID_PREGUNTA, String.valueOf(idPregunta));
        datos.put(CAMPO_RESPUESTA, respuesta);

        // Mientras llega la respuesta, se evita mandar el registro dos veces
        btnRegistrar.setEnabled(false);
        mostrarAviso("Enviando registro...");
        conexion.enviar(new Paquete(Tipo.REGISTRO, usuario, null, datos));
    }

    /** Llega la respuesta del servidor (ya en el hilo de Swing). */
    private void recibirRespuesta(Paquete p) {
        if (!p.esOk() && !p.esError()) {
            return; // solo interesan las respuestas OK o ERROR
        }
        btnRegistrar.setEnabled(true);
        if (p.esOk()) {
            mostrarExito();
        } else {
            mostrarError((String) p.getDatos());
        }
    }

    /**
     * Se llama cuando el servidor responde OK. Deja el aviso en verde y
     * desactiva "Registrarme"; el usuario cierra con "Cerrar" y vuelve al login.
     */
    public void mostrarExito() {
        mostrarAviso("Cuenta creada. Ya puedes iniciar sesión.");
        btnRegistrar.setEnabled(false);
        btnCancelar.setText("Cerrar");
    }

    /** Mensaje de error en rojo (validación o ERROR del servidor). */
    public void mostrarError(String motivo) {
        lblMensaje.setForeground(Color.RED);
        lblMensaje.setText(motivo);
    }

    /** Mensaje informativo en verde, como "Enviando..." en VentanaLogin. */
    private void mostrarAviso(String mensaje) {
        lblMensaje.setForeground(new Color(0, 130, 0));
        lblMensaje.setText(mensaje);
    }

    /** Al cerrar la ventana deja de escuchar, para no recibir dos veces si se vuelve a abrir. */
    @Override
    public void dispose() {
        if (conexion != null) {
            conexion.quitar(Tipo.REGISTRO, alResponder);
        }
        super.dispose();
    }

    /** Para probar la ventana sola, sin servidor (no envía nada). */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaRegistro ventana = new VentanaRegistro(null);
            ventana.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            ventana.setVisible(true);
        });
    }
}
