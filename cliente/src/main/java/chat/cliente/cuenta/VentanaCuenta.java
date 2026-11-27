package chat.cliente.cuenta;

import chat.cliente.app.Conexion;
import chat.cliente.app.VentanaPrincipal;
import chat.comun.protocolo.Paquete;
import chat.comun.protocolo.Tipo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.function.Consumer;

/** Confirma la eliminación de la cuenta de la sesión actual. */
public final class VentanaCuenta extends JFrame {
    private final Conexion conexion;
    private final Consumer<Paquete> alEliminar = this::recibirResultado;
    private final JPasswordField contrasena = new JPasswordField(24);
    private final JButton eliminar = new JButton("Eliminar mi cuenta");

    public VentanaCuenta(Conexion conexion) {
        super("Eliminar cuenta");
        this.conexion = conexion;
        JPanel contenido = new JPanel(new GridLayout(0, 1, 0, 8));
        contenido.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        contenido.add(new JLabel("Eliminar tu cuenta es permanente."));
        JLabel etiqueta = new JLabel("Contraseña actual");
        etiqueta.setLabelFor(contrasena);
        contenido.add(etiqueta);
        contenido.add(contrasena);
        contenido.add(eliminar);
        add(contenido);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        eliminar.addActionListener(e -> eliminarCuenta());
        conexion.alRecibir(Tipo.ELIMINAR_CUENTA, alEliminar);
        pack();
        setLocationRelativeTo(null);
    }

    private void eliminarCuenta() {
        String usuario = conexion.getUsuario();
        char[] clave = contrasena.getPassword();
        contrasena.setText("");
        try {
            if (usuario == null || clave.length == 0) {
                JOptionPane.showMessageDialog(this, "Inicia sesión y escribe tu contraseña actual.",
                        "Eliminar cuenta", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String[] opciones = {"Cancelar", "Eliminar cuenta"};
            int confirmacion = JOptionPane.showOptionDialog(this,
                    "¿Quieres eliminar tu cuenta y sus datos asociados? Esta acción es permanente.",
                    "Confirmar eliminación", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, opciones, opciones[0]);
            if (confirmacion == 1) {
                eliminar.setEnabled(false);
                conexion.enviar(new Paquete(Tipo.ELIMINAR_CUENTA, usuario, null, new String(clave)));
            }
        } finally {
            Arrays.fill(clave, '\0');
        }
    }

    private void recibirResultado(Paquete paquete) {
        if (eliminar.isEnabled()) return;
        eliminar.setEnabled(true);
        String mensaje = paquete.getDatos() instanceof String texto ? texto : "Respuesta inesperada del servidor.";
        if (paquete.esOk()) {
            JOptionPane.showMessageDialog(this, mensaje);
            VentanaPrincipal.volverAlLogin(conexion);
        } else {
            JOptionPane.showMessageDialog(this, mensaje, "Eliminar cuenta", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible) contrasena.setText("");
        super.setVisible(visible);
    }

    @Override
    public void dispose() {
        conexion.quitar(Tipo.ELIMINAR_CUENTA, alEliminar);
        contrasena.setText("");
        super.dispose();
    }
}
