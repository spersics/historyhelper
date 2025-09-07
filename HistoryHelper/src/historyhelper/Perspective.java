package historyhelper;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class Perspective implements IPerspectiveFactory {

    @Override
    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        layout.addView("historyhelper.views.historyGeneratorView", IPageLayout.LEFT, 0.5f, editorArea);
        layout.setEditorAreaVisible(true);
        layout.setFixed(false);
    }
}
