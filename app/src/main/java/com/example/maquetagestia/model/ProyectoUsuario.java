package com.example.maquetagestia.model;

public class ProyectoUsuario {
    private String id;
    private String proyectoId;
    private String usuarioId;
    private String rol; // 'Lider', 'Colaborador', 'Observador'
    private String fechaUnion;

    public ProyectoUsuario() {
    }

    public ProyectoUsuario(String proyectoId, String usuarioId, String rol) {
        this.proyectoId = proyectoId;
        this.usuarioId = usuarioId;
        this.rol = rol;
    }

    public ProyectoUsuario(String id, String proyectoId, String usuarioId, String rol, String fechaUnion) {
        this.id = id;
        this.proyectoId = proyectoId;
        this.usuarioId = usuarioId;
        this.rol = rol;
        this.fechaUnion = fechaUnion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getFechaUnion() { return fechaUnion; }
    public void setFechaUnion(String fechaUnion) { this.fechaUnion = fechaUnion; }

    @Override
    public String toString() {
        return "ProyectoUsuario{" +
                "proyectoId='" + proyectoId + '\'' +
                ", usuarioId='" + usuarioId + '\'' +
                ", rol='" + rol + '\'' +
                '}';
    }
}