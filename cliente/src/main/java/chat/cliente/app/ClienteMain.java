package chat.cliente.app;

import chat.comun.protocolo.Paquete;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.GridLayout;
import javax.swing.JLabel;
import java.io.IOException;
import java.util.prefs.Preferences;

/** Arranque del cliente: pide IP y puerto, se conecta y abre el login. */
public class ClienteMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClienteMain::conectar);
    }

    private static void conectar() {
        Preferences prefs = Preferences.userNodeForPackage(ClienteMain.class);
        JTextField ip = new JTextField(prefs.get("ip", "localhost"), 15);
        JTextField puerto = new JTextField(prefs.get("puerto", String.valueOf(Paquete.PUERTO)), 6);
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("IP del servidor:"));
        panel.add(ip);
        panel.add(new JLabel("Puerto:"));
        panel.add(puerto);

        while (true) {
            int r = JOptionPane.showConfirmDialog(null, panel, "Conectar al servidor",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r != JOptionPane.OK_OPTION) System.exit(0);
            try {
                int p = Integer.parseInt(puerto.getText().trim());
                Conexion conexion = new Conexion(ip.getText().trim(), p);
                prefs.put("ip", ip.getText().trim());
                prefs.put("puerto", String.valueOf(p));
                VentanaPrincipal.abrirLogin(conexion);
                return;
            } catch (NumberFormatException e) {
                error("El puerto debe ser un número.");
            } catch (IOException | IllegalArgumentException e) {
                error("No se pudo conectar a " + ip.getText().trim() + ":" + puerto.getText().trim()
                        + "\nRevisa la IP y que el servidor esté encendido.");
            }
        }
    }

    private static void error(String msg) {
        JOptionPane.showMessageDialog(null, msg, "Chat", JOptionPane.ERROR_MESSAGE);
    }
}
