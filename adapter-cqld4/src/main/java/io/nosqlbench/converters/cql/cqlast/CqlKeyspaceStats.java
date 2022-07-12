package io.nosqlbench.converters.cql.cqlast;

import java.util.HashMap;
import java.util.Map;

public class CqlKeyspaceStats {
    String keyspaceName;

    Map<String,String> keyspaceAttributes = new HashMap<String,String>();

    Map<String, CqlTableStats> keyspaceTables = new HashMap<String, CqlTableStats>();
    public String getKeyspaceName() {
        return keyspaceName;
    }

    public void setKeyspaceName(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    public Map<String, String> getKeyspaceAttributes() {
        return keyspaceAttributes;
    }

    public String getKeyspaceAttribute(String attributeName) {
        return keyspaceAttributes.get(attributeName);
    }

    public void setKeyspaceAttributes(Map<String, String> keyspaceAttributes) {
        this.keyspaceAttributes = keyspaceAttributes;
    }

    public void setKeyspaceAttribute(String attributeName, String attributeVal) {
        this.keyspaceAttributes.put(attributeName, attributeVal);
    }

    public Map<String, CqlTableStats> getKeyspaceTables() {
        return keyspaceTables;
    }

    public CqlTableStats getKeyspaceTable(String tableName) {
        return keyspaceTables.get(tableName);
    }

    public void setKeyspaceTables(Map<String, CqlTableStats> keyspaceTables) {
        this.keyspaceTables = keyspaceTables;
    }

    public void setKeyspaceTable(String tableName, CqlTableStats tableAttributes) {
        this.keyspaceTables.put(tableName, tableAttributes);
    }


}
