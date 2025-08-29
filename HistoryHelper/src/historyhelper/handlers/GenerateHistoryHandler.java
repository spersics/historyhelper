package historyhelper.handlers;

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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityType;

import java.util.List;

public class GenerateHistoryHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);
        try {

            DBSEntity table = getSelectedEntity(HandlerUtil.getCurrentSelection(event));
            if (table == null) {
                MessageDialog.openWarning(shell, "History Helper", "Выберите таблицу в навигаторе БД.");
                return null;
            }

            DBRProgressMonitor monitor = new VoidProgressMonitor();
            List<String> selectedColumns = null;
            boolean onInsert = false;
            boolean onUpdate = false;
            boolean onDelete = false;
            while (true) {
                HistoryDialog dialog = new HistoryDialog(shell, table, monitor);

                if (dialog.open() != Window.OK) {
                    return null;
                }
                if (dialog.getSelectedColumns() == null || dialog.getSelectedColumns().isEmpty()) {
                    MessageDialog.openWarning(shell, "History Helper", "Выберите хотя бы одну колонку для генерации");
                    continue;
                }
                if (!dialog.isOnInsert() && !dialog.isOnUpdate() && !dialog.isOnDelete()) {
                    MessageDialog.openWarning(shell, "History Helper", "Выберите хотя бы один триггер");
                    continue;
                }

                selectedColumns = dialog.getSelectedColumns().stream().map(DBSEntityAttribute::getName).toList();
                onInsert = dialog.isOnInsert();
                onUpdate = dialog.isOnUpdate();
                onDelete = dialog.isOnDelete();
                break;
            }


            String sql;
            try {
                sql = HistorySqlBuilder.buildHistoryTableSql(table, selectedColumns, onInsert, onUpdate, onDelete);
            } catch (Exception e) {
                MessageDialog.openInformation(shell, "History Helper", "Ошибка генерации SQL: " + e.getMessage());
                return null;
            }

            String[] buttons = new String[]{"Применить", "Копировать", "Закрыть"};
            int choice = new MessageDialog(shell, "SQL для " + table.getName(), null, sql, MessageDialog.INFORMATION, buttons, 0).open();
            if (choice == 0) {
                applySql(shell, table, sql);
                Clipboard cb = new Clipboard(shell.getDisplay());
                cb.setContents(new Object[]{sql}, new Transfer[]{TextTransfer.getInstance()});
                cb.dispose();
                MessageDialog.openInformation(shell, "History Helper", "SQL скопирован в буфер обмена.");
            } else if (choice == 1) {
                Clipboard cb = new Clipboard(shell.getDisplay());
                cb.setContents(new Object[]{sql}, new Transfer[]{TextTransfer.getInstance()});
                cb.dispose();
                MessageDialog.openInformation(shell, "History Helper", "SQL скопирован в буфер обмена.");
            }
            return null;
        } catch (Throwable t) {
            MessageDialog.openError(shell, "History Helper - Ошибка", String.valueOf(t));
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
            MessageDialog.openInformation(shell, "History Helper", "SQL применен к " + table.getName());

        } catch (Exception ex) {
            MessageDialog.openError(shell, "History Helper - Ошибка", String.valueOf(ex));
        }
    }

}
