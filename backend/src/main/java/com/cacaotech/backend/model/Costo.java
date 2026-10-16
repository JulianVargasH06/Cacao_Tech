package com.cacaotech.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "costos")
public class Costo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformacion_id", nullable = false)
    private Transformacion transformacion;

    @Column(nullable = false, length = 50)
    private String categoria;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(name = "valor_cop", precision = 12, scale = 2, nullable = false)
    private BigDecimal valorCop;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public Costo() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Transformacion getTransformacion() { return transformacion; }
    public void setTransformacion(Transformacion transformacion) { this.transformacion = transformacion; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public BigDecimal getValorCop() { return valorCop; }
    public void setValorCop(BigDecimal valorCop) { this.valorCop = valorCop; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}