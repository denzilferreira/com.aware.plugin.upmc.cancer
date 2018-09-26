package com.aware.plugin.upmc.dash.utils;

public class InvalidWearNodeException extends Exception {
    private String nodeId;
    public InvalidWearNodeException(String node) {
        this.nodeId = node;
    }
    public String getNode() {
        return this.nodeId;
    }
}
