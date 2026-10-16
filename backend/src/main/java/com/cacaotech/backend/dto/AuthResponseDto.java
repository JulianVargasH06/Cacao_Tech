package com.cacaotech.backend.dto;

public class AuthResponseDto {
    private String token;
    private String tipo = "Bearer";
    private String correo;
    private String nombre;
    private String rol;

    public AuthResponseDto(String token, String correo, String nombre, String rol) {
        this.token = token;
        this.correo = correo;
        this.nombre = nombre;
        this.rol = rol;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}