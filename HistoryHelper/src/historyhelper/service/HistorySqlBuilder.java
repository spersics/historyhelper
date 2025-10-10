package historyhelper.service;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.util.ArrayList;
import java.util.List;

public class HistorySqlBuilder {
    private final static String EMPTY_SPACE = " \r\n"
            + "	";
    private static final String SYS_USER_TYPE = "varchar";
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
            boolean isOptimizedStorageSelected) throws DBException {
        String histTable = table.getName() + "_hist";
        String justTableName = table.getName();
        DBRProgressMonitor monitor = new VoidProgressMonitor();
        String dbName = table.getDataSource().getInfo().getDatabaseProductName();

        if (!dbName.equalsIgnoreCase("postgresql")) {
            return null;
        }

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
            if (!selectedColumns.contains(colName)) continue;

            columnsDef.add(getSqlColumnForCreateTable(colName, colType, precision, nullable, scale));
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

                columnsDef.add(getSqlColumnForCreateTable(colName, colType, null, false, null));
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
        createSql.append("CREATE TABLE IF NOT EXISTS " + histTable + " \n(" + String.join(",\n", columnsDef));
        if (btnEventColumn) {
            createSql.append(",\nsys_event " + SYS_EVENT_TYPE);
        }
        if (isOptimizedStorageSelected) {
            createSql.append(",\nCONSTRAINT " + histTable + "_pkey ").append("PRIMARY KEY ").append("(" + pkColumnName + ")");
        }
        createSql.append(");");

        StringBuilder function = new StringBuilder();
        StringBuilder triggers = new StringBuilder();

        if (isOptimizedStorageSelected) {
            List<String> excludedColumnNames = columnNames.stream().map(c -> getExcludedColumnValue(c)).toList();
            // insert
            function
                    .append("\nCREATE OR REPLACE FUNCTION ").append("func_" + histTable + "() ").append("RETURNS TRIGGER AS $$")
                    .append("\nBEGIN")
                    .append(EMPTY_SPACE).append("--trigger on insert")
                    .append(EMPTY_SPACE).append("IF TG_OP = 'INSERT' THEN")
                    .append(EMPTY_SPACE).append("INSERT INTO ").append(justTableName + "_hist ")
                    .append("(" + String.join(",", columnNames));
            if (btnEventColumn) {
                function.append(",sys_event");
            }
            function.append(")")
                    .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesNew));
            if (btnEventColumn) {
                function.append(",'INSERT'");
            }
            function.append(");")
                    .append(EMPTY_SPACE).append("RETURN NEW;");

            // update
            function.append("\n")
                    .append(EMPTY_SPACE).append("--trigger on update")
                    .append(EMPTY_SPACE).append("ELSIF TG_OP = 'UPDATE' THEN")
                    .append(EMPTY_SPACE).append("INSERT INTO ").append(justTableName + "_hist ")
                    .append("(" + String.join(",", columnNames));

            if (btnEventColumn) {
                function.append(",sys_event");
            }

            function.append(")")
                    .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesOld));

            if (btnEventColumn) {
                function.append(",'UPDATE'");
            }
            function.append(")")
                    .append(EMPTY_SPACE).append("ON CONFLICT (" + pkColumnName + ") ").append("DO UPDATE")
                    .append(EMPTY_SPACE).append("SET ").append(String.join("," + EMPTY_SPACE, excludedColumnNames));

            if (btnEventColumn) {
                function.append(", \nsys_event = EXCLUDED.sys_event");
            }
            function.append(";")
                    .append(EMPTY_SPACE).append("RETURN OLD;");


            // delete
            function.append("\n")
                    .append(EMPTY_SPACE).append("--trigger on delete")
                    .append(EMPTY_SPACE).append("ELSIF TG_OP = 'DELETE' THEN")
                    .append(EMPTY_SPACE).append("INSERT INTO ").append(justTableName + "_hist ")
                    .append("(" + String.join(",", columnNames));

            if (btnEventColumn) {
                function.append(",sys_event");
            }

            function.append(")")
                    .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesOld));

            if (btnEventColumn) {
                function.append(",'DELETE'");
            }
            function.append(")")
                    .append(EMPTY_SPACE).append("ON CONFLICT (" + pkColumnName + ") ").append("DO UPDATE")
                    .append(EMPTY_SPACE).append("SET ").append(String.join("," + EMPTY_SPACE, excludedColumnNames));

            if (btnEventColumn) {
                function.append(", \nsys_event = EXCLUDED.sys_event");
            }
            function.append(";")
                    .append(EMPTY_SPACE).append("RETURN OLD;");

            function.append("\nEND IF;")
                    .append("\nEND;")
                    .append("\n$$ LANGUAGE plpgsql;");
        } else {
            // insert
            function
                    .append("\nCREATE OR REPLACE FUNCTION ").append("func_" + histTable + "() ").append("RETURNS TRIGGER AS $$")
                    .append("\nBEGIN")
                    .append(EMPTY_SPACE).append("--trigger on insert")
                    .append(EMPTY_SPACE).append("IF TG_OP = 'INSERT' THEN")
                    .append(EMPTY_SPACE).append("INSERT INTO ").append(justTableName + "_hist ")
                    .append("(" + String.join(",", columnNames));
            if (btnEventColumn) {
                function.append(",sys_event");
            }
            function.append(")")
                    .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesNew));
            if (btnEventColumn) {
                function.append(",'INSERT'");
            }
            function.append(");")
                    .append(EMPTY_SPACE).append("RETURN NEW;");

            // update
            function.append("\n")
                    .append(EMPTY_SPACE).append("--trigger on update")
                    .append(EMPTY_SPACE).append("ELSIF TG_OP = 'UPDATE' THEN")
                    .append(EMPTY_SPACE).append("INSERT INTO ").append(justTableName + "_hist ")
                    .append("(" + String.join(",", columnNames));

            if (btnEventColumn) {
                function.append(",sys_event");
            }

            function.append(")")
                    .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesNew));

            if (btnEventColumn) {
                function.append(",'UPDATE'");
            }
            function.append(");")
                    .append(EMPTY_SPACE).append("RETURN NEW;");

            //delete
            function.append("\n")
                    .append(EMPTY_SPACE).append("--trigger on delete")
                    .append(EMPTY_SPACE).append("ELSIF TG_OP = 'DELETE' THEN")
                    .append(EMPTY_SPACE).append("INSERT INTO ").append(justTableName + "_hist ")
                    .append("(" + String.join(",", columnNames));

            if (btnEventColumn) {
                function.append(",sys_event");
            }

            function.append(")")
                    .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesOld));

            if (btnEventColumn) {
                function.append(",'DELETE'");
            }
            function.append(");")
                    .append(EMPTY_SPACE).append("RETURN OLD;");

            function.append("\nEND IF;")
                    .append("\nEND;")
                    .append("\n$$ LANGUAGE plpgsql;");
        }


        triggers.append("\n\n").append("-- triggers");
        if (insert) {
            triggers.append("\nDROP TRIGGER IF EXISTS ").append(histTable + "_ins ").append("ON " + justTableName + ";")
                    .append("\nCREATE TRIGGER ").append(histTable + "_ins")
                    .append("\nAFTER INSERT ON ").append(justTableName)
                    .append("\nFOR EACH ROW EXECUTE FUNCTION " + "func_" + histTable + "();");
        }
        if (update) {
            triggers.append("\n\nDROP TRIGGER IF EXISTS ").append(histTable + "_upd ").append("ON " + justTableName + ";")
                    .append("\nCREATE TRIGGER ").append(histTable + "_upd")
                    .append("\nAFTER UPDATE ON ").append(justTableName)
                    .append("\nFOR EACH ROW EXECUTE FUNCTION " + "func_" + histTable + "();");
        }

        if (delete) {
            triggers.append("\n\nDROP TRIGGER IF EXISTS ").append(histTable + "_del ").append("ON " + justTableName + ";")
                    .append("\nCREATE TRIGGER ").append(histTable + "_del")
                    .append("\nAFTER DELETE ON ").append(justTableName)
                    .append("\nFOR EACH ROW EXECUTE FUNCTION " + "func_" + histTable + "();");
        }
        return createSql + "\n" + function.toString() + "\n" + triggers.toString();
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

    private static String getExcludedColumnValue(String colName) {
        StringBuilder sb = new StringBuilder();
        sb.append(colName + " = " + "EXCLUDED." + colName);
        return sb.toString();
    }


    private static String getSqlColumnForCreateTable(String colName, String colType, Integer precision, Boolean nullable, Integer scale) {
        StringBuilder sb = new StringBuilder();
        String type;
        switch (colType.toUpperCase()) {
            case "varchar":
                type = colType + "(" + precision.toString() + ")";
                break;
            case "VARCHAR2":
                type = colType + "(" + precision.toString() + ")";
                break;
            case "CHAR":
                type = colType + "(" + precision.toString() + ")";
                break;
            case "NUMERIC":
                type = colType + "(" + precision.toString() + scale != null ? "," + scale.toString() : "" + ")";
                break;
            default:
                type = colType;
        }
        sb.append(colName).append(" ").append(type).append(nullable ? " NOT NULL" : "");
        return sb.toString();
    }
}
