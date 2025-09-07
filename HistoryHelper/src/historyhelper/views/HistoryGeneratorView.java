package historyhelper.views;

import historyhelper.ui.HistoryDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;

public class HistoryGeneratorView extends ViewPart {

    @Override
    public void createPartControl(Composite parent) {
        Button btn = new Button(parent, SWT.PUSH);
        btn.setText("Configure History");
        btn.addListener(SWT.Selection, e -> openHistoryDialog());
    }

    @Override
    public void setFocus() {
        // TODO Auto-generated method stub
    }

    private void openHistoryDialog() {
        ISelection selection = getSite().getWorkbenchWindow().getSelectionService().getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structured = (IStructuredSelection) selection;
            Object firstElement = structured.getFirstElement();
            if (firstElement instanceof DBSTable) {
                DBSTable selectedTable = (DBSTable) firstElement;
                HistoryDialog dialog = new HistoryDialog(getSite().getShell(), selectedTable, new VoidProgressMonitor());
                dialog.open();
            }
        }
    }
}
