package io.nosqlbench.converters.cql.cqlast;

import java.util.HashMap;
import java.util.Map;

public class CqlSchemaStats {
    Map<String, CqlKeyspaceStats> keyspaces = new HashMap<String, CqlKeyspaceStats>();

    public Map<String, CqlKeyspaceStats> getKeyspaces() {
        return keyspaces;
    }

    public void setKeyspaces(Map<String, CqlKeyspaceStats> keyspaces) {
        this.keyspaces = keyspaces;
    }

    public CqlKeyspaceStats getKeyspace(String keyspaceName) {
        return keyspaces.get(keyspaceName);
    }

    public void setKeyspace(CqlKeyspaceStats keyspace) {
        this.keyspaces.put(keyspace.getKeyspaceName(), keyspace);
    }

}
