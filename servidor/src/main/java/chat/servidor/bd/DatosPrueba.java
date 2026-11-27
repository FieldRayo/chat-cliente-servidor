package chat.servidor.bd;

import chat.servidor.registro.Hash;
import java.util.List;

/**
 * Carga datos de prueba para que los demás no tengan que registrarse cada vez.
 * Se ejecuta una sola vez (si "ana" ya existe, no hace nada).
 * Todos los usuarios tienen la contraseña "Test1234" y responden "rojo"
 * a la pregunta 1 (las preguntas las inserta schema.sql).
 */
public class DatosPrueba {
    private static final String CLAVE_PRUEBA = "Test1234";

    public static void main(String[] args) {
        BaseDatos.inicializar();

        UsuarioDAO usuarios = new UsuarioDAO();
        AmistadDAO amistades = new AmistadDAO();
        MensajeAmigoDAO mensajesAmigos = new MensajeAmigoDAO();
        GrupoDAO grupos = new GrupoDAO();

        if (usuarios.existeNombre("ana")) {
            System.out.println("Los datos de prueba ya estaban cargados.");
            return;
        }

        int ana = crearUsuario(usuarios, "ana");
        int beto = crearUsuario(usuarios, "beto");
        int carla = crearUsuario(usuarios, "carla");
        int diego = crearUsuario(usuarios, "diego");
        crearUsuario(usuarios, "eva");

        amistades.aceptar(amistades.enviarSolicitud(ana, beto), beto);
        amistades.aceptar(amistades.enviarSolicitud(ana, carla), carla);
        amistades.aceptar(amistades.enviarSolicitud(beto, carla), carla);
        amistades.enviarSolicitud(diego, ana);

        mensajesAmigos.guardar(ana, beto, "Hola Beto, ¿listo para la demo?");
        mensajesAmigos.guardar(beto, ana, "Listo. Ya probé el login.");

        int equipo = grupos.crear("Equipo CocoChat", ana, List.of(beto, carla));
        grupos.aceptarInvitacion(equipo, beto);
        grupos.aceptarInvitacion(equipo, carla);
        grupos.guardarMensaje(equipo, ana, "Bienvenidos al grupo.");
        grupos.guardarMensaje(equipo, beto, "Gracias, Ana.");

        System.out.println("Datos de prueba cargados.");
        System.out.println("Usuarios: ana, beto, carla, diego, eva (clave: " + CLAVE_PRUEBA + ")");
        System.out.println("Amigos: ana-beto, ana-carla, beto-carla. Solicitud pendiente: diego -> ana.");
        System.out.println("Grupo \"Equipo CocoChat\": dueña ana, miembros beto y carla.");
    }

    private static int crearUsuario(UsuarioDAO usuarios, String nombre) {
        return usuarios.registrar(nombre, nombre + "@prueba.com", Hash.generar(CLAVE_PRUEBA),
                1, Hash.generarRespuesta("rojo"));
    }
}
