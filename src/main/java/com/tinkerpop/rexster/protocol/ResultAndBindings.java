package com.tinkerpop.rexster.protocol;


import java.util.List;

public class ResultAndBindings {
    private Object result;
    private List<String> bindings;

    public ResultAndBindings(Object result, List<String> bindings) {
        this.result = result;
        this.bindings = bindings;
    }

    public Object getResult() {
        return result;
    }

    public List<String> getBindings() {
        return bindings;
    }
}
