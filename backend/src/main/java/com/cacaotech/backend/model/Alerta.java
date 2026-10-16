package com.cacaotech.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
public class Alerta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String severidad;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformacion_id")
    private Transformacion transformacion;

    @Column(nullable = false, length = 20)
    private String estado = "ACTIVA";

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    @Column(name = "resuelta_en")
    private LocalDateTime resueltaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelta_por")
    private Usuario resueltaPor;

    public Alerta() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getSeveridad() { return severidad; }
    public void setSeveridad(String severidad) { this.severidad = severidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }
    public Transformacion getTransformacion() { return transformacion; }
    public void setTransformacion(Transformacion transformacion) { this.transformacion = transformacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
    public LocalDateTime getResueltaEn() { return resueltaEn; }
    public void setResueltaEn(LocalDateTime resueltaEn) { this.resueltaEn = resueltaEn; }
    public Usuario getResueltaPor() { return resueltaPor; }
    public void setResueltaPor(Usuario resueltaPor) { this.resueltaPor = resueltaPor; }
}