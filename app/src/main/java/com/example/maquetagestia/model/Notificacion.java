package com.example.maquetagestia.model;

public class Notificacion {
    private String id;
    private String usuarioId;
    private String titulo;
    private String mensaje;
    private String tipo; // 'Asignacion', 'Vencimiento', 'Completada', 'Sistema'
    private String referenciaId;
    private boolean leido;
    private String fechaGeneracion;

    public Notificacion() {
    }

    public Notificacion(String usuarioId, String titulo, String mensaje, String tipo, String referenciaId) {
        this.usuarioId = usuarioId;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.leido = false;
    }

    public Notificacion(String id, String usuarioId, String titulo, String mensaje,
                        String tipo, String referenciaId, boolean leido, String fechaGeneracion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.leido = leido;
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }

    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }

    public String getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(String fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    @Override
    public String toString() {
        return "Notificacion{" +
                "titulo='" + titulo + '\'' +
                ", tipo='" + tipo + '\'' +
                ", leido=" + leido +
                '}';
    }
}