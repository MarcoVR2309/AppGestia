package com.example.maquetagestia.model;

public class EnlaceTarea {
    private String id;
    private String tareaId;
    private String usuarioId;
    private String autorNombre;
    private String titulo;
    private String url;
    private String fechaCreacion;

    public EnlaceTarea() {
    }

    public EnlaceTarea(String id, String tareaId, String usuarioId, String autorNombre,
                       String titulo, String url, String fechaCreacion) {
        this.id = id;
        this.tareaId = tareaId;
        this.usuarioId = usuarioId;
        this.autorNombre = autorNombre;
        this.titulo = titulo;
        this.url = url;
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

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
