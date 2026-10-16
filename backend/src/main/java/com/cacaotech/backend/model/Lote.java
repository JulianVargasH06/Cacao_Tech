package com.cacaotech.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lotes", uniqueConstraints = {
    @UniqueConstraint(columnNames = "codigo")
})
public class Lote {

    public enum TipoLote {
        RECEPCION,
        RESULTADO_TRANSFORMACION
    }

    public enum EstadoLote {
        PENDIENTE,
        LIBERADO,
        BLOQUEADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(length = 120)
    private String productor;

    @Column(length = 120)
    private String origen;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Positive(message = "La cantidad inicial debe ser estrictamente mayor a cero")
    @Column(name = "cantidad_inicial_kg", precision = 12, scale = 3, nullable = false)
    private BigDecimal cantidadInicial;

    @PositiveOrZero(message = "La cantidad disponible no puede ser negativa")
    @Column(name = "cantidad_disponible_kg", precision = 12, scale = 3, nullable = false)
    private BigDecimal cantidadDisponible;

    @Column(nullable = false, length = 2)
    private String unidad = "KG";

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_lote", nullable = false, length = 30)
    private TipoLote tipoLote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoLote estado = EstadoLote.PENDIENTE;

    @Version
    private Integer version;

    public Lote() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getProductor() { return productor; }
    public void setProductor(String productor) { this.productor = productor; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public BigDecimal getCantidadInicial() { return cantidadInicial; }
    public void setCantidadInicial(BigDecimal cantidadInicial) { this.cantidadInicial = cantidadInicial; }
    public BigDecimal getCantidadDisponible() { return cantidadDisponible; }
    public void setCantidadDisponible(BigDecimal cantidadDisponible) { this.cantidadDisponible = cantidadDisponible; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public TipoLote getTipoLote() { return tipoLote; }
    public void setTipoLote(TipoLote tipoLote) { this.tipoLote = tipoLote; }
    public EstadoLote getEstado() { return estado; }
    public void setEstado(EstadoLote estado) { this.estado = estado; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}