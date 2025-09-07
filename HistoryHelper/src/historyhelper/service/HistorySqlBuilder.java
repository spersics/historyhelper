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

    public static String buildHistoryTableSql(DBSEntity table, List<String> selectedColumns, boolean insert, boolean update, boolean delete) throws DBException {
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

        String createSql = "CREATE TABLE IF NOT EXISTS " + histTable + " \n(" + String.join(",\n", columnsDef) + ");";

        StringBuilder triggers = new StringBuilder();


        if (insert) {
            triggers.append("\n--trigger on insert")
                    .append("\nCREATE OR REPLACE FUNCTION ").append(justTableName + "_ins()")
                    .append("\nRETURNS TRIGGER AS $$")
                    .append("\nBEGIN ").append(EMPTY_SPACE).append("INSERT INTO ").append(justTableName + "_hist ")
                    .append("(" + String.join(",", columnNames) + ")")
                    .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesNew) + ");")
                    .append(EMPTY_SPACE).append("RETURN NEW;")
                    .append("\nEND;")
                    .append("\n$$ LANGUAGE PLPGSQL;")
                    .append("\n\nCREATE OR REPLACE TRIGGER ").append(justTableName + "_hist_ins")
                    .append("\nAFTER INSERT ON ").append(justTableName)
                    .append("\nFOR EACH ROW")
                    .append("\nEXECUTE FUNCTION ").append(justTableName + "_ins();");
        }
        if (update) {
            triggers.append("\n\n--trigger on update")
                    .append("\nCREATE OR REPLACE FUNCTION ").append(justTableName + "_upd()")
                    .append("\nRETURNS TRIGGER AS $$")
                    .append("\nBEGIN ").append(EMPTY_SPACE).append("INSERT INTO ").append(justTableName + "_hist ")
                    .append("(" + String.join(",", columnNames) + ")")
                    .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesNew) + ");")
                    .append(EMPTY_SPACE).append("RETURN NEW;")
                    .append("\nEND;")
                    .append("\n$$ LANGUAGE PLPGSQL;")
                    .append("\n\nCREATE OR REPLACE TRIGGER ").append(justTableName + "_hist_upd")
                    .append("\nAFTER UPDATE ON ").append(justTableName)
                    .append("\nFOR EACH ROW")
                    .append("\nEXECUTE FUNCTION ").append(justTableName + "_upd();");
        }

        if (delete) {
            triggers.append("\n\n--trigger on delete")
                    .append("\nCREATE OR REPLACE FUNCTION ").append(justTableName + "_del()")
                    .append("\nRETURNS TRIGGER AS $$")
                    .append("\nBEGIN ").append(EMPTY_SPACE).append("INSERT INTO ").append(justTableName + "_hist ")
                    .append("(" + String.join(",", columnNames) + ")")
                    .append(EMPTY_SPACE).append("VALUES ").append("(" + String.join(",", columnValuesOld) + ");")
                    .append(EMPTY_SPACE).append("RETURN OLD;")
                    .append("\nEND;")
                    .append("\n$$ LANGUAGE PLPGSQL;")
                    .append("\n\nCREATE OR REPLACE TRIGGER ").append(justTableName + "_hist_del")
                    .append("\nAFTER DELETE ON ").append(justTableName)
                    .append("\nFOR EACH ROW")
                    .append("\nEXECUTE FUNCTION ").append(justTableName + "_del();");

        }
        return createSql + "\n" + triggers.toString();
    }


    private static String getSqlColumnForCreateTable(String colName, String colType, Integer precision, Boolean nullable, Integer scale) {
        StringBuilder sb = new StringBuilder();
        String type;
        switch (colType.toUpperCase()) {
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
