
import oracle.forms.handler.IHandler;
import oracle.forms.properties.ID;
import oracle.forms.ui.VBean;
import java.awt.*;

public class Stopwatch extends VBean {

    public final static ID START = ID.registerProperty("START");
    public final static ID STOP = ID.registerProperty("STOP");
    public final static ID RESET = ID.registerProperty("RESET");
    
    private TimerLabel theTime;

    public Stopwatch() {
    }

    @Override
    public void init(IHandler handler) {
        System.out.println("init_stopwatch");
        
        super.init(handler);

        theTime = new TimerLabel();
        theTime.setFont(new Font("Courier", Font.BOLD, 25));
        this.add(theTime);
    }

    @Override
    public boolean setProperty(ID property, Object value) {
        System.out.println("set");

        if (property == START) {
            stopTheTimer();
        } else if (property == STOP) {
            startTheTimer();
        } else if (property == RESET) {
            resetTheTimer();
        }
        return super.setProperty(property, value);
    }

    @Override
    public Object getProperty(ID property) {
        System.out.println("get");

        return super.getProperty(property);
    }

    private void startTheTimer() {
        theTime.timerStart();
    }

    private void resetTheTimer() {
        theTime.timerReset();
    }

    private void stopTheTimer() {
        theTime.timerStop();
        theTime.updateText();
    }

    private void pauseTheTimer() {
        theTime.timerNoUpdate();
    }

}
