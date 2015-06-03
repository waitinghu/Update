package com.seuic.update;

import java.util.ArrayList;

public class UpdateInfo {
    
    private int version;
    private String size;
    private String name;
    private ArrayList<String> description;
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public String getSize() {
        return size;
    }
    
    public void setSize(String size) {
        this.size = size;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public ArrayList<String> getDescription() {
        return description;
    }
    public void setDescription(ArrayList<String> description) {
        this.description = description;
    }
}
