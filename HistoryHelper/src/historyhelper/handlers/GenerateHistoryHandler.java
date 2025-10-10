package historyhelper.handlers;

import historyhelper.dialog.SqlDialogWithButtons;
import historyhelper.messages.Messages;
import historyhelper.service.HistorySqlBuilder;
import historyhelper.ui.HistoryDialog;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.DBSEntityType;

import java.util.Collection;
import java.util.List;

public class GenerateHistoryHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);
        try {
            DBSEntity table = getSelectedEntity(HandlerUtil.getCurrentSelection(event));
            if (table == null) {
                MessageDialog.openWarning(shell, Messages.HistoryDialog_title, Messages.Warn_select_table_in_db_navigator);
                return null;
            }
            if (table.getName().endsWith("_hist") || table.getName().endsWith("_history")) {
                boolean proceed = MessageDialog.openQuestion(shell, Messages.Warn, Messages.Warn_selected_table_ends_with_hist_question);
                if (!proceed) {
                    return null;
                }
            }
            DBRProgressMonitor monitor = new VoidProgressMonitor();
            List<String> selectedColumns = null;
            List<String> additionalColumns = null;
            boolean onInsert = false;
            boolean onUpdate = false;
            boolean onDelete = false;
            boolean isOptimizedStorageSelected = false;

            while (true) {
                HistoryDialog dialog = new HistoryDialog(shell, table, monitor);
                if (dialog.open() != Window.OK) {
                    return null;
                }
                if (dialog.getSelectedColumns() == null || dialog.getSelectedColumns().isEmpty()) {
                    MessageDialog.openWarning(shell, Messages.HistoryDialog_title, Messages.Warn_select_at_least_one_column);
                    continue;
                }
                if (!dialog.isOnInsert() && !dialog.isOnUpdate() && !dialog.isOnDelete()) {
                    MessageDialog.openWarning(shell, Messages.HistoryDialog_title, Messages.Warn_select_at_least_one_trigger);
                    continue;
                }
                selectedColumns = dialog.getSelectedColumns().stream().map(DBSEntityAttribute::getName).toList();
                additionalColumns = dialog.getAdditionalColumns();
                onInsert = dialog.isOnInsert();
                onUpdate = dialog.isOnUpdate();
                onDelete = dialog.isOnDelete();
                isOptimizedStorageSelected = dialog.isOptimizedStorageSelected();
                break;
            }

            String pk = getPkColumn(table, monitor);
            String sql;
            try {
                sql = HistorySqlBuilder.buildHistoryTableSql(pk, table, selectedColumns, additionalColumns, onInsert, onUpdate, onDelete, isOptimizedStorageSelected);
            } catch (Exception e) {
                MessageDialog.openInformation(shell, Messages.HistoryDialog_title, Messages.Warn_sql_gen + e.getMessage());
                return null;
            }

            SqlDialogWithButtons dialog = new SqlDialogWithButtons(shell, sql);
            dialog.open();

            int choice = dialog.getResult();
            if (choice == 0) {
                applySql(shell, table, sql);
                Clipboard cb = new Clipboard(shell.getDisplay());
                cb.setContents(new Object[]{sql}, new Transfer[]{TextTransfer.getInstance()});
                cb.dispose();
                MessageDialog.openInformation(shell, Messages.HistoryDialog_title, Messages.Warn_sql_copied);
            } else if (choice == 1) {
                Clipboard cb = new Clipboard(shell.getDisplay());
                cb.setContents(new Object[]{sql}, new Transfer[]{TextTransfer.getInstance()});
                cb.dispose();
                MessageDialog.openInformation(shell, Messages.HistoryDialog_title, Messages.Warn_sql_copied);
            }

            return null;
        } catch (Throwable t) {
            MessageDialog.openError(shell, Messages.Error_plugin_msg_hd, String.valueOf(t));
            throw new ExecutionException("HistoryHelper failed", t);
        }
    }

    private DBSEntity getSelectedEntity(ISelection sel) {
        if (!(sel instanceof IStructuredSelection)) return null;
        Object first = ((IStructuredSelection) sel).getFirstElement();

        DBNDatabaseNode node = null;
        if (first instanceof DBNDatabaseNode) {
            node = (DBNDatabaseNode) first;
        } else if (first instanceof org.eclipse.core.runtime.IAdaptable) {
            node = (DBNDatabaseNode) ((org.eclipse.core.runtime.IAdaptable) first).getAdapter(DBNDatabaseNode.class);
        }
        if (node == null) return null;

        Object obj = node.getObject();
        if (obj instanceof DBSEntity) {
            DBSEntity ent = (DBSEntity) obj;
            return ent.getEntityType() == DBSEntityType.TABLE ? ent : null;
        }
        return null;
    }

    private void applySql(Shell shell, DBSEntity table, String sql) {
        try {
            DBCExecutionContext ctx = DBUtils.getDefaultContext(table, true);
            try (DBCSession session = ctx.openSession(new VoidProgressMonitor(), DBCExecutionPurpose.USER, "Apply history SQL")) {
                try (DBCStatement statement = session.prepareStatement(DBCStatementType.SCRIPT, sql, false, false, false)) {
                    statement.executeStatement();
                }
            }
            MessageDialog.openInformation(shell, Messages.HistoryDialog_title, Messages.Warn_sql_executed_for + table.getName());
        } catch (Exception ex) {
            MessageDialog.openError(shell, Messages.Error_plugin_msg_hd, String.valueOf(ex));
        }
    }

    private String getPkColumn(DBSEntity entity, DBRProgressMonitor monitor) throws Exception {
        DBSTable table = DBUtils.getAdapter(DBSTable.class, entity);
        if (table == null) return null;

        Collection<? extends DBSTableConstraint> constraints = table.getConstraints(monitor);
        if (constraints == null) return null;

        for (DBSTableConstraint c : constraints) {
            if (c.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                if (c instanceof DBSEntityReferrer) {
                    Collection<? extends DBSEntityAttributeRef> refs = ((DBSEntityReferrer) c).getAttributeReferences(monitor);
                    if (refs != null && !refs.isEmpty()) {
                        return refs.stream().map(ref -> ref.getAttribute().getName())
                                .findFirst().get();
                    }
                }
            }
        }
        return null;
    }
}
