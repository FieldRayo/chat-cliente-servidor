package chat.servidor.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Una pestaña con una tabla de solo lectura que se llena con datos de la BD.
 * La consulta corre en otro hilo (SwingWorker) para que la ventana no se congele.
 */
class PanelConsulta extends JPanel {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DefaultTableModel modelo;
    private final JTable tabla;
    private final TableRowSorter<DefaultTableModel> ordenador;
    private final JTextField campoBuscar = new JTextField(18);
    private final JLabel lblEstado = new JLabel(" ");
    private final Supplier<List<Object[]>> consulta;
    private boolean cargando = false;

    /**
     * @param columnas nombres de las columnas
     * @param consulta código que trae las filas de la BD (corre fuera del hilo de Swing)
     */
    PanelConsulta(String[] columnas, Supplier<List<Object[]>> consulta) {
        super(new BorderLayout());
        this.consulta = consulta;

        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(22);
        ordenador = new TableRowSorter<>(modelo);
        tabla.setRowSorter(ordenador);

        JButton btnActualizar = new JButton("Actualizar");
        btnActualizar.addActionListener(e -> actualizar());

        campoBuscar.setToolTipText("Busca en todas las columnas");
        campoBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        barra.add(btnActualizar);
        barra.add(new JLabel("Buscar:"));
        barra.add(campoBuscar);
        barra.add(lblEstado);

        add(barra, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    /** Vuelve a consultar la BD. Si ya hay una consulta en curso, no lanza otra. */
    void actualizar() {
        if (cargando) {
            return;
        }
        cargando = true;
        lblEstado.setForeground(Color.GRAY);
        lblEstado.setText("Cargando…");

        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() {
                return consulta.get();
            }

            @Override
            protected void done() {
                cargando = false;
                try {
                    List<Object[]> filas = get();
                    modelo.setRowCount(0);
                    for (Object[] fila : filas) {
                        modelo.addRow(fila);
                    }
                    lblEstado.setForeground(Color.GRAY);
                    lblEstado.setText(filas.size() + " registros · actualizado a las " + LocalTime.now().format(HORA));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause() != null ? e.getCause() : e;
                    lblEstado.setForeground(new Color(180, 0, 0));
                    lblEstado.setText("No se pudo leer la base de datos: " + causa.getMessage());
                }
            }
        }.execute();
    }

    private void filtrar() {
        String texto = campoBuscar.getText().trim();
        ordenador.setRowFilter(texto.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(texto)));
    }
}
