
import javax.swing.*;
 
import java.awt.event.*;

public class TimerLabel extends JLabel implements ActionListener {

    private int hrs;
    private int mins;
    private int secs;
    private int hsecs;

    private String nums[];

    private boolean running;
    private boolean update;

    private Timer alarm;

    public TimerLabel() {
        super("00:00:00.00", JLabel.CENTER);

        nums = new String[100];
        for (int i = 0; i < 100; i++) {
            nums[i] = Integer.toString(i);

            if (i < 10) {
                nums[i] = "0" + nums[i];
            }
        }

        alarm = new Timer(10, this);
        alarm.start();

        timerReset();
    }

    public void actionPerformed(ActionEvent ev) {

        if (running) {
            hsecs++;

            if (hsecs == 100) {
                hsecs = 0;
                secs++;

                if (secs == 60) {
                    secs = 0;
                    mins++;

                    if (mins == 60) {
                        mins = 0;
                        hrs = (hrs + 1) % 100;
                    }
                }
            }

            if (update) {
                updateText();
            }
        }

        alarm.restart();
    }

    public void timerReset() {

        alarm.stop();

        hrs = mins = secs = hsecs = 0;
        updateText();

        running = false;
        update = true;

        alarm.restart();
    }

    public void timerStart() {
        running = true;
        update = true;
    }

    public void timerStop() {
        running = false;
        update = true;
    }

    public void timerNoUpdate() {
        update = false;
    }

    public void updateText() {
        setText(nums[hrs] + ":"
                + nums[mins] + ":"
                + nums[secs] + "."
                + nums[hsecs]);
    }

}
