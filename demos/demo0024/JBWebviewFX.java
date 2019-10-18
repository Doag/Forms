
import java.awt.Dimension;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import oracle.forms.handler.IHandler;
import oracle.forms.properties.ID;
import oracle.forms.ui.VBean;

import oracle.forms.ui.CustomEvent;

public class JBWebviewFX extends VBean {

    public final static ID SET_CONTENT = ID.registerProperty("SET_CONTENT");
    public final static ID EXEC_JS = ID.registerProperty("EXEC_JS");
    public final static ID LINK_PRESS = ID.registerProperty("LINK_PRESS");
    public static callBack mc;
    private static final int JFXPANEL_WIDTH_INT = 300;
    private static final int JFXPANEL_HEIGHT_INT = 250;
    private static JFXPanel fxContainer;
    private IHandler m_handler;
    String content;
    WebView webView;

    public JBWebviewFX() {
        super();
    }

    @Override
    public void init(IHandler handler) {

        System.out.println("init");
        m_handler = handler;
        super.init(handler);

        fxContainer = new JFXPanel();
        fxContainer.setPreferredSize(new Dimension(JFXPANEL_WIDTH_INT, JFXPANEL_HEIGHT_INT));

        // create JavaFX scene
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                createScene();
                add(fxContainer);
            }

        });

    }

    @Override
    public boolean setProperty(ID property, Object value) {
        System.out.println("set");
        if (property == SET_CONTENT) {
            content = (String) value;

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    webView.getEngine().loadContent(content, "text/html");
                }

            });
        }

        if (property == EXEC_JS) {
            content = (String) value;

            Platform.runLater(new Runnable() {
                @Override
                public void run() {

                    webView.getEngine().executeScript(content);
                }

            });
        }

        return super.setProperty(property, value);
    }

    @Override
    public Object getProperty(ID property) {
        System.out.println("get");
        return super.getProperty(property);
    }

    private void createScene() {
        webView = new WebView();
        final JBWebviewFX ml = this;

        webView.getEngine().getLoadWorker().stateProperty().addListener(
                new ChangeListener() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {

                if (newValue != Worker.State.SUCCEEDED) {
                    return;
                }

                JSObject window = (JSObject) webView.getEngine().executeScript("window");
                mc = new callBack();
                mc.mobj = ml;
                window.setMember("myCallBack", mc);
            }

        }
        );

        VBox vBox = new VBox(webView);

        fxContainer.setScene(new Scene(vBox, 960, 600));

    }

    public class callBack {

        JBWebviewFX mobj;

        public void mf1(String param1) {

            CustomEvent ce = new CustomEvent(mobj.getHandler(), "LINK_PRESS");
            ce.setProperty(LINK_PRESS, param1);
            mobj.dispatchCustomEvent(ce);
        }
    }

}
