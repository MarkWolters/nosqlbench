package io.nosqlbench.converters.cql.cqlast;

import java.util.HashMap;
import java.util.Map;

public class CqlTableStats {
    String tableName;

    Map<String,String> attributes = new HashMap<String,String>();

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getAttribute(String attributeName) {
        return attributes.get(attributeName);
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void setAttribute(String attributeName, String attributeVal) {
        attributes.put(attributeName, attributeVal);
    }

}
