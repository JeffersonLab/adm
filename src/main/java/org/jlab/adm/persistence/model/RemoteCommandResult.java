package org.jlab.adm.persistence.model;

public class RemoteCommandResult {
    private final String out;
    private final String err;

    public RemoteCommandResult(String out, String err) {
        this.out = out;
        this.err = err;
    }

    public String getOut() {
        return out;
    }

    public String getErr() {
        return err;
    }
}