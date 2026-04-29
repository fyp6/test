package com.mycompany.project.model;

public class ClinicBean {
    private long id;
    private String name;
    private String location;
    private boolean walkInEnabled;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isWalkInEnabled() {
        return walkInEnabled;
    }

    public void setWalkInEnabled(boolean walkInEnabled) {
        this.walkInEnabled = walkInEnabled;
    }
}
