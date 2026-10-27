package dev.antoniochacon.service;

import dev.antoniochacon.model.dto.RepuestoForm;
import dev.antoniochacon.model.entity.Modelo;
import dev.antoniochacon.model.entity.Repuesto;
import dev.antoniochacon.repository.ModeloRepository;
import dev.antoniochacon.repository.RepuestoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepuestoService {

    public static final int STOCK_BAJO = 5;

    private final RepuestoRepository repuestos;
    private final ModeloRepository modelos;

    public RepuestoService(RepuestoRepository repuestos, ModeloRepository modelos) {
        this.repuestos = repuestos;
        this.modelos = modelos;
    }

    public List<Repuesto> listar() {
        return repuestos.findAllByOrderByNombreAsc();
    }

    public List<Repuesto> buscarCompatibles(Long modeloId) {
        if (modeloId == null || modelos.findById(modeloId).isEmpty()) {
            throw new ReglaNegocioException("El modelo no existe");
        }
        return repuestos.findByModeloIdOrderByNombreAsc(modeloId);
    }

    public List<Repuesto> stockBajo() {
        return repuestos.findByStockLessThanOrderByStockAsc(STOCK_BAJO);
    }

    public Repuesto obtener(Long id) {
        return repuestos.findById(id).orElseThrow(() -> new ReglaNegocioException("El repuesto no existe"));
    }

    @Transactional
    public Repuesto crear(RepuestoForm form) {
        String ean = validar(form);
        if (repuestos.existsByCodigoEan(ean)) {
            throw new ReglaNegocioException("El código EAN ya está registrado");
        }
        Repuesto repuesto = new Repuesto();
        aplicar(repuesto, form, ean);
        return repuestos.save(repuesto);
    }

    @Transactional
    public Repuesto actualizar(Long id, RepuestoForm form) {
        Repuesto repuesto = obtener(id);
        String ean = validar(form);
        if (repuestos.existsByCodigoEanAndIdNot(ean, id)) {
            throw new ReglaNegocioException("El código EAN ya está registrado");
        }
        aplicar(repuesto, form, ean);
        return repuestos.save(repuesto);
    }

    @Transactional
    public void eliminar(Long id) {
        obtener(id);
        repuestos.deleteById(id);
    }

    public RepuestoForm aFormulario(Repuesto repuesto) {
        RepuestoForm form = new RepuestoForm();
        form.setId(repuesto.getId());
        form.setNombre(repuesto.getNombre());
        form.setDescripcion(repuesto.getDescripcion());
        form.setPrecio(repuesto.getPrecio());
        form.setCodigoEan(repuesto.getCodigoEan());
        form.setStock(repuesto.getStock());
        form.setImagenUrl(repuesto.getImagenUrl());
        form.setModeloId(repuesto.getModelo().getId());
        return form;
    }

    private String validar(RepuestoForm form) {
        String ean = form.getCodigoEan() == null ? "" : form.getCodigoEan().trim();
        if (!ean.matches("\\d{13}")) {
            throw new ReglaNegocioException("El código EAN debe tener 13 dígitos");
        }
        if (form.getPrecio() == null || form.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocioException("El precio debe ser mayor que cero");
        }
        if (form.getStock() == null || form.getStock() < 0) {
            throw new ReglaNegocioException("El stock no puede ser negativo");
        }
        return ean;
    }

    private void aplicar(Repuesto repuesto, RepuestoForm form, String ean) {
        Modelo modelo = modelos.findById(form.getModeloId())
                .orElseThrow(() -> new ReglaNegocioException("El modelo compatible no existe"));
        repuesto.setNombre(form.getNombre().trim());
        repuesto.setDescripcion(blancoANulo(form.getDescripcion()));
        repuesto.setPrecio(form.getPrecio());
        repuesto.setCodigoEan(ean);
        repuesto.setStock(form.getStock());
        repuesto.setImagenUrl(blancoANulo(form.getImagenUrl()));
        repuesto.setModelo(modelo);
    }

    private String blancoANulo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
