package com.example.maquetagestia.model;

public class Tarea {
    private String id;
    private String proyectoId;
    private String faseId; // Opcional (puede ser null)
    private String titulo;
    private String instrucciones;
    private String prioridad; // 'Baja', 'Media', 'Alta', 'Urgente'
    private String estado; // 'Pendiente', 'EnProgreso', 'Finalizada', 'Cancelada'
    private String fechaLimite;
    private String fechaCulminacion;
    private String createdAt;

    public Tarea() {
    }

    // Constructor para creación rápida
    public Tarea(String proyectoId, String faseId, String titulo, String instrucciones,
                 String prioridad, String estado, String fechaLimite) {
        this.proyectoId = proyectoId;
        this.faseId = faseId;
        this.titulo = titulo;
        this.instrucciones = instrucciones;
        this.prioridad = prioridad;
        this.estado = estado;
        this.fechaLimite = fechaLimite;
    }

    // Constructor completo
    public Tarea(String id, String proyectoId, String faseId, String titulo,
                 String instrucciones, String prioridad, String estado,
                 String fechaLimite, String fechaCulminacion, String createdAt) {
        this.id = id;
        this.proyectoId = proyectoId;
        this.faseId = faseId;
        this.titulo = titulo;
        this.instrucciones = instrucciones;
        this.prioridad = prioridad;
        this.estado = estado;
        this.fechaLimite = fechaLimite;
        this.fechaCulminacion = fechaCulminacion;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }

    public String getFaseId() { return faseId; }
    public void setFaseId(String faseId) { this.faseId = faseId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getInstrucciones() { return instrucciones; }
    public void setInstrucciones(String instrucciones) { this.instrucciones = instrucciones; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(String fechaLimite) { this.fechaLimite = fechaLimite; }

    public String getFechaCulminacion() { return fechaCulminacion; }
    public void setFechaCulminacion(String fechaCulminacion) { this.fechaCulminacion = fechaCulminacion; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Tarea{" +
                "id='" + id + '\'' +
                ", titulo='" + titulo + '\'' +
                ", prioridad='" + prioridad + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}