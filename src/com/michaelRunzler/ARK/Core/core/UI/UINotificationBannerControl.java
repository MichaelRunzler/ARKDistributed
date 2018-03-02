package core.UI;

import core.CoreUtil.JFXUtil;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Allows for dynamic UI banner-style notification inside a larger UI component.
 */
public class UINotificationBannerControl
{
    /**
     * Represents severity levels for displayed notifications
     */
    public enum Severity{
        DEBUG, INFO, WARNING, ERROR
    }

    private Timer UITimer;
    private Label display;
    private ArrayList<NotificationBannerEvent> tasksScheduled;
    private HashMap<Severity, Node> icons;
    private Label queuedDisplayChange;

    /**
     * Constructs a default instance of this class. Uses a new default instance of a {@link Label} for its display node.
     */
    public UINotificationBannerControl() {
        this(new Label());
    }

    /**
     * Constructs a new instance of this class with the specified {@link Label} as its display node.
     * @param labelNode the {@link Label} to use as this class's display node
     */
    public UINotificationBannerControl(Label labelNode) {
        UITimer = new Timer();
        tasksScheduled = new ArrayList<>();
        this.display = labelNode;
        queuedDisplayChange = null;

        icons = new HashMap<>();
        icons.put(Severity.DEBUG, null);
        icons.put(Severity.INFO, JFXUtil.generateGraphicFromResource("Core/assets/info.png", 15));
        icons.put(Severity.WARNING, JFXUtil.generateGraphicFromResource("Core/assets/warning.png", 15));
        icons.put(Severity.ERROR, JFXUtil.generateGraphicFromResource("Core/assets/error.png", 15));
    }

    /**
     * Displays the notification label with the specified text, severity (and associated icon), and duration.
     * If any calls are made to this method during the delay between showing the notification and hiding it again,
     * they will be queued behind the current display. As such, try to avoid displaying messages with durations more than
     * a few seconds, as they will prevent other notifications from being displayed until the previous notifications in the
     * queue are finished displaying.
     * @param text the text to display in the notification area. If null or zero-length, only the icon will be displayed.
     * @param level the {@link Severity} level of the notification. This changes which icon is used for the notification banner.
     * @param duration the time in milliseconds before the notification will be hidden
     */
    public void displayNotice(String text, Severity level, long duration)
    {
        if(duration <= 0) return;

        TimerTask show = new TimerTask() {
            @Override
            public void run() {
                display.setVisible(false);

                Platform.runLater(() ->{
                    display.setText(text);
                    display.setGraphic(icons.get(level));
                });

                FadeTransition in = new FadeTransition();
                in.setFromValue(0.0);
                in.setToValue(1.0);
                in.setDuration(new Duration(250));
                in.setNode(display);
                in.play();
                display.setVisible(true);

                // Remove this task from the queue if it was executed from it
                if(tasksScheduled.size() > 0){
                    tasksScheduled.remove(0);
                    // Check queue size again, if there are still more tasks, chain to the next one.
                    if(tasksScheduled.size() > 0) UITimer.schedule(tasksScheduled.get(0).task, tasksScheduled.get(0).delay);
                    // If not, check to see if a display node change was requested.
                    else checkForDisplayChange();
                }
            }
        };

        TimerTask hide = new TimerTask() {
            @Override
            public void run() {
                FadeTransition out = new FadeTransition();
                out.setFromValue(1.0);
                out.setToValue(0.0);
                out.setDuration(new Duration(250));
                out.setNode(display);
                out.play();
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignored) {}
                display.setVisible(false);

                // Remove this task from the queue if it was executed from it
                if(tasksScheduled.size() > 0){
                    tasksScheduled.remove(0);
                    // Check queue size again, if there are still more tasks, chain to the next one.
                    if(tasksScheduled.size() > 0) UITimer.schedule(tasksScheduled.get(0).task, tasksScheduled.get(0).delay);
                        // If not, check to see if a display node change was requested.
                    else checkForDisplayChange();
                }
            }
        };

        // Check if the queue is empty - if it is, it will need to be kickstarted again since no events are available to chain to it.
        boolean isEmpty = tasksScheduled.size() == 0;

        // Add the show-display and hide-display events to the queue, it will be chained to when the show-display event finishes.
        tasksScheduled.add(new NotificationBannerEvent(show, 0));
        tasksScheduled.add(new NotificationBannerEvent(hide, duration));
        if(isEmpty) UITimer.schedule(tasksScheduled.get(0).task, tasksScheduled.get(0).delay);
    }

    /**
     * Displays the notification label with the specified text, severity (and associated icon), and duration.
     * Behaves exactly the same way as {@link #displayNotice(String, Severity, long)}, but clears the display queue before
     * showing this notification, ensuring that this notification will be displayed (almost) immediately.
     * @param text the text to display in the notification area. If null or zero-length, only the icon will be displayed.
     * @param level the {@link Severity} level of the notification. This changes which icon is used for the notification banner.
     * @param duration the time in milliseconds before the notification will be hidden
     */
    public void displayNoticeImmediately(String text, Severity level, long duration)
    {
        UITimer.cancel();
        UITimer = new Timer();
        tasksScheduled.clear();
        displayNotice(text, level, duration);
    }

    /**
     * Forcibly terminates any current label display events and clears the display queue.
     * Also resets the label to its invisible state.
     */
    public void clearQueue()
    {
        UITimer.cancel();
        UITimer = new Timer();

        tasksScheduled.clear();
        display.setVisible(false);

        checkForDisplayChange();
    }

    /**
     * Sets this class's display node to the specified {@link Label}.
     * If the system is in the process of displaying any notifications at the moment, the change will not take effect until
     * the queue is empty or {@link #clearQueue()} is called.
     * If the provided {@link Label} is null, the change will be ignored.
     * @param newLabel the {@link Label} to use as the display node
     */
    public void setLabelNode(Label newLabel) {
        this.display = newLabel;
    }

    /**
     * Returns this class's current display node. Note that if {@link #setLabelNode(Label)} was called, the result of this
     * method may or may not reflect that change, since calls to {@link #setLabelNode(Label)} are deferred until queue execution
     * finishes, and the active display node is not updated until that condition is met.
     * @return the {@link Label} acting as the current active display node
     */
    public Label getLabelNode() {
        return display;
    }

    private void checkForDisplayChange()
    {
        if (queuedDisplayChange == null || tasksScheduled.size() != 0) return;

        this.display = queuedDisplayChange;
        queuedDisplayChange = null;
    }
}