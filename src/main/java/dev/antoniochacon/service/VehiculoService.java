package dev.antoniochacon.service;

import dev.antoniochacon.model.dto.VehiculoForm;
import dev.antoniochacon.model.entity.Modelo;
import dev.antoniochacon.model.entity.Vehiculo;
import dev.antoniochacon.repository.ModeloRepository;
import dev.antoniochacon.repository.ReparacionRepository;
import dev.antoniochacon.repository.VehiculoRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehiculoService {

    public static final Pattern PLACA = Pattern.compile("^[A-Z]{3}-\\d{3}$");

    private final VehiculoRepository vehiculos;
    private final ModeloRepository modelos;
    private final ReparacionRepository reparaciones;

    public VehiculoService(VehiculoRepository vehiculos, ModeloRepository modelos, ReparacionRepository reparaciones) {
        this.vehiculos = vehiculos;
        this.modelos = modelos;
        this.reparaciones = reparaciones;
    }

    public List<Vehiculo> listar() {
        return vehiculos.findAllByOrderByPlacaAsc();
    }

    public Vehiculo obtener(Long id) {
        return vehiculos.findById(id).orElseThrow(() -> new ReglaNegocioException("El vehículo no existe"));
    }

    public Optional<Vehiculo> buscarModeloPorPlaca(String placa) {
        return vehiculos.findByPlaca(normalizarPlaca(placa));
    }

    @Transactional
    public Vehiculo crear(VehiculoForm form) {
        String placa = normalizarPlaca(form.getPlaca());
        if (vehiculos.existsByPlaca(placa)) {
            throw new ReglaNegocioException("La placa ya está registrada");
        }
        Vehiculo vehiculo = new Vehiculo();
        aplicar(vehiculo, form, placa);
        return vehiculos.save(vehiculo);
    }

    @Transactional
    public Vehiculo actualizar(Long id, VehiculoForm form) {
        Vehiculo vehiculo = obtener(id);
        String placa = normalizarPlaca(form.getPlaca());
        if (vehiculos.existsByPlacaAndIdNot(placa, id)) {
            throw new ReglaNegocioException("La placa ya está registrada");
        }
        aplicar(vehiculo, form, placa);
        return vehiculos.save(vehiculo);
    }

    @Transactional
    public void eliminar(Long id) {
        obtener(id);
        if (reparaciones.existsByVehiculoId(id)) {
            throw new ReglaNegocioException("No se puede eliminar el vehículo porque tiene reparaciones en su historial");
        }
        vehiculos.deleteById(id);
    }

    public VehiculoForm aFormulario(Vehiculo vehiculo) {
        VehiculoForm form = new VehiculoForm();
        form.setId(vehiculo.getId());
        form.setPlaca(vehiculo.getPlaca());
        form.setColorPlaca(vehiculo.getColorPlaca());
        form.setDueno(vehiculo.getDueno());
        form.setModeloId(vehiculo.getModelo().getId());
        return form;
    }

    public String normalizarPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new ReglaNegocioException("La placa es obligatoria");
        }
        String normalizada = placa.trim().toUpperCase(Locale.ROOT);
        if (!PLACA.matcher(normalizada).matches()) {
            throw new ReglaNegocioException("La placa debe tener el formato ABC-123");
        }
        return normalizada;
    }

    private void aplicar(Vehiculo vehiculo, VehiculoForm form, String placa) {
        if (form.getColorPlaca() == null) {
            throw new ReglaNegocioException("El color debe ser amarillo, blanco, rojo, azul o verde");
        }
        Modelo modelo = modelos.findById(form.getModeloId())
                .orElseThrow(() -> new ReglaNegocioException("El modelo no existe"));
        vehiculo.setPlaca(placa);
        vehiculo.setColorPlaca(form.getColorPlaca());
        vehiculo.setDueno(form.getDueno().trim());
        vehiculo.setModelo(modelo);
    }
}
