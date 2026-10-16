package com.cacaotech.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "entradas_transformacion")
public class EntradaTransformacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformacion_id", nullable = false)
    private Transformacion transformacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @Column(name = "cantidad_kg", precision = 12, scale = 3, nullable = false)
    private BigDecimal cantidadKg;

    public EntradaTransformacion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Transformacion getTransformacion() { return transformacion; }
    public void setTransformacion(Transformacion transformacion) { this.transformacion = transformacion; }
    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }
    public BigDecimal getCantidadKg() { return cantidadKg; }
    public void setCantidadKg(BigDecimal cantidadKg) { this.cantidadKg = cantidadKg; }
}