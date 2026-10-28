package com.example.maquetagestia.model;

public class Usuario {
    private String id;
    private String nombreCompleto;
    private String correo;
    private String passwordHash;
    private String avatarUrl;
    private String telefono;
    private boolean notificacionesActivas;
    private String createdAt;
    private String updatedAt;

    public Usuario() {
    }

    // Constructor para registro nuevo
    public Usuario(String nombreCompleto, String correo, String passwordHash, String telefono) {
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.telefono = telefono;
        this.notificacionesActivas = true;
    }

    // Constructor completo
    public Usuario(String id, String nombreCompleto, String correo, String passwordHash,
                   String avatarUrl, String telefono, boolean notificacionesActivas,
                   String createdAt, String updatedAt) {
        this.id = id;
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.passwordHash = passwordHash;
        this.avatarUrl = avatarUrl;
        this.telefono = telefono;
        this.notificacionesActivas = notificacionesActivas;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public boolean isNotificacionesActivas() { return notificacionesActivas; }
    public void setNotificacionesActivas(boolean notificacionesActivas) { this.notificacionesActivas = notificacionesActivas; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Usuario{" +
                "id='" + id + '\'' +
                ", nombreCompleto='" + nombreCompleto + '\'' +
                ", correo='" + correo + '\'' +
                ", telefono='" + telefono + '\'' +
                '}';
    }
}