package chat.servidor.gui;

import chat.comun.protocolo.Paquete;
import chat.servidor.bd.BaseDatos;
import chat.servidor.nucleo.Eventos;
import chat.servidor.nucleo.ServidorMain;
import chat.servidor.nucleo.Sesiones;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Ventana del servidor: muestra arriba la IP y el puerto, y en pestañas todo lo que pasa.
 *
 * <p>Para abrirla basta con {@code VentanaServidor.abrir(Paquete.PUERTO);} en ServidorMain, después de
 * {@code BaseDatos.inicializar()}. Eso también conecta los {@link Eventos} del núcleo con la {@link Bitacora}.
 * Para probarla sin red, corre el {@link #main} de esta clase: mete eventos de ejemplo.
 */
public class VentanaServidor extends JFrame {

    private static final String TODOS = "Todos";

    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final TableRowSorter<DefaultTableModel> ordenador;
    private final JTextField campoBuscar = new JTextField(20);
    private final JComboBox<String> comboCategoria;
    private final JLabel lblContador = new JLabel();
    private final PanelConsulta panelUsuarios;
    private final PanelConsulta panelAmistades;
    private final PanelConsulta panelGrupos;
    private final PanelConsulta panelMensajes;

    // ---- Monitoreo de carga (para ver si el servidor se está saturando) ----
    private final JLabel lblSesiones = new JLabel();
    private final JProgressBar barraCarga = new JProgressBar(0, ServidorMain.MAX_CLIENTES);
    private final JLabel lblNivel = new JLabel();
    private final JLabel lblEventosMinuto = new JLabel();
    /** Momento (en milisegundos) de cada evento nuevo; sirve para contar los del último minuto. */
    private final Deque<Long> tiemposEventos = new ArrayDeque<>();

    public VentanaServidor(int puerto) {
        super("Servidor de chat");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(crearEncabezado(puerto), BorderLayout.NORTH);

        // ---- Tabla de la bitácora: Fecha | Tipo | Evento ----
        modelo = new DefaultTableModel(new Object[]{"Fecha", "Tipo", "Evento"}, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(22);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(0).setMaxWidth(170);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(1).setMaxWidth(120);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(650);
        ordenador = new TableRowSorter<>(modelo);
        tabla.setRowSorter(ordenador);

        String[] opciones = new String[Bitacora.CATEGORIAS.length + 1];
        opciones[0] = TODOS;
        System.arraycopy(Bitacora.CATEGORIAS, 0, opciones, 1, Bitacora.CATEGORIAS.length);
        comboCategoria = new JComboBox<>(opciones);

        // ---- Pestañas con lo que hay en la BD (usan los DAOs de 02 · Base de datos) ----
        panelUsuarios = new PanelConsulta(ConsultasBD.COLUMNAS_USUARIOS, ConsultasBD::usuarios);
        panelAmistades = new PanelConsulta(ConsultasBD.COLUMNAS_AMISTADES, ConsultasBD::amistades);
        panelGrupos = new PanelConsulta(ConsultasBD.COLUMNAS_GRUPOS, ConsultasBD::grupos);
        panelMensajes = new PanelConsulta(ConsultasBD.COLUMNAS_MENSAJES, ConsultasBD::mensajes);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Bitácora", crearPestanaBitacora());
        pestanas.addTab("Usuarios", panelUsuarios);
        pestanas.addTab("Amistades", panelAmistades);
        pestanas.addTab("Grupos", panelGrupos);
        pestanas.addTab("Mensajes", panelMensajes);
        add(pestanas, BorderLayout.CENTER);

        // Cada vez que abres una pestaña de datos, se vuelve a leer la BD.
        pestanas.addChangeListener(e -> {
            Component actual = pestanas.getSelectedComponent();
            if (actual instanceof PanelConsulta) {
                ((PanelConsulta) actual).actualizar();
            }
        });

        // Lo que ya estaba anotado antes de abrir la ventana, y después cada evento nuevo.
        for (Bitacora.Evento evento : Bitacora.historial()) {
            agregarFila(evento);
        }
        Bitacora.alAgregar(this::agregarFila);
        actualizarContador();

        // Cuando alguien entra, sale o se registra, se refresca la lista de usuarios.
        Bitacora.alAgregar(evento -> {
            if (Bitacora.ACCESO.equals(evento.categoria()) || Bitacora.CONEXIONES.equals(evento.categoria())) {
                panelUsuarios.actualizar();
            }
        });
        panelUsuarios.actualizar();

        // Monitoreo: cada evento nuevo cuenta para "eventos por minuto", y cada segundo se refrescan los números.
        Bitacora.alAgregar(evento -> tiemposEventos.addLast(System.currentTimeMillis()));
        actualizarMonitoreo();
        new Timer(1000, e -> actualizarMonitoreo()).start();

        // Cerrar la ventana apaga el servidor, así que primero se pregunta.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int respuesta = JOptionPane.showConfirmDialog(VentanaServidor.this,
                        "Si cierras esta ventana se apaga el servidor y todos los clientes se desconectan.\n¿Seguro?",
                        "Apagar servidor", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (respuesta == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });
    }

    /**
     * Carga el historial guardado, conecta los eventos del núcleo con la bitácora y muestra la ventana.
     * Se llama después de BaseDatos.inicializar(). Se puede llamar desde cualquier hilo.
     */
    public static void abrir(int puerto) {
        Bitacora.cargarDesdeBD();
        Eventos.alRegistrar(Bitacora::registrar);
        SwingUtilities.invokeLater(() -> new VentanaServidor(puerto).setVisible(true));
    }

    // ------------------------------------------------------------------ partes de la ventana

    private JPanel crearEncabezado(int puerto) {
        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        encabezado.add(crearDatosConexion(puerto), BorderLayout.NORTH);
        encabezado.add(crearMonitoreo(), BorderLayout.SOUTH);
        return encabezado;
    }

    /** IP y puerto del servidor, para que los clientes sepan a dónde conectarse. */
    private JPanel crearDatosConexion(int puerto) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));

        JLabel lblEstado = new JLabel("● Encendido");
        lblEstado.setForeground(new Color(0, 140, 60));
        lblEstado.setFont(lblEstado.getFont().deriveFont(Font.BOLD));

        JLabel lblIp = new JLabel("IP: " + String.join("  /  ", ipsLocales()));
        lblIp.setFont(lblIp.getFont().deriveFont(Font.BOLD, 14f));

        JLabel lblPuerto = new JLabel("Puerto: " + puerto);
        lblPuerto.setFont(lblPuerto.getFont().deriveFont(Font.BOLD, 14f));

        JLabel lblAyuda = new JLabel("(los clientes escriben esta IP para conectarse)");
        lblAyuda.setForeground(Color.GRAY);

        panel.add(lblEstado);
        panel.add(lblIp);
        panel.add(lblPuerto);
        panel.add(lblAyuda);
        return panel;
    }

    /** Cuántas sesiones hay contra el máximo que aguanta el servidor y cuánto movimiento hay. */
    private JPanel crearMonitoreo() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        barraCarga.setStringPainted(true);
        barraCarga.setPreferredSize(new Dimension(180, 18));
        lblNivel.setFont(lblNivel.getFont().deriveFont(Font.BOLD));
        lblEventosMinuto.setToolTipText("Cuántos eventos llegaron a la bitácora en los últimos 60 segundos");

        panel.add(new JLabel("Carga:"));
        panel.add(lblSesiones);
        panel.add(barraCarga);
        panel.add(lblNivel);
        panel.add(new JLabel("·"));
        panel.add(lblEventosMinuto);
        return panel;
    }

    private JPanel crearPestanaBitacora() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        barra.add(new JLabel("Buscar:"));
        barra.add(campoBuscar);
        barra.add(new JLabel("Tipo:"));
        barra.add(comboCategoria);
        JButton btnLimpiar = new JButton("Limpiar filtros");
        barra.add(btnLimpiar);
        barra.add(lblContador);

        campoBuscar.setToolTipText("Busca en el texto del evento, por ejemplo el nombre de un usuario");
        campoBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { aplicarFiltros(); }
            @Override public void removeUpdate(DocumentEvent e) { aplicarFiltros(); }
            @Override public void changedUpdate(DocumentEvent e) { aplicarFiltros(); }
        });
        comboCategoria.addActionListener(e -> aplicarFiltros());
        btnLimpiar.addActionListener(e -> {
            campoBuscar.setText("");
            comboCategoria.setSelectedItem(TODOS);
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(barra, BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    // ------------------------------------------------------------------ lógica

    /** Agrega un renglón y, si estabas viendo lo más reciente, baja solo hasta el final. */
    private void agregarFila(Bitacora.Evento evento) {
        JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, tabla);
        boolean estabaAbajo = scroll == null || estaHastaAbajo(scroll);

        modelo.addRow(new Object[]{evento.fecha(), evento.categoria(), evento.texto()});
        actualizarContador();

        if (estabaAbajo && tabla.getRowCount() > 0) {
            int ultima = tabla.getRowCount() - 1;
            tabla.scrollRectToVisible(tabla.getCellRect(ultima, 0, true));
        }
    }

    private boolean estaHastaAbajo(JScrollPane scroll) {
        var barra = scroll.getVerticalScrollBar();
        return barra.getValue() + barra.getVisibleAmount() >= barra.getMaximum() - 30;
    }

    private void aplicarFiltros() {
        List<RowFilter<DefaultTableModel, Integer>> filtros = new ArrayList<>();

        String texto = campoBuscar.getText().trim();
        if (!texto.isEmpty()) {
            filtros.add(RowFilter.regexFilter("(?i)" + Pattern.quote(texto), 2));
        }
        String categoria = (String) comboCategoria.getSelectedItem();
        if (categoria != null && !TODOS.equals(categoria)) {
            filtros.add(RowFilter.regexFilter("^" + Pattern.quote(categoria) + "$", 1));
        }

        ordenador.setRowFilter(filtros.isEmpty() ? null : RowFilter.andFilter(filtros));
        actualizarContador();
    }

    /** Refresca los números de carga. Corre en el hilo de Swing (lo llama el Timer). */
    private void actualizarMonitoreo() {
        int sesiones = Sesiones.conectados().size();
        int maximo = ServidorMain.MAX_CLIENTES;
        lblSesiones.setText("Sesiones activas: " + sesiones + " / " + maximo);
        barraCarga.setValue(sesiones);
        barraCarga.setString(Math.round(sesiones * 100.0 / maximo) + "%");

        double porcentaje = (double) sesiones / maximo;
        if (porcentaje >= 0.9) {
            lblNivel.setText("Saturado");
            lblNivel.setForeground(new Color(190, 0, 0));
        } else if (porcentaje >= 0.6) {
            lblNivel.setText("Carga alta");
            lblNivel.setForeground(new Color(200, 120, 0));
        } else {
            lblNivel.setText("Normal");
            lblNivel.setForeground(new Color(0, 140, 60));
        }

        long haceUnMinuto = System.currentTimeMillis() - 60_000;
        while (!tiemposEventos.isEmpty() && tiemposEventos.peekFirst() < haceUnMinuto) {
            tiemposEventos.removeFirst();
        }
        lblEventosMinuto.setText("Eventos en el último minuto: " + tiemposEventos.size());
    }

    private void actualizarContador() {
        int total = modelo.getRowCount();
        int visibles = tabla.getRowCount();
        lblContador.setText(visibles == total
                ? total + " eventos"
                : "Mostrando " + visibles + " de " + total + " eventos");
    }

    /** Las IPv4 de esta computadora en la red (sin 127.0.0.1). Son las que deben escribir los clientes. */
    static List<String> ipsLocales() {
        List<String> ips = new ArrayList<>();
        try {
            for (NetworkInterface red : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!red.isUp() || red.isLoopback() || red.isVirtual()) {
                    continue;
                }
                for (InetAddress direccion : Collections.list(red.getInetAddresses())) {
                    if (direccion instanceof Inet4Address) {
                        ips.add(direccion.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // Si no se pueden leer las tarjetas de red, se muestra el aviso de abajo.
        }
        if (ips.isEmpty()) {
            ips.add("sin red (revisa el WiFi o el cable)");
        }
        return ips;
    }

    // ------------------------------------------------------------------ prueba sin servidor

    /**
     * Abre la ventana con eventos de ejemplo para verla sin clientes.
     * Las pestañas de datos leen chat.db; para llenarla corre antes {@code DatosPrueba}.
     */
    public static void main(String[] args) {
        BaseDatos.inicializar();
        abrir(Paquete.PUERTO);

        String[] ejemplos = {
            "Servidor escuchando en el puerto 5000.",
            "Conexión desde /192.168.1.31:52144",
            "[REGISTRO] de=ana para=null",
            "[LOGIN] de=ana para=null",
            "ana inició sesión.",
            "Conexión desde /192.168.1.45:50321",
            "[LOGIN] de=luis para=null",
            "luis inició sesión.",
            "[MSG_TEMPORAL] de=ana para=luis",
            "[SOLICITUD_AMISTAD] de=ana para=luis",
            "[RESPUESTA_AMISTAD] de=luis para=ana",
            "[MSG_AMIGO] de=luis para=ana",
            "[GRUPO_CREAR] de=ana para=null",
            "[GRUPO_INVITAR] de=ana para=luis",
            "[GRUPO_MSG] de=luis para=null",
            "Error atendiendo GRUPO_RENOMBRAR: java.lang.IllegalStateException: el grupo no existe",
            "luis cerró sesión.",
        };
        Thread demo = new Thread(() -> {
            for (String ejemplo : ejemplos) {
                Bitacora.registrar(ejemplo);
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        demo.setDaemon(true);
        demo.start();
    }
}
