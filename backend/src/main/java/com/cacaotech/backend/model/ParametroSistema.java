package com.cacaotech.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parametros_sistema")
public class ParametroSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String clave;

    @Column(nullable = false, length = 255)
    private String valor;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private Usuario actualizadoPor;

    public ParametroSistema() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
    public Usuario getActualizadoPor() { return actualizadoPor; }
    public void setActualizadoPor(Usuario actualizadoPor) { this.actualizadoPor = actualizadoPor; }
}