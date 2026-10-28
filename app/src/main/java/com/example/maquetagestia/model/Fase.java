package com.example.maquetagestia.model;

public class Fase {
    private String id;
    private String proyectoId;
    private String nombre;
    private int orden;
    private String fechaEstimada;
    private String estado; // 'Pendiente', 'EnProgreso', 'Completada'

    public Fase() {
    }

    public Fase(String proyectoId, String nombre, int orden, String fechaEstimada, String estado) {
        this.proyectoId = proyectoId;
        this.nombre = nombre;
        this.orden = orden;
        this.fechaEstimada = fechaEstimada;
        this.estado = estado;
    }

    public Fase(String id, String proyectoId, String nombre, int orden, String fechaEstimada, String estado) {
        this.id = id;
        this.proyectoId = proyectoId;
        this.nombre = nombre;
        this.orden = orden;
        this.fechaEstimada = fechaEstimada;
        this.estado = estado;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public String getFechaEstimada() { return fechaEstimada; }
    public void setFechaEstimada(String fechaEstimada) { this.fechaEstimada = fechaEstimada; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public String toString() {
        return "Fase{" +
                "orden=" + orden +
                ", nombre='" + nombre + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}