package com.appointech.appointech_backend.domain.models;

public class EspecialidadAdminInfo {
    private Especialidad especialidad;
    private long tecnicosAsignados;

    public EspecialidadAdminInfo() {
    }

    public EspecialidadAdminInfo(Especialidad especialidad, long tecnicosAsignados) {
        this.especialidad = especialidad;
        this.tecnicosAsignados = tecnicosAsignados;
    }

    public Especialidad getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(Especialidad especialidad) {
        this.especialidad = especialidad;
    }

    public long getTecnicosAsignados() {
        return tecnicosAsignados;
    }

    public void setTecnicosAsignados(long tecnicosAsignados) {
        this.tecnicosAsignados = tecnicosAsignados;
    }
}