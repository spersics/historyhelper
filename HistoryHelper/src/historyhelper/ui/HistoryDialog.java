package historyhelper.ui;


import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.osgi.framework.FrameworkUtil;

import historyhelper.messages.Messages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class HistoryDialog extends TitleAreaDialog {
    private FormToolkit toolkit;
    private final DBSEntity table;
    private final DBRProgressMonitor monitor;
    private static final Set<String> PG_SYS_COLS = Set.of("tableoid", "cmax", "xmax", "cmin", "xmin", "ctid");
    private static final String SYS_USER_COL = "sys_user";
    private static final String SYS_DATE_COL = "sys_action_date";
    private static final String SYS_EVENT_COL = "sys_event";

    private List<DBSEntityAttribute> selectedColumns = new ArrayList<>();
    private List<String> additionalColumns = new ArrayList<>();
    private boolean onInsert;
    private boolean onUpdate;
    private boolean onDelete;
    private boolean isOptimizedStorageSelected;

    private Table columnTable;
    private Button chkInsert, chkUpdate, chkDelete, btnUserColumn, btnDateColumn, btnEventColumn, btnStorage, btnOptimizedStorage;

    public HistoryDialog(Shell parentShell, DBSEntity table, DBRProgressMonitor monitor) {
        super(parentShell);
        this.table = table;
        this.monitor = monitor;
        toolkit = new FormToolkit(parentShell.getDisplay());
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(Messages.HistoryDialog_subtitle);

        Composite area = (Composite) super.createDialogArea(parent);
        area.setLayout(new GridLayout());

        ScrolledComposite sc = new ScrolledComposite(area, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        sc.setExpandHorizontal(true);
        sc.setExpandVertical(true);

        Composite container = new Composite(sc, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        sc.setContent(container);

        new Label(container, SWT.NONE).setText(Messages.HistoryDialog_columns_group);
        columnTable = new Table(container, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL);
        columnTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        try {
            Collection<? extends DBSEntityAttribute> attributes = table.getAttributes(monitor);
            for (DBSEntityAttribute attr : attributes) {
                String name = attr.getName().toLowerCase();
                if (PG_SYS_COLS.contains(name)) continue;

                TableItem item = new TableItem(columnTable, SWT.NONE);
                item.setText(attr.getName() + " (" + attr.getFullTypeName() + ")");
                item.setData(attr);
            }
        } catch (DBException e) {
            e.printStackTrace();
        }

        ExpandableComposite exp = toolkit.createExpandableComposite(container, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
        exp.setText(Messages.HistoryDialog_additional_columns_group);
        exp.setExpanded(false);
        Composite extras = toolkit.createComposite(exp);
        extras.setLayout(new GridLayout(1, false));
        btnUserColumn = new Button(extras, SWT.CHECK);
        btnUserColumn.setText(Messages.AdditionalColumnUser);
        btnDateColumn = new Button(extras, SWT.CHECK);
        btnDateColumn.setText(Messages.AdditionalColumnDate);
        btnEventColumn = new Button(extras, SWT.CHECK);
        btnEventColumn.setText(Messages.AdditionalColumnEvent);
        exp.setClient(extras);
        exp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Group storageGroup = new Group(container, SWT.NONE);
        storageGroup.setText(Messages.HistoryDialog_optimized_storage_group);
        storageGroup.setLayout(new GridLayout(1, false));
        storageGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        btnStorage = new Button(storageGroup, SWT.RADIO);
        btnStorage.setText(Messages.StorageBtnMessage);
        btnOptimizedStorage = new Button(storageGroup, SWT.RADIO);
        btnOptimizedStorage.setText(Messages.StorageOptimizedBtnMessage);
        btnStorage.setSelection(true);

        Group triggersGroup = new Group(container, SWT.NONE);
        triggersGroup.setText(Messages.HistoryDialog_triggers_group);
        triggersGroup.setLayout(new GridLayout(1, false));
        triggersGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        chkInsert = new Button(triggersGroup, SWT.CHECK);
        chkInsert.setText(Messages.Trigger_on_insert);
        chkUpdate = new Button(triggersGroup, SWT.CHECK);
        chkUpdate.setText(Messages.Trigger_on_update);
        chkDelete = new Button(triggersGroup, SWT.CHECK);
        chkDelete.setText(Messages.Trigger_on_delete);

        return area;
    }

    @Override
    protected void okPressed() {
        selectedColumns.clear();
        for (TableItem item : columnTable.getItems()) {
            if (item.getChecked()) {
                selectedColumns.add((DBSEntityAttribute) item.getData());
            }
        }

        if (btnUserColumn.getSelection()) {
            additionalColumns.add(SYS_USER_COL);
        }
        if (btnDateColumn.getSelection()) {
            additionalColumns.add(SYS_DATE_COL);
        }
        if (btnEventColumn.getSelection()) {
            additionalColumns.add(SYS_EVENT_COL);
        }

        onInsert = chkInsert.getSelection();
        onUpdate = chkUpdate.getSelection();
        onDelete = chkDelete.getSelection();
        isOptimizedStorageSelected = btnOptimizedStorage.getSelection();

        super.okPressed();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.HistoryDialog_title);
        var url = FileLocator.find(FrameworkUtil.getBundle(getClass()), new Path("icons/icon.png"), null);
        if (url != null) {
            ImageDescriptor d = ImageDescriptor.createFromURL(url);
            Image dialogIcon = d.createImage();
            newShell.setImage(dialogIcon);
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        ((GridLayout) parent.getLayout()).numColumns++;

        Label by = new Label(parent, SWT.NONE);
        by.setText("created by Artem Belousov @spersics");
        by.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        GridData dg = new GridData(SWT.LEFT, SWT.CENTER, true, false);
        by.setLayoutData(dg);

        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
    }

    public List<DBSEntityAttribute> getSelectedColumns() {
        return selectedColumns;
    }

    public List<String> getAdditionalColumns() {
        return additionalColumns;
    }

    public boolean isOnInsert() {
        return onInsert;
    }

    public boolean isOnUpdate() {
        return onUpdate;
    }

    public boolean isOnDelete() {
        return onDelete;
    }

    public boolean isOptimizedStorageSelected() {
        return isOptimizedStorageSelected;
    }
}
