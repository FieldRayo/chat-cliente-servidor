package chat.servidor.bd;

import chat.servidor.util.Hash;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga datos de prueba para que los demás no tengan que registrarse cada vez.
 * Se ejecuta una sola vez (si "ana" ya existe, no hace nada).
 * Todos los usuarios tienen la contraseña "Test1234" y respuestas de seguridad
 * "rojo", "azul" y "verde" a las 3 primeras preguntas del catálogo.
 */
public class DatosPrueba {
    private static final String CLAVE_PRUEBA = "Test1234";

    private static final String[] PREGUNTAS = {
        "¿Cómo se llamaba tu primera mascota?",
        "¿En qué ciudad nació tu madre?",
        "¿Cuál es el nombre de tu mejor amigo de la infancia?",
        "¿Cómo se llamaba tu primera escuela?",
        "¿Cuál fue tu primer videojuego favorito?",
        "¿Cuál es tu comida favorita de la infancia?",
        "¿Cuál era el apodo que te decían de niño?",
        "¿Cómo se llamaba tu maestro favorito?"
    };

    public static void main(String[] args) {
        BaseDatos.inicializar();

        UsuarioDAO usuarios = new UsuarioDAO();
        SeguridadDAO seguridad = new SeguridadDAO();
        AmistadDAO amistades = new AmistadDAO();
        MensajeAmigoDAO mensajesAmigos = new MensajeAmigoDAO();
        GrupoDAO grupos = new GrupoDAO();

        for (String p : PREGUNTAS) {
            seguridad.agregarPregunta(p);
        }

        if (usuarios.existeNombre("ana")) {
            System.out.println("Los datos de prueba ya estaban cargados.");
            return;
        }

        List<Integer> idsPreguntas = List.copyOf(seguridad.listarPreguntas().keySet());
        int ana = crearUsuario(usuarios, "ana", idsPreguntas);
        int beto = crearUsuario(usuarios, "beto", idsPreguntas);
        int carla = crearUsuario(usuarios, "carla", idsPreguntas);
        int diego = crearUsuario(usuarios, "diego", idsPreguntas);
        crearUsuario(usuarios, "eva", idsPreguntas);

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

    private static int crearUsuario(UsuarioDAO usuarios, String nombre, List<Integer> idsPreguntas) {
        String[] respuestas = {"rojo", "azul", "verde"};
        Map<Integer, String> hashes = new LinkedHashMap<>();
        for (int i = 0; i < 3; i++) {
            hashes.put(idsPreguntas.get(i), Hash.hashear(Hash.normalizarRespuesta(respuestas[i])));
        }
        return usuarios.registrar(nombre, nombre + "@prueba.com", Hash.hashear(CLAVE_PRUEBA), hashes);
    }
}
