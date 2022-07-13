package io.nosqlbench.converters.cql.cqlast;

import org.apache.commons.math4.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class CqlSchemaStatsParser {
    private static String KEYSPACE = "Keyspace";
    private static String TABLE = "Table";

    CqlSchemaStats stats = null;
    CqlKeyspaceStats currentKeyspace = null;
    CqlTableStats currentTable = null;

    public CqlSchemaStats parse(Path statspath) throws IOException {
        this.stats = new CqlSchemaStats();
        BufferedReader reader = Files.newBufferedReader(statspath);
        String currentLine = reader.readLine(); //ignore 1st line
        while((currentLine = reader.readLine()) != null) {
            currentLine = currentLine.replaceAll("\t","");
            if (!evalForKeyspace(currentLine)) {
                if (!evalForTable(currentLine)) {
                    String[] splitLine = currentLine.split(":");
                    if (splitLine.length > 1) {
                        Pair<String, String> keyval = new Pair(splitLine[0].trim(), splitLine[1].trim());
                        addAttribute(keyval);
                    }
                }
            }
        }
        writeCurrentTable();
        writeCurrentKeyspace();
        return stats;
    }

    private void addAttribute(Pair<String, String> keyval) {
        if (currentTable != null) {
            currentTable.setAttribute(keyval.getFirst(), keyval.getSecond());
        } else if (currentKeyspace != null) {
            currentKeyspace.setKeyspaceAttribute(keyval.getFirst(), keyval.getSecond());
        } else {
            throw new RuntimeException("Orphaned attribute: " + keyval.toString());
        }
    }

    private boolean evalForTable(String currentLine) {
        if (currentLine.startsWith(TABLE)) {
            writeCurrentTable();
            currentTable = new CqlTableStats();
            currentTable.setTableName(currentLine.split(":")[1].trim());
            return true;
        }
        return false;
    }

    private boolean evalForKeyspace(String currentLine) {
        if (currentLine.startsWith(KEYSPACE)) {
            writeCurrentTable();
            writeCurrentKeyspace();
            currentKeyspace = new CqlKeyspaceStats();
            currentKeyspace.setKeyspaceName(currentLine.split(":")[1].trim());
            currentTable = null;
            return true;
        }
        return false;
    }

    private void writeCurrentKeyspace() {
        if (currentKeyspace != null) {
            stats.setKeyspace(currentKeyspace);
        }
    }

    private void writeCurrentTable() {
        if (currentTable != null) {
            if (currentKeyspace == null) {
                throw new RuntimeException("Table " + currentTable.getTableName() + "has no associated keyspace");
            } else {
                currentKeyspace.setKeyspaceTable(currentTable.getTableName(), currentTable);
            }
        }
    }
}
