package com.example.maquetagestia.model;

public class Proyecto {
    private String id;
    private String nombre;
    private String descripcion;
    private String tipo; // 'Empresarial' o 'Academico'
    private String estado; // 'Planificacion', 'EnProgreso', 'Pausado', 'Finalizado'
    private String fechaInicio;
    private String fechaFinEstimada;
    private String creadoPor;
    private String createdAt;

    public Proyecto() {
    }

    // Constructor para nuevo proyecto
    public Proyecto(String nombre, String descripcion, String tipo, String estado,
                    String fechaInicio, String fechaFinEstimada, String creadoPor) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFinEstimada = fechaFinEstimada;
        this.creadoPor = creadoPor;
    }

    // Constructor completo
    public Proyecto(String id, String nombre, String descripcion, String tipo,
                    String estado, String fechaInicio, String fechaFinEstimada,
                    String creadoPor, String createdAt) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFinEstimada = fechaFinEstimada;
        this.creadoPor = creadoPor;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFinEstimada() { return fechaFinEstimada; }
    public void setFechaFinEstimada(String fechaFinEstimada) { this.fechaFinEstimada = fechaFinEstimada; }

    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Proyecto{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}