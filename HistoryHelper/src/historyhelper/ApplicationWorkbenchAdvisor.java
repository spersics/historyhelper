package historyhelper;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

    private static final String PERSPECTIVE_ID = "HistoryHelper.perspective";

    @Override
    public String getInitialWindowPerspectiveId() {
        return PERSPECTIVE_ID;
    }

    @Override
    public void preWindowOpen(IWorkbenchWindowConfigurer configurer) {
        configurer.setInitialSize(new Point(800, 600));
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setTitle("HistHelper Plugin");
    }
}
