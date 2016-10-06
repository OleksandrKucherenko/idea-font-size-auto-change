package fontsize;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiDocumentManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * <h3>FontSizeAutoChange</h3>
 */
public class FontSizeAutoChange implements ApplicationComponent {
    private static final Logger LOG = Logger.getInstance(FontSizeAutoChange.class.getName());


    public static final String RESOLUTION_PREFIX = "resolution.";
    public static final String FONTSIZE_SUFFIX = "FontSize";

    /** Resolution - to - screen Name. Example: resolution.2560x1440=dell */
    protected final Map<String, String> resolutions = new HashMap<>();
    /** Screen Name - to - font size. Example: dellFontSize=14 */
    protected final Map<String, Integer> lookupFontSize = new HashMap<>();
    protected int lastWidth = -1;
    protected int lastHeight = -1;
    protected Timer t;
    protected int retinaWidth;
    protected int retinaHeight;
    protected int retinaFontSize;
    protected int normalFontSize;

    public FontSizeAutoChange() throws IOException {
        super();

        Properties p = new Properties();
        p.load(getClass().getResourceAsStream("fontsize.properties"));

        retinaHeight = Integer.valueOf(p.getProperty("retinaHeight"));
        retinaWidth = Integer.valueOf(p.getProperty("retinaWidth"));
        retinaFontSize = Integer.valueOf(p.getProperty("retinaFontSize"));
        normalFontSize = Integer.valueOf(p.getProperty("normalFontSize"));

        // register pre-defined resolution from v1
        resolutions.put( retinaWidth + "x" + retinaHeight, "retina" );
        lookupFontSize.put("normal", normalFontSize);
        lookupFontSize.put("retina", retinaFontSize);

        // parse: resolution.{width}x{height}={screen-name}
        final Enumeration<Object> keys = p.keys();
        while (keys.hasMoreElements()) {
            final String key = ((String) keys.nextElement());
            LOG.info("found key: " + key);

            if (key.startsWith(RESOLUTION_PREFIX)) {
                final String resolution = key.substring(RESOLUTION_PREFIX.length());
                final String screenName = p.getProperty(key);

                resolutions.put(resolution.toLowerCase(Locale.US), screenName);
                LOG.info("found screenName: " + screenName);

                // parse: {screen-name}FontSize={fontSize}
                if (!lookupFontSize.containsKey(screenName)) {
                    final String keyScreenFontSize = screenName + FONTSIZE_SUFFIX;
                    final Integer fontSize = Integer.valueOf(p.getProperty(keyScreenFontSize, "" + normalFontSize));

                    lookupFontSize.put(screenName, fontSize);
                    LOG.info("found screen: '" + screenName + "', font size: " + fontSize);
                }
            }
        }

    }

    public void initComponent() {

        t = new Timer(5000, e -> {

            final IdeFrame frame = WindowManager.getInstance().getIdeFrame(null);
            // width and height of bounds will match the screen resolution (screen that used for displaying the IDE)
            final Rectangle bounds = (null != frame) ? frame.getComponent().getGraphicsConfiguration().getBounds() : new Rectangle(0,0,0,0);

            if (bounds.getWidth() == lastWidth && bounds.getHeight() == lastHeight) {
                return;
            }
            lastWidth = (int)bounds.getWidth();
            lastHeight = (int)bounds.getHeight();

            final String resolution = lastWidth + "x" + lastHeight;
            final String screenName = resolutions.containsKey(resolution) ? resolutions.get(resolution) : "normal";
            final int newFontSize = lookupFontSize.containsKey(screenName) ? lookupFontSize.get(screenName) : normalFontSize;

            UISettings.getInstance().FONT_SIZE = newFontSize;
            UISettings.getInstance().OVERRIDE_NONIDEA_LAF_FONTS = true;

            EditorColorsManager.getInstance().getGlobalScheme().setEditorFontSize(newFontSize);
            EditorColorsManager.getInstance().getGlobalScheme().setConsoleFontSize(newFontSize);

            EditorColorsScheme oldScheme = EditorColorsManager.getInstance().getGlobalScheme();

            //set different scheme to apply settings
            for (EditorColorsScheme otherScheme : EditorColorsManager.getInstance().getAllSchemes()) {
                if (otherScheme != oldScheme) {
                    EditorColorsManager.getInstance().setGlobalScheme(otherScheme);
                    break;
                }
            }
            EditorColorsManager.getInstance().setGlobalScheme(oldScheme);
        });

        t.setDelay(5000);
        t.setInitialDelay(5000);
        t.setRepeats(true);
        t.start();
    }

    @Override
    public void disposeComponent() {
        if (t != null)
            t.stop();
        t = null;
    }

    public String getComponentName() {
        return "FontSizeAutoChange";
    }

}

