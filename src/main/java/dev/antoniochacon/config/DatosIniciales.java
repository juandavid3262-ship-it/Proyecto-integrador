package dev.antoniochacon.config;

import dev.antoniochacon.model.entity.Modelo;
import dev.antoniochacon.model.entity.Reparacion;
import dev.antoniochacon.model.entity.Repuesto;
import dev.antoniochacon.model.entity.Usuario;
import dev.antoniochacon.model.entity.Vehiculo;
import dev.antoniochacon.model.enums.ColorPlaca;
import dev.antoniochacon.model.enums.EstadoReparacion;
import dev.antoniochacon.model.enums.Rol;
import dev.antoniochacon.model.enums.Tema;
import dev.antoniochacon.repository.ModeloRepository;
import dev.antoniochacon.repository.ReparacionRepository;
import dev.antoniochacon.repository.RepuestoRepository;
import dev.antoniochacon.repository.UsuarioRepository;
import dev.antoniochacon.repository.VehiculoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final UsuarioRepository usuarios;
    private final ModeloRepository modelos;
    private final VehiculoRepository vehiculos;
    private final RepuestoRepository repuestos;
    private final ReparacionRepository reparaciones;
    private final PasswordEncoder encoder;

    public DatosIniciales(
            UsuarioRepository usuarios,
            ModeloRepository modelos,
            VehiculoRepository vehiculos,
            RepuestoRepository repuestos,
            ReparacionRepository reparaciones,
            PasswordEncoder encoder
    ) {
        this.usuarios = usuarios;
        this.modelos = modelos;
        this.vehiculos = vehiculos;
        this.repuestos = repuestos;
        this.reparaciones = reparaciones;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (usuarios.count() > 0) {
            return;
        }
        Usuario admin = usuario("Antonio Chacón", "admin@taller.com", "Admin123*", Rol.ADMINISTRADOR, Tema.CLARO);
        Usuario recepcion = usuario("Juan David Neira", "recepcion@taller.com", "Recepcion123*", Rol.RECEPCIONISTA, Tema.CLARO);
        Usuario carlos = usuario("Carlos Méndez", "mecanico@taller.com", "Mecanico123*", Rol.MECANICO, Tema.OSCURO);
        Usuario laura = usuario("Laura Gómez", "mecanico2@taller.com", "Mecanico456*", Rol.MECANICO, Tema.CLARO);
        usuarios.save(admin);
        usuarios.save(recepcion);
        usuarios.save(carlos);
        usuarios.save(laura);

        Modelo spark = modelo("Spark", "Chevrolet");
        Modelo logan = modelo("Logan", "Renault");
        Modelo mazda3 = modelo("3", "Mazda");
        Modelo rio = modelo("Rio", "Kia");
        modelos.save(spark);
        modelos.save(logan);
        modelos.save(mazda3);
        modelos.save(rio);

        Vehiculo v1 = vehiculo("ABC-123", ColorPlaca.AMARILLO, "Marta López", spark);
        Vehiculo v2 = vehiculo("XYZ-890", ColorPlaca.BLANCO, "Pedro Ruiz", logan);
        Vehiculo v3 = vehiculo("JDM-456", ColorPlaca.ROJO, "Sofía Álvarez", mazda3);
        Vehiculo v4 = vehiculo("QWE-111", ColorPlaca.AZUL, "Andrés Gil", spark);
        Vehiculo v5 = vehiculo("RTY-222", ColorPlaca.VERDE, "Helena Díaz", rio);
        Vehiculo v6 = vehiculo("UIO-333", ColorPlaca.BLANCO, "Camilo Ortiz", logan);
        vehiculos.save(v1);
        vehiculos.save(v2);
        vehiculos.save(v3);
        vehiculos.save(v4);
        vehiculos.save(v5);
        vehiculos.save(v6);

        repuestos.save(repuesto("Filtro de aceite", "Filtro para motor 1.2", "45000", "7701234567001", 12, spark));
        repuestos.save(repuesto("Pastillas de freno", "Juego delantero", "128000", "7701234567002", 3, spark));
        repuestos.save(repuesto("Bujía", "Bujía de iridio", "32000", "7701234567003", 2, logan));
        repuestos.save(repuesto("Amortiguador", "Delantero derecho", "210000", "7701234567004", 6, mazda3));
        repuestos.save(repuesto("Correa de repartición", "Kit con tensor", "185000", "7701234567005", 1, rio));
        repuestos.save(repuesto("Filtro de aire", "Elemento de cabina", "38000", "7701234567006", 9, logan));
        repuestos.save(repuesto("Disco de freno", "Ventilado 280 mm", "156000", "7701234567007", 4, mazda3));
        repuestos.save(repuesto("Aceite 10W30", "Galón semisintético", "89000", "7701234567008", 15, spark));

        reparacion(LocalDate.of(2026, 1, 14), "Cambio de aceite", EstadoReparacion.TERMINADO, v1, carlos);
        reparacion(LocalDate.of(2026, 2, 3), "Frenos", EstadoReparacion.TERMINADO, v2, laura);
        reparacion(LocalDate.of(2026, 3, 19), "Suspensión", EstadoReparacion.CANCELADO, v3, carlos);
        reparacion(LocalDate.of(2026, 4, 8), "Diagnóstico", EstadoReparacion.EN_PROCESO, v4, laura);
        reparacion(LocalDate.of(2026, 5, 21), "Cambio de aceite", EstadoReparacion.TERMINADO, v5, carlos);
        reparacion(LocalDate.of(2026, 6, 11), "Frenos", EstadoReparacion.EN_PROCESO, v6, laura);
        reparacion(LocalDate.of(2026, 7, 2), "Diagnóstico", EstadoReparacion.TERMINADO, v1, laura);
        reparacion(LocalDate.of(2026, 8, 17), "Suspensión", EstadoReparacion.TERMINADO, v3, carlos);
        reparacion(LocalDate.of(2026, 9, 9), "Frenos", EstadoReparacion.CANCELADO, v4, carlos);
        reparacion(LocalDate.of(2026, 10, 6), "Cambio de aceite", EstadoReparacion.EN_PROCESO, v2, laura);
        reparacion(LocalDate.of(2026, 10, 22), "Diagnóstico", EstadoReparacion.TERMINADO, v5, carlos);

        log.info("Taller listo en http://localhost:8080  (admin@taller.com / Admin123*)");
    }

    private Usuario usuario(String nombre, String email, String clave, Rol rol, Tema tema) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setPasswordHash(encoder.encode(clave));
        usuario.setRol(rol);
        usuario.setTemaPreferido(tema);
        usuario.setSidebarPlegada(false);
        return usuario;
    }

    private Modelo modelo(String nombre, String marca) {
        Modelo modelo = new Modelo();
        modelo.setNombre(nombre);
        modelo.setMarca(marca);
        return modelo;
    }

    private Vehiculo vehiculo(String placa, ColorPlaca color, String dueno, Modelo modelo) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPlaca(placa);
        vehiculo.setColorPlaca(color);
        vehiculo.setDueno(dueno);
        vehiculo.setModelo(modelo);
        return vehiculo;
    }

    private Repuesto repuesto(String nombre, String descripcion, String precio, String ean, int stock, Modelo modelo) {
        Repuesto repuesto = new Repuesto();
        repuesto.setNombre(nombre);
        repuesto.setDescripcion(descripcion);
        repuesto.setPrecio(new BigDecimal(precio));
        repuesto.setCodigoEan(ean);
        repuesto.setStock(stock);
        repuesto.setImagenUrl("https://placehold.co/96x96/1c2f24/f3ecdf?text=" + nombre.split(" ")[0]);
        repuesto.setModelo(modelo);
        return repuesto;
    }

    private void reparacion(LocalDate fecha, String tipo, EstadoReparacion estado, Vehiculo vehiculo, Usuario mecanico) {
        Reparacion reparacion = new Reparacion();
        reparacion.setFecha(fecha);
        reparacion.setTipo(tipo);
        reparacion.setEstado(estado);
        reparacion.setVehiculo(vehiculo);
        reparacion.setMecanico(mecanico);
        reparaciones.save(reparacion);
    }
}
