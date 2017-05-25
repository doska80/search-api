package com.vivareal.search.api.model;

public class Healthcheck {
    private boolean status;

    public Healthcheck(boolean status) {
        this.status = status;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
