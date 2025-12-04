package historyhelper.service;

import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

public class HistorySqlBuilderMySql {
    private final static String EMPTY_SPACE = " \r\n"
            + "	";
    private static final String SYS_USER_TYPE = "varchar(255)";
    private static final String SYS_DATE_TYPE = "timestamp";
    private static final String SYS_EVENT_TYPE = "varchar(6)";

    public static String buildHistoryTableSql(
            String pkColumnName,
            DBSEntity table,
            List<String> selectedColumns,
            List<String> additionalColumns,
            boolean insert,
            boolean update,
            boolean delete,
            boolean isOptimizedStorageSelected, String dbName) throws DBException {
        String schemeName = dbName + ".";
        String histTable = table.getName() + "_hist";
        String justTableName = table.getName();
        DBRProgressMonitor monitor = new VoidProgressMonitor();

        List<String> columnsDef = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        List<String> columnValuesOld = new ArrayList<>();
        List<String> columnValuesNew = new ArrayList<>();
        List<String> columnOptimizedNames = new ArrayList<>();

        boolean btnEventColumn = false;

        for (DBSEntityAttribute column : table.getAttributes(monitor)) {
            String colName = column.getName();
            String colType = column.getTypeName();
            Integer precision = column.getPrecision();
            Integer scale = column.getScale();
            Boolean nullable = column.isRequired();
            Long length = column.getMaxLength();
            if (!selectedColumns.contains(colName)) continue;

            columnsDef.add(getSqlColumnForCreateTable(colName, colType, precision, nullable, scale, length));
            columnNames.add(colName);
            columnValuesOld.add("OLD." + colName);
            columnValuesNew.add("NEW." + colName);
            columnOptimizedNames.add(colName + " = " + "EXCLUDED." + colName);
        }
        if (!additionalColumns.isEmpty()) {
            for (String column : additionalColumns) {
                if (column.equalsIgnoreCase("sys_event")) {
                    btnEventColumn = true;
                    continue;
                }
                String colName = column;
                String colType = getAdditionalColumnType(colName);

                columnsDef.add(getSqlColumnForCreateTable(colName, colType, null, false, null, null));
                columnNames.add(colName);
                if (colName.equals("sys_user")) {
                    columnValuesOld.add("current_user");
                    columnValuesNew.add("current_user");
                }
                if (colName.equals("sys_action_date")) {
                    columnValuesOld.add("current_timestamp");
                    columnValuesNew.add("current_timestamp");
                }
            }
        }
        StringBuilder createSql = new StringBuilder();
        createSql
                .append("DROP TABLE IF EXISTS ").append(schemeName + histTable + ";")
                .append("\n\nCREATE TABLE IF NOT EXISTS " + schemeName + histTable + " \n(" + String.join(",\n", columnsDef));
        if (btnEventColumn) {
            createSql.append(",\nsys_event " + SYS_EVENT_TYPE);
        }
        if (isOptimizedStorageSelected) {
            createSql.append(",\nCONSTRAINT " + histTable + "_pkey ").append("PRIMARY KEY ").append("(" + pkColumnName + ")");
        }
        createSql.append(") ENGINE=InnoDB;");


        StringBuilder triggers = new StringBuilder();

        if (isOptimizedStorageSelected) {
            List<String> excludedColumnNames = columnNames.stream().map(c -> getExcludedColumnValue(c)).toList();

            if (insert) {
                triggers
                        .append("\nDROP TRIGGER IF EXISTS ").append(schemeName + "trg_" + histTable + "_ins;")
                        .append("\n\nCREATE TRIGGER ").append(schemeName + "trg_" + histTable + "_ins")
                        .append("\nAFTER INSERT ON ").append(schemeName + justTableName)
                        .append("\nFOR EACH ROW")
                        .append(EMPTY_SPACE).append("INSERT INTO ").append(schemeName + histTable)
                        .append("(" + String.join(",", columnNames));
                if (btnEventColumn) {
                    triggers.append(",sys_event");
                }
                triggers.append(")")
                        .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesNew));
                if (btnEventColumn) {
                    triggers.append(",'INSERT'");
                }
                triggers.append(")")
                        .append("\nON DUPLICATE KEY UPDATE")
                        .append("\n").append(String.join("," + EMPTY_SPACE, excludedColumnNames));
                if (btnEventColumn) {
                    triggers.append(", \nsys_event = VALUES(sys_event)");
                }
                triggers.append(";");

            }
            if (update) {
                triggers
                        .append("\n\nDROP TRIGGER IF EXISTS ").append(schemeName + "trg_" + histTable + "_upd;")
                        .append("\n\nCREATE TRIGGER ").append(schemeName + "trg_" + histTable + "_upd")
                        .append("\nAFTER UPDATE ON ").append(schemeName + justTableName)
                        .append("\nFOR EACH ROW")
                        .append(EMPTY_SPACE).append("INSERT INTO ").append(schemeName + histTable)
                        .append("(" + String.join(",", columnNames));
                if (btnEventColumn) {
                    triggers.append(",sys_event");
                }
                triggers.append(")")
                        .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesOld));
                if (btnEventColumn) {
                    triggers.append(",'UPDATE'");
                }
                triggers.append(")")
                        .append("\nON DUPLICATE KEY UPDATE")
                        .append("\n").append(String.join("," + EMPTY_SPACE, excludedColumnNames));
                if (btnEventColumn) {
                    triggers.append(", \nsys_event = VALUES(sys_event)");
                }
                triggers.append(";");
            }
            if (delete) {
                triggers
                        .append("\n\nDROP TRIGGER IF EXISTS ").append(schemeName + "trg_" + histTable + "_del;")
                        .append("\n\nCREATE TRIGGER ").append(schemeName + "trg_" + histTable + "_del")
                        .append("\nAFTER DELETE ON ").append(schemeName + justTableName)
                        .append("\nFOR EACH ROW")
                        .append(EMPTY_SPACE).append("INSERT INTO ").append(schemeName + histTable)
                        .append("(" + String.join(",", columnNames));
                if (btnEventColumn) {
                    triggers.append(",sys_event");
                }
                triggers.append(")")
                        .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesOld));
                if (btnEventColumn) {
                    triggers.append(",'DELETE'");
                }
                triggers.append(")")
                        .append("\nON DUPLICATE KEY UPDATE")
                        .append("\n").append(String.join("," + EMPTY_SPACE, excludedColumnNames));
                if (btnEventColumn) {
                    triggers.append(", \nsys_event = VALUES(sys_event)");
                }
                triggers.append(";");
            }
        } else {
            if (insert) {
                triggers
                        .append("\nDROP TRIGGER IF EXISTS ").append(schemeName + "trg_" + histTable + "_ins;")
                        .append("\n\nCREATE TRIGGER ").append(schemeName + "trg_" + histTable + "_ins")
                        .append("\nAFTER INSERT ON ").append(schemeName + justTableName)
                        .append("\nFOR EACH ROW")
                        .append(EMPTY_SPACE).append("INSERT INTO ").append(schemeName + histTable)
                        .append("(" + String.join(",", columnNames));
                if (btnEventColumn) {
                    triggers.append(",sys_event");
                }
                triggers.append(")")
                        .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesNew));
                if (btnEventColumn) {
                    triggers.append(",'INSERT'");
                }
                triggers.append(");");

            }
            if (update) {
                triggers
                        .append("\n\nDROP TRIGGER IF EXISTS ").append(schemeName + "trg_" + histTable + "_upd;")
                        .append("\n\nCREATE TRIGGER ").append(schemeName + "trg_" + histTable + "_upd")
                        .append("\nAFTER UPDATE ON ").append(schemeName + justTableName)
                        .append("\nFOR EACH ROW")
                        .append(EMPTY_SPACE).append("INSERT INTO ").append(schemeName + histTable)
                        .append("(" + String.join(",", columnNames));
                if (btnEventColumn) {
                    triggers.append(",sys_event");
                }
                triggers.append(")")
                        .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesNew));
                if (btnEventColumn) {
                    triggers.append(",'UPDATE'");
                }
                triggers.append(");");
            }
            if (delete) {
                triggers
                        .append("\n\nDROP TRIGGER IF EXISTS ").append(schemeName + "trg_" + histTable + "_del;")
                        .append("\n\nCREATE TRIGGER ").append(schemeName + "trg_" + histTable + "_del")
                        .append("\nAFTER DELETE ON ").append(schemeName + justTableName)
                        .append("\nFOR EACH ROW")
                        .append(EMPTY_SPACE).append("INSERT INTO ").append(schemeName + histTable)
                        .append("(" + String.join(",", columnNames));
                if (btnEventColumn) {
                    triggers.append(",sys_event");
                }
                triggers.append(")")
                        .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesOld));
                if (btnEventColumn) {
                    triggers.append(",'DELETE'");
                }
                triggers.append(");");
            }
        }
        return createSql + "\n" + triggers.toString();
    }


    private static String getExcludedColumnValue(String colName) {
        StringBuilder sb = new StringBuilder();
        sb.append(colName + " = " + "VALUES(" + colName + ")");
        return sb.toString();
    }

    private static String getAdditionalColumnType(String columnName) {
        String type;
        switch (columnName) {
            case "sys_user":
                type = SYS_USER_TYPE;
                break;
            case "sys_action_date":
                type = SYS_DATE_TYPE;
                break;
            case "sys_event":
                type = SYS_EVENT_TYPE;
                break;
            default:
                type = null;
        }
        return type;
    }

    private static String getSqlColumnForCreateTable(String colName, String colType, Integer precision, Boolean nullable, Integer scale, Long length) {
        StringBuilder sb = new StringBuilder();
        String type;
        switch (colType.toUpperCase()) {
            case "VARCHAR", "CHAR", "BINARY", "VARBINARY":
                if (length != null && length > 0) {
                    type = colType + "(" + length.toString() + ")";
                    break;
                }
                type = colType;
                break;
            case "DECIMAL", "NUMERIC":
                if (precision != null) {
                    if (scale != null) {
                        type = colType + "(" + precision.toString() + "," + scale.toString() + ")";
                        break;
                    }
                    type = colType + "(" + precision.toString() + ")";
                    break;
                }
                type = colType;
                break;
            case "DATETIME", "TIMESTAMP", "TIME":
                if (precision != null) {
                    type = colType + "(" + precision.toString() + ")";
                    break;
                }
                type = colType;
                break;
            default:
                type = colType;
        }
        sb.append(colName).append(" ").append(type).append(nullable ? " NOT NULL" : "");
        return sb.toString();
    }
}
