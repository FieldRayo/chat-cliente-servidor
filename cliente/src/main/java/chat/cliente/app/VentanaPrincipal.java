package chat.cliente.app;

import chat.comun.protocolo.Paquete;
import chat.comun.protocolo.Tipo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * El menú que aparece después del login.
 *
 * <p><b>Contrato para las demás áreas:</b>
 * <ul>
 *   <li>Cada ventana es un {@code JFrame} con constructor público {@code (Conexion conexion)}.
 *       El usuario con sesión está en {@code conexion.getUsuario()}.</li>
 *   <li>El menú crea cada ventana UNA sola vez al iniciar sesión, oculta. Así recibe sus paquetes
 *       (mensajes, solicitudes, invitaciones) aunque el usuario no la haya abierto.
 *       Suscríbete con {@code conexion.alRecibir(...)} en tu constructor y no hace falta quitar nada.</li>
 *   <li>La X de tu ventana solo la esconde (el menú se encarga): cerrar una ventana no es cerrar la sesión.
 *       Si quieres llamar la atención del usuario, puedes hacer {@code setVisible(true)} tú mismo.</li>
 * </ul>
 *
 * 06 · Login, al recibir OK, hace:
 * <pre>
 * conexion.setUsuario(usuario);
 * new VentanaPrincipal(conexion).setVisible(true);
 * dispose();
 * </pre>
 * 07 · Cuenta, al recibir OK de ELIMINAR_CUENTA, hace {@code VentanaPrincipal.volverAlLogin(conexion)}.
 */
public class VentanaPrincipal extends JFrame {
    private final transient Conexion conexion;

    public VentanaPrincipal(Conexion conexion) {
        super("Chat · " + conexion.getUsuario());
        this.conexion = conexion;

        JPanel botones = new JPanel(new GridLayout(0, 1, 8, 8));
        botones.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        botones.add(boton("Usuarios", "chat.cliente.usuarios.VentanaUsuarios"));
        botones.add(boton("Amigos", "chat.cliente.chatamigos.VentanaAmigos"));
        botones.add(boton("Grupos", "chat.cliente.grupos.VentanaGrupos"));
        botones.add(boton("Notificaciones", "chat.cliente.amistades.VentanaNotificaciones"));
        botones.add(boton("Eliminar cuenta", "chat.cliente.cuenta.VentanaCuenta"));
        JButton salir = new JButton("Cerrar sesión");
        salir.addActionListener(e -> cerrarSesion());
        botones.add(salir);
        add(botones);

        // Cerrar el menú con la X = salir del programa (el servidor detecta la desconexión).
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                conexion.enviar(new Paquete(Tipo.LOGOUT, conexion.getUsuario(), null, null));
                System.exit(0);
            }
        });
        pack();
        setSize(Math.max(getWidth(), 280), getHeight());
        setLocationRelativeTo(null);
    }

    /** Crea la ventana ya (oculta, escuchando) y el botón solo la muestra. */
    private JButton boton(String texto, String clase) {
        JFrame ventana = crear(clase, conexion);
        if (ventana != null) ventana.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        JButton b = new JButton(texto);
        b.addActionListener(e -> {
            if (ventana == null) {
                JOptionPane.showMessageDialog(this, "Esta ventana todavía no está lista:\n" + clase,
                        "Chat", JOptionPane.INFORMATION_MESSAGE);
            } else {
                ventana.setVisible(true);
                ventana.toFront();
            }
        });
        return b;
    }

    /** Único lugar donde se hace LOGOUT. */
    private void cerrarSesion() {
        conexion.enviar(new Paquete(Tipo.LOGOUT, conexion.getUsuario(), null, null));
        volverAlLogin(conexion);
    }

    /**
     * Cierra todas las ventanas de la sesión y abre el login, sin mandar nada al servidor.
     * Lo usa 07 · Cuenta después de eliminar la cuenta.
     */
    public static void volverAlLogin(Conexion conexion) {
        conexion.setUsuario(null);
        conexion.quitarTodos();
        for (Frame f : Frame.getFrames()) f.dispose();
        abrirLogin(conexion);
    }

    static void abrirLogin(Conexion conexion) {
        JFrame login = crear("chat.cliente.login.VentanaLogin", conexion);
        if (login != null) {
            login.setVisible(true);
        } else {
            // ponytail: sin login todavía (fase 1) se entra directo al menú para probar la red.
            conexion.setUsuario("prueba");
            new VentanaPrincipal(conexion).setVisible(true);
        }
    }

    // ponytail: por reflexión para que Cliente.jar compile aunque un área no haya entregado su ventana.
    // Cuando existan las 7 ventanas, cambiar por `new VentanaX(conexion)` directo.
    private static JFrame crear(String clase, Conexion conexion) {
        try {
            return (JFrame) Class.forName(clase).getConstructor(Conexion.class).newInstance(conexion);
        } catch (ClassNotFoundException e) {
            System.err.println("Aún no existe " + clase);
        } catch (ReflectiveOperationException | ClassCastException e) {
            System.err.println(clase + " debe ser un JFrame con constructor público (Conexion):");
            e.printStackTrace();
        }
        return null;
    }
}
