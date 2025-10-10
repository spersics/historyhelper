package historyhelper.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import historyhelper.messages.Messages;

public class SqlDialogWithButtons extends Dialog{
	private String sql;
	private Integer res = -1;
	
	public static final Integer EXECUTE = 0;
	public static final Integer COPY = 1;
	public static final Integer CANCEL = 2;
	
	public SqlDialogWithButtons(Shell parentShell, String sql) {
		super(parentShell);
		this.sql = sql;
	}
	
	@Override
	protected Control createDialogArea (Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));
		
		Text text = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setEditable(false);
		text.setText(sql);
		
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = 800;
		data.heightHint = 500;
		text.setLayoutData(data);
		
		return container;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButton(parent, EXECUTE, Messages.Btn_execute, false);
		super.createButton(parent, COPY, Messages.Btn_copy, false);
		super.createButton(parent, CANCEL, Messages.Btn_cancel, false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		this.res = buttonId;
		super.buttonPressed(buttonId);
		super.close();
	}
	
	public Integer getResult() {
		return res;
	}
	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(Messages.Warn_sql_for);
	}

}
