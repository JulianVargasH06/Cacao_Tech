package com.cacaotech.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "salidas_transformacion")
public class SalidaTransformacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformacion_id", nullable = false)
    private Transformacion transformacion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_resultante_id", nullable = false, unique = true)
    private Lote loteResultante;

    @Column(name = "cantidad_kg", precision = 12, scale = 3, nullable = false)
    private BigDecimal cantidadKg;

    @Column(name = "costo_asignado_cop", precision = 12, scale = 2)
    private BigDecimal costoAsignadoCop;

    public SalidaTransformacion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Transformacion getTransformacion() { return transformacion; }
    public void setTransformacion(Transformacion transformacion) { this.transformacion = transformacion; }
    public Lote getLoteResultante() { return loteResultante; }
    public void setLoteResultante(Lote loteResultante) { this.loteResultante = loteResultante; }
    public BigDecimal getCantidadKg() { return cantidadKg; }
    public void setCantidadKg(BigDecimal cantidadKg) { this.cantidadKg = cantidadKg; }
    public BigDecimal getCostoAsignadoCop() { return costoAsignadoCop; }
    public void setCostoAsignadoCop(BigDecimal costoAsignadoCop) { this.costoAsignadoCop = costoAsignadoCop; }
}