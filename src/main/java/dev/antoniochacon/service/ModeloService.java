package dev.antoniochacon.service;

import dev.antoniochacon.model.dto.ModeloForm;
import dev.antoniochacon.model.entity.Modelo;
import dev.antoniochacon.repository.ModeloRepository;
import dev.antoniochacon.repository.RepuestoRepository;
import dev.antoniochacon.repository.VehiculoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModeloService {

    private final ModeloRepository modelos;
    private final VehiculoRepository vehiculos;
    private final RepuestoRepository repuestos;

    public ModeloService(ModeloRepository modelos, VehiculoRepository vehiculos, RepuestoRepository repuestos) {
        this.modelos = modelos;
        this.vehiculos = vehiculos;
        this.repuestos = repuestos;
    }

    public List<Modelo> listar() {
        return modelos.findAllByOrderByMarcaAscNombreAsc();
    }

    public Modelo obtener(Long id) {
        return modelos.findById(id).orElseThrow(() -> new ReglaNegocioException("El modelo no existe"));
    }

    @Transactional
    public Modelo crear(ModeloForm form) {
        String nombre = normalizar(form.getNombre());
        String marca = normalizar(form.getMarca());
        if (modelos.existsByNombreIgnoreCaseAndMarcaIgnoreCase(nombre, marca)) {
            throw new ReglaNegocioException("Ya existe un modelo con ese nombre y esa marca");
        }
        Modelo modelo = new Modelo();
        modelo.setNombre(nombre);
        modelo.setMarca(marca);
        return modelos.save(modelo);
    }

    @Transactional
    public Modelo actualizar(Long id, ModeloForm form) {
        Modelo modelo = obtener(id);
        String nombre = normalizar(form.getNombre());
        String marca = normalizar(form.getMarca());
        if (modelos.existsByNombreIgnoreCaseAndMarcaIgnoreCaseAndIdNot(nombre, marca, id)) {
            throw new ReglaNegocioException("Ya existe un modelo con ese nombre y esa marca");
        }
        modelo.setNombre(nombre);
        modelo.setMarca(marca);
        return modelos.save(modelo);
    }

    @Transactional
    public void eliminar(Long id) {
        obtener(id);
        if (vehiculos.existsByModeloId(id) || repuestos.existsByModeloId(id)) {
            throw new ReglaNegocioException("No se puede eliminar el modelo porque tiene vehículos o repuestos asociados");
        }
        modelos.deleteById(id);
    }

    public ModeloForm aFormulario(Modelo modelo) {
        ModeloForm form = new ModeloForm();
        form.setId(modelo.getId());
        form.setNombre(modelo.getNombre());
        form.setMarca(modelo.getMarca());
        return form;
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("El nombre y la marca son obligatorios");
        }
        return valor.trim().replaceAll("\\s+", " ");
    }
}
