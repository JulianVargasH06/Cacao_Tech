package com.cacaotech.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "operaciones_procesadas")
public class OperacionProcesada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clave_idempotencia", nullable = false, unique = true, length = 100)
    private String claveIdempotencia;

    @Column(name = "tipo_operacion", nullable = false, length = 50)
    private String tipoOperacion;

    @Column(name = "resultado_referencia", length = 100)
    private String resultadoReferencia;

    @Column(name = "procesada_en", nullable = false)
    private LocalDateTime procesadaEn = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    public OperacionProcesada() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClaveIdempotencia() { return claveIdempotencia; }
    public void setClaveIdempotencia(String claveIdempotencia) { this.claveIdempotencia = claveIdempotencia; }
    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }
    public String getResultadoReferencia() { return resultadoReferencia; }
    public void setResultadoReferencia(String resultadoReferencia) { this.resultadoReferencia = resultadoReferencia; }
    public LocalDateTime getProcesadaEn() { return procesadaEn; }
    public void setProcesadaEn(LocalDateTime procesadaEn) { this.procesadaEn = procesadaEn; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}