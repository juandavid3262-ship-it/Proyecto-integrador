package dev.antoniochacon.service;

import dev.antoniochacon.model.dto.ReparacionForm;
import dev.antoniochacon.model.entity.Reparacion;
import dev.antoniochacon.model.entity.Usuario;
import dev.antoniochacon.model.entity.Vehiculo;
import dev.antoniochacon.model.enums.EstadoReparacion;
import dev.antoniochacon.model.enums.Rol;
import dev.antoniochacon.repository.ReparacionRepository;
import dev.antoniochacon.repository.UsuarioRepository;
import dev.antoniochacon.repository.VehiculoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReparacionService {

    private final ReparacionRepository reparaciones;
    private final VehiculoRepository vehiculos;
    private final UsuarioRepository usuarios;

    public ReparacionService(
            ReparacionRepository reparaciones,
            VehiculoRepository vehiculos,
            UsuarioRepository usuarios
    ) {
        this.reparaciones = reparaciones;
        this.vehiculos = vehiculos;
        this.usuarios = usuarios;
    }

    public Reparacion obtenerEditable(Long id, Usuario actor) {
        Reparacion reparacion = reparaciones.findById(id)
                .orElseThrow(() -> new ReglaNegocioException("La reparación no existe"));
        verificarPuedeModificar(reparacion, actor);
        return reparacion;
    }

    public List<Reparacion> listar(Usuario actor) {
        if (actor.getRol() == Rol.MECANICO) {
            return reparaciones.findByMecanicoIdOrderByFechaDescIdDesc(actor.getId());
        }
        return reparaciones.findAllByOrderByFechaDescIdDesc();
    }

    public List<Reparacion> buscarPorPeriodo(java.time.LocalDate desde, java.time.LocalDate hasta) {
        validarRango(desde, hasta);
        return reparaciones.findByFechaBetweenOrderByFechaDescIdDesc(desde, hasta);
    }

    @Transactional
    public Reparacion crear(ReparacionForm form, Usuario actor) {
        Reparacion reparacion = new Reparacion();
        aplicar(reparacion, form, actor);
        return reparaciones.save(reparacion);
    }

    @Transactional
    public Reparacion actualizar(Long id, ReparacionForm form, Usuario actor) {
        Reparacion reparacion = obtener(id);
        verificarPuedeModificar(reparacion, actor);
        aplicar(reparacion, form, actor);
        return reparaciones.save(reparacion);
    }

    @Transactional
    public void eliminar(Long id, Usuario actor) {
        Reparacion reparacion = obtener(id);
        verificarPuedeModificar(reparacion, actor);
        reparaciones.delete(reparacion);
    }

    public ReparacionForm aFormulario(Reparacion reparacion) {
        ReparacionForm form = new ReparacionForm();
        form.setId(reparacion.getId());
        form.setFecha(reparacion.getFecha());
        form.setTipo(reparacion.getTipo());
        form.setEstado(reparacion.getEstado());
        form.setVehiculoId(reparacion.getVehiculo().getId());
        form.setMecanicoId(reparacion.getMecanico().getId());
        return form;
    }

    public void validarRango(java.time.LocalDate desde, java.time.LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ReglaNegocioException("Indica la fecha inicial y la fecha final");
        }
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la fecha final");
        }
    }

    private Reparacion obtener(Long id) {
        return reparaciones.findById(id).orElseThrow(() -> new ReglaNegocioException("La reparación no existe"));
    }

    private void aplicar(Reparacion reparacion, ReparacionForm form, Usuario actor) {
        if (form.getEstado() == null) {
            throw new ReglaNegocioException("El estado debe ser terminado, cancelado o en proceso");
        }
        EstadoReparacion estado = form.getEstado();
        Vehiculo vehiculo = vehiculos.findById(form.getVehiculoId())
                .orElseThrow(() -> new ReglaNegocioException("El vehículo no existe"));
        Long mecanicoId = actor.getRol() == Rol.MECANICO ? actor.getId() : form.getMecanicoId();
        if (mecanicoId == null) {
            throw new ReglaNegocioException("Debes asignar un mecánico");
        }
        Usuario mecanico = usuarios.findByIdAndRol(mecanicoId, Rol.MECANICO)
                .orElseThrow(() -> new ReglaNegocioException("El usuario asignado debe tener el rol de mecánico"));
        reparacion.setFecha(form.getFecha());
        reparacion.setTipo(form.getTipo().trim());
        reparacion.setEstado(estado);
        reparacion.setVehiculo(vehiculo);
        reparacion.setMecanico(mecanico);
    }

    private void verificarPuedeModificar(Reparacion reparacion, Usuario actor) {
        if (actor.getRol() == Rol.MECANICO && !reparacion.getMecanico().getId().equals(actor.getId())) {
            throw new AccesoDenegadoNegocioException("Solo puedes modificar tus propias reparaciones");
        }
    }
}
