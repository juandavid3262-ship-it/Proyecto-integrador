package dev.antoniochacon;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.antoniochacon.model.dto.ReparacionForm;
import dev.antoniochacon.model.entity.Reparacion;
import dev.antoniochacon.model.entity.Usuario;
import dev.antoniochacon.repository.ReparacionRepository;
import dev.antoniochacon.repository.UsuarioRepository;
import dev.antoniochacon.service.AccesoDenegadoNegocioException;
import dev.antoniochacon.service.ReglaNegocioException;
import dev.antoniochacon.service.ReparacionService;
import dev.antoniochacon.service.VehiculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class TallerIntegradorTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @Autowired
    private VehiculoService vehiculos;

    @Autowired
    private ReparacionService reparaciones;

    @Autowired
    private ReparacionRepository reparacionRepository;

    @Autowired
    private UsuarioRepository usuarios;

    @BeforeEach
    void prepararCliente() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void rechazaPlacaFueraDelFormatoColombiano() {
        assertThrows(ReglaNegocioException.class, () -> vehiculos.normalizarPlaca("12-AB"));
    }

    @Test
    void encuentraElModeloDeUnaPlacaSemilla() {
        assertTrue(vehiculos.buscarModeloPorPlaca("abc-123").isPresent());
    }

    @Test
    void mecanicoNoModificaLaReparacionDeOtro() {
        Usuario laura = usuarios.findByEmail("mecanico2@taller.com").orElseThrow();
        Reparacion ajena = reparacionRepository.findAllByOrderByFechaDescIdDesc().stream()
                .filter(reparacion -> !reparacion.getMecanico().getId().equals(laura.getId()))
                .findFirst()
                .orElseThrow();
        ReparacionForm form = reparaciones.aFormulario(ajena);
        assertThrows(AccesoDenegadoNegocioException.class, () -> reparaciones.actualizar(ajena.getId(), form, laura));
    }

    @Test
    void visitanteVaAlLogin() throws Exception {
        mvc.perform(get("/inicio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void recepcionistaNoEntraAlDashboard() throws Exception {
        mvc.perform(get("/dashboard").with(user("recepcion@taller.com").roles("RECEPCIONISTA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rangoInvertidoResponde400() throws Exception {
        mvc.perform(get("/dashboard")
                        .param("desde", "2026-10-10")
                        .param("hasta", "2026-01-01")
                        .with(user("admin@taller.com").roles("ADMINISTRADOR")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void descargaElHistorialEnPdfYCsv() throws Exception {
        mvc.perform(get("/reportes/reparaciones/pdf")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-10-27")
                        .with(user("admin@taller.com").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("%PDF")));

        mvc.perform(get("/reportes/reparaciones/csv")
                        .param("desde", "2026-01-01")
                        .param("hasta", "2026-10-27")
                        .with(user("admin@taller.com").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(content().string(containsString("fecha,tipo,estado,placa")));
    }

    @Test
    void elLoginRechazaUnaClaveIncorrecta() throws Exception {
        mvc.perform(post("/login")
                        .param("email", "admin@taller.com")
                        .param("password", "clave-mala")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }
}
