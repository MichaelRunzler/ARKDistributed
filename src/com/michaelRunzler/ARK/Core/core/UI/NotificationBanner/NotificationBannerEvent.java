package core.UI.NotificationBanner;

import java.util.TimerTask;

/**
 * Represents a queued timer trigger event for the {@link UINotificationBannerControl} class.
 * Data container-type object.
 */
public class NotificationBannerEvent
{
    public TimerTask task;
    public long delay;

    public NotificationBannerEvent(TimerTask task, long delay)
    {
        this.task = task;
        this.delay = delay;
    }
}
