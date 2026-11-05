package com.example.maquetagestia.model;

public class DashboardDTO {
    private int proyectosActivos;
    private int tareasAsignadas;
    private int tareasCompletadas;
    private int tareasPendientes;
    private int porcentajeAvanceGlobal;

    public DashboardDTO() {
    }

    public DashboardDTO(int proyectosActivos, int tareasAsignadas, int tareasCompletadas, int tareasPendientes, int porcentajeAvanceGlobal) {
        this.proyectosActivos = proyectosActivos;
        this.tareasAsignadas = tareasAsignadas;
        this.tareasCompletadas = tareasCompletadas;
        this.tareasPendientes = tareasPendientes;
        this.porcentajeAvanceGlobal = porcentajeAvanceGlobal;
    }

    public int getProyectosActivos() {
        return proyectosActivos;
    }

    public void setProyectosActivos(int proyectosActivos) {
        this.proyectosActivos = proyectosActivos;
    }

    public int getTareasAsignadas() {
        return tareasAsignadas;
    }

    public void setTareasAsignadas(int tareasAsignadas) {
        this.tareasAsignadas = tareasAsignadas;
    }

    public int getTareasCompletadas() {
        return tareasCompletadas;
    }

    public void setTareasCompletadas(int tareasCompletadas) {
        this.tareasCompletadas = tareasCompletadas;
    }

    public int getTareasPendientes() {
        return tareasPendientes;
    }

    public void setTareasPendientes(int tareasPendientes) {
        this.tareasPendientes = tareasPendientes;
    }

    public int getPorcentajeAvanceGlobal() {
        return porcentajeAvanceGlobal;
    }

    public void setPorcentajeAvanceGlobal(int porcentajeAvanceGlobal) {
        this.porcentajeAvanceGlobal = porcentajeAvanceGlobal;
    }
}
