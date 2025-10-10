package historyhelper.messages;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS{
	private static final String BUNDLE_NAME = "historyhelper.messages.messages";
	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	public static String HistoryDialog_title;
	public static String HistoryDialog_subtitle;
	public static String HistoryDialog_columns_group;
	public static String HistoryDialog_triggers_group;
	public static String HistoryDialog_additional_columns_group;
	public static String HistoryDialog_optimized_storage_group;
	public static String Trigger_on_insert;
	public static String Trigger_on_update;
	public static String Trigger_on_delete;
	public static String Btn_ok;
	public static String Btn_cancel;
	public static String Btn_copy;
	public static String Btn_execute;
	public static String Warn_select_table_in_db_navigator;
	public static String Warn_select_at_least_one_column;
	public static String Warn_select_at_least_one_trigger;
	public static String Warn_sql_gen;
	public static String Warn_sql_copied;
	public static String Warn_sql_executed_for;
	public static String Warn_sql_for;
	public static String Warn;
	public static String Warn_selected_table_ends_with_hist_question;
	public static String Error_plugin_msg_hd;
	public static String AdditionalColumnUser;
	public static String AdditionalColumnDate;
	public static String AdditionalColumnEvent;
	public static String StorageBtnMessage;
	public static String StorageOptimizedBtnMessage;
}
