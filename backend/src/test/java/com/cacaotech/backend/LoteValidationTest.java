package com.cacaotech.backend;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import com.cacaotech.backend.model.Lote;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LoteValidationTest {

    @Autowired
    private LoteRepository loteRepository;

    private Lote crearLoteValido(String codigo) {
        Lote lote = new Lote();
        lote.setCodigo(codigo);
        lote.setProductor("Productor Demo");
        lote.setOrigen("Huila");
        lote.setFechaRegistro(LocalDate.now());
        lote.setCantidadInicial(new BigDecimal("100.000"));
        lote.setCantidadDisponible(new BigDecimal("100.000"));
        lote.setUnidad("KG");
        lote.setTipoLote(Lote.TipoLote.RECEPCION);
        lote.setEstado(Lote.EstadoLote.PENDIENTE);
        return lote;
    }

    @Test
    @DisplayName("Debe rechazar un lote con cantidadInicial negativa")
    void debeRechazarCantidadInicialNegativa() {
        Lote lote = crearLoteValido("LOTE-NEG-INI");
        lote.setCantidadInicial(new BigDecimal("-10.000"));

        assertThatThrownBy(() -> {
            loteRepository.saveAndFlush(lote);
        }).satisfies(exception -> {
            boolean hasConstraintViolation = exception instanceof ConstraintViolationException
                    || exception.getCause() instanceof ConstraintViolationException
                    || (exception.getCause() != null && exception.getCause().getCause() instanceof ConstraintViolationException);
            assertThat(hasConstraintViolation)
                    .as("Se esperaba ConstraintViolationException debido a @Positive en cantidadInicial")
                    .isTrue();
        });
    }

    @Test
    @DisplayName("Debe rechazar un lote con cantidadDisponible negativa")
    void debeRechazarCantidadDisponibleNegativa() {
        Lote lote = crearLoteValido("LOTE-NEG-DISP");
        lote.setCantidadDisponible(new BigDecimal("-5.000"));

        assertThatThrownBy(() -> {
            loteRepository.saveAndFlush(lote);
        }).satisfies(exception -> {
            boolean hasConstraintViolation = exception instanceof ConstraintViolationException
                    || exception.getCause() instanceof ConstraintViolationException
                    || (exception.getCause() != null && exception.getCause().getCause() instanceof ConstraintViolationException);
            assertThat(hasConstraintViolation)
                    .as("Se esperaba ConstraintViolationException debido a @PositiveOrZero en cantidadDisponible")
                    .isTrue();
        });
    }

    @Test
    @DisplayName("Debe rechazar dos lotes distintos con el mismo código único")
    void debeRechazarCodigoDuplicado() {
        Lote lote1 = crearLoteValido("LOTE-DUP-01");
        loteRepository.saveAndFlush(lote1);

        Lote lote2 = crearLoteValido("LOTE-DUP-01");

        assertThatThrownBy(() -> {
            loteRepository.saveAndFlush(lote2);
        }).satisfies(exception -> {
            assertThat(exception)
                    .as("Se esperaba DataIntegrityViolationException por violación de restricción única en código")
                    .isInstanceOf(DataIntegrityViolationException.class);
        });
    }
}
