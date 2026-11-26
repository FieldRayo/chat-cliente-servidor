package chat.cliente.cuenta;

import chat.cliente.app.Conexion;
import chat.comun.protocolo.Paquete;
import chat.comun.protocolo.Tipo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** Solicita la pregunta registrada y permite cambiar la contraseña. */
public final class VentanaRecuperar extends JFrame {
    private final Conexion conexion;
    private final Consumer<Paquete> alConsultar = this::recibirPregunta;
    private final Consumer<Paquete> alRestablecer = this::recibirResultado;
    private final CardLayout pasos = new CardLayout();
    private final JPanel contenido = new JPanel(pasos);
    private final JTextField usuario = new JTextField(24);
    private final JLabel pregunta = new JLabel();
    private final JPasswordField respuesta = new JPasswordField(24);
    private final JPasswordField contrasena = new JPasswordField(24);
    private final JPasswordField confirmar = new JPasswordField(24);
    private final JButton continuar = new JButton("Consultar pregunta");
    private final JButton guardar = new JButton("Restablecer contraseña");
    private final JButton volver = new JButton("Cambiar usuario");
    private String usuarioConsultado;

    public VentanaRecuperar(Conexion conexion) {
        super("Recuperar acceso");
        this.conexion = conexion;

        JPanel consulta = new JPanel(new GridLayout(0, 1, 0, 8));
        agregarCampo(consulta, "Nombre de usuario", usuario);
        consulta.add(continuar);

        JPanel restablecer = new JPanel(new GridLayout(0, 1, 0, 8));
        restablecer.add(pregunta);
        agregarCampo(restablecer, "Respuesta de seguridad", respuesta);
        agregarCampo(restablecer, "Nueva contraseña (mínimo 6 caracteres)", contrasena);
        agregarCampo(restablecer, "Confirmar contraseña", confirmar);
        restablecer.add(guardar);
        restablecer.add(volver);

        contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contenido.add(consulta, "usuario");
        contenido.add(restablecer, "pregunta");
        add(contenido);
        continuar.addActionListener(e -> consultarPregunta());
        guardar.addActionListener(e -> restablecer());
        volver.addActionListener(e -> mostrarConsulta());
        getRootPane().setDefaultButton(continuar);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        conexion.alRecibir(Tipo.RECUPERAR_PREGUNTA, alConsultar);
        conexion.alRecibir(Tipo.RECUPERAR, alRestablecer);
    }

    private void consultarPregunta() {
        String nombre = usuario.getText().trim();
        if (nombre.isEmpty()) {
            error("Escribe tu nombre de usuario.");
            return;
        }
        usuarioConsultado = nombre;
        esperando(true);
        conexion.enviar(new Paquete(Tipo.RECUPERAR_PREGUNTA, nombre, null, null));
    }

    private void recibirPregunta(Paquete paquete) {
        if (!isDisplayable() || !usuarioConsultadoCoincide(paquete)) return;
        esperando(false);
        if (paquete.esOk() && paquete.getDatos() instanceof String texto) {
            pregunta.setText(texto);
            pasos.show(contenido, "pregunta");
            getRootPane().setDefaultButton(guardar);
            pack();
            respuesta.requestFocusInWindow();
        } else {
            error(mensaje(paquete));
        }
    }

    private void restablecer() {
        char[] respuestaEscrita = respuesta.getPassword();
        char[] clave = contrasena.getPassword();
        char[] confirmacion = confirmar.getPassword();
        try {
            String textoRespuesta = new String(respuestaEscrita);
            if (textoRespuesta.isBlank() || clave.length < 6) {
                error("Escribe la respuesta y una contraseña de al menos 6 caracteres.");
            } else if (!Arrays.equals(clave, confirmacion)) {
                error("Las contraseñas no coinciden.");
            } else {
                esperando(true);
                var datos = new ArrayList<>(List.of(textoRespuesta, new String(clave)));
                limpiarCampos();
                conexion.enviar(new Paquete(Tipo.RECUPERAR, usuarioConsultado, null, datos));
            }
        } finally {
            Arrays.fill(respuestaEscrita, '\0');
            Arrays.fill(clave, '\0');
            Arrays.fill(confirmacion, '\0');
        }
    }

    private void recibirResultado(Paquete paquete) {
        if (!isDisplayable() || !usuarioConsultadoCoincide(paquete)) return;
        esperando(false);
        if (paquete.esOk()) {
            JOptionPane.showMessageDialog(this, mensaje(paquete));
            dispose();
        } else {
            error(mensaje(paquete));
            respuesta.requestFocusInWindow();
        }
    }

    private void mostrarConsulta() {
        limpiarCampos();
        usuarioConsultado = null;
        pasos.show(contenido, "usuario");
        getRootPane().setDefaultButton(continuar);
        usuario.requestFocusInWindow();
    }

    private void limpiarCampos() {
        respuesta.setText("");
        contrasena.setText("");
        confirmar.setText("");
    }

    private void esperando(boolean esperando) {
        continuar.setEnabled(!esperando);
        guardar.setEnabled(!esperando);
        volver.setEnabled(!esperando);
        usuario.setEnabled(!esperando);
        setDefaultCloseOperation(esperando ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
    }

    private boolean usuarioConsultadoCoincide(Paquete paquete) {
        return usuarioConsultado != null && usuarioConsultado.equals(paquete.getPara());
    }

    private static void agregarCampo(JPanel panel, String texto, JTextField campo) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setLabelFor(campo);
        panel.add(etiqueta);
        panel.add(campo);
    }

    private static String mensaje(Paquete paquete) {
        return paquete.getDatos() instanceof String texto ? texto : "El servidor envió una respuesta inesperada.";
    }

    private void error(String texto) {
        JOptionPane.showMessageDialog(this, texto, "Recuperar acceso", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void dispose() {
        conexion.quitar(Tipo.RECUPERAR_PREGUNTA, alConsultar);
        conexion.quitar(Tipo.RECUPERAR, alRestablecer);
        limpiarCampos();
        super.dispose();
    }
}
