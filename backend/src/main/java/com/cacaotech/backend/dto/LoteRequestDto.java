package com.cacaotech.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public class LoteRequestDto {

    @NotBlank(message = "El código del lote es obligatorio")
    private String codigo;

    private String productor;
    
    private String origen;

    @NotNull(message = "La fecha de registro es obligatoria")
    private LocalDate fechaRegistro;

    @NotNull(message = "La cantidad inicial es obligatoria")
    @Positive(message = "La cantidad debe ser estrictamente mayor a cero")
    private BigDecimal cantidadInicial;

    private String unidad = "KG";

    public LoteRequestDto() {}

    public LoteRequestDto(String codigo, String productor, String origen, LocalDate fechaRegistro, BigDecimal cantidadInicial, String unidad) {
        this.codigo = codigo;
        this.productor = productor;
        this.origen = origen;
        this.fechaRegistro = fechaRegistro;
        this.cantidadInicial = cantidadInicial;
        this.unidad = unidad;
    }

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
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
}