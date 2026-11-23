package com.example.maquetagestia.model;

public class Subtarea {
    private String id;
    private String tareaId;
    private String titulo;
    private boolean completada;
    private int orden;

    public Subtarea() {
    }

    public Subtarea(String id, String tareaId, String titulo, boolean completada, int orden) {
        this.id = id;
        this.tareaId = tareaId;
        this.titulo = titulo;
        this.completada = completada;
        this.orden = orden;
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

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public boolean isCompletada() {
        return completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }
}
