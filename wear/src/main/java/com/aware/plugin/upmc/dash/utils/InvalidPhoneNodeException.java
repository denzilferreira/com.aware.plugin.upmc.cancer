package com.aware.plugin.upmc.dash.utils;

public class InvalidPhoneNodeException extends Exception {
    private String nodeId;
    public InvalidPhoneNodeException(String node) {
        this.nodeId = node;
    }
    public String getNode() {
        return this.nodeId;
    }
}
