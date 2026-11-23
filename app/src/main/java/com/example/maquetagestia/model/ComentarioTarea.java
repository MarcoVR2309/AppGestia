package com.example.maquetagestia.model;

public class ComentarioTarea {
    private String id;
    private String tareaId;
    private String usuarioId;
    private String autorNombre;
    private String contenido;
    private String fechaCreacion;

    public ComentarioTarea() {
    }

    public ComentarioTarea(String id, String tareaId, String usuarioId, String autorNombre,
                           String contenido, String fechaCreacion) {
        this.id = id;
        this.tareaId = tareaId;
        this.usuarioId = usuarioId;
        this.autorNombre = autorNombre;
        this.contenido = contenido;
        this.fechaCreacion = fechaCreacion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTareaId() {
        return tareaId;
    }

    public void setTareaId(String tareaId) {
        this.tareaId = tareaId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public void setAutorNombre(String autorNombre) {
        this.autorNombre = autorNombre;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
