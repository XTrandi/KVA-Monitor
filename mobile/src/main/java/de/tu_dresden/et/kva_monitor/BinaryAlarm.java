package de.tu_dresden.et.kva_monitor;

/*  This is a simple placeholder for simulating alarming features during this project. Future
    implementations may add more transitions, time thresholds for allowing state transitions etc.
 */

import android.content.res.Resources;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.lang.reflect.Field;

public class BinaryAlarm {

    static final long[] VIBRATION_PATTERN       = {0, 250, 250, 250};


    // the present value of this data point
    private boolean presentValue;

    // the present value has to be equal to this reference value in order to cause an alarm
    private boolean alarmValue;

    private int notificationID;

    private CommService context;
    private NotificationCompat.Builder notificationBuilder;

    enum State {
        NORMAL,
        IN_ALARM
    }

    private State alarmState;

    public BinaryAlarm(CommService context, String resourceIdentifier) {
        this.context = context;
        alarmState = State.NORMAL;

        notificationID = resourceIdentifier.hashCode();
        // Retrieve data via resource name using Java Reflection
        Resources res = context.getResources();
        Field field = null;
        int resID = 0;

        try {
            field = R.bool.class.getDeclaredField(resourceIdentifier + "_alarmValue");
            resID = field.getInt(field);
            alarmValue = res.getBoolean(resID);
            presentValue = !alarmValue;

            field = R.string.class.getDeclaredField(resourceIdentifier + "_alarmTitle");
            resID = field.getInt(field);
            String title = res.getString(resID);

            field = R.string.class.getDeclaredField(resourceIdentifier + "_alarmText");
            resID = field.getInt(field);
            String text = res.getString(resID);

            field = R.drawable.class.getDeclaredField(resourceIdentifier + "_alarmIcon");
            int iconResID = field.getInt(field);

            // Build notification from resource
            notificationBuilder = new NotificationCompat.Builder(
                    context, CommService.CHANNEL_ID_ALARM)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(iconResID)
                    .setPriority(NotificationCompat.PRIORITY_MAX) // might include that in resource file
                    .setVibrate(VIBRATION_PATTERN)
                    .setSound( RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) );

            // Wearable-only notification features

            //Intent actionIntent = new Intent()
            NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender()
                    //.setDisplayIntent()
                    ;

            notificationBuilder.extend(wearableExtender);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return;
        }

    }


    public void setPresentValue(boolean presentValue) {
        if (presentValue == this.presentValue) { return; }

        this.presentValue = presentValue;

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // State machine
        switch (alarmState) {
            case NORMAL:
                // Condition for transitioning (currently unnecessary)
                if (presentValue == alarmValue) {
                    alarmState = State.IN_ALARM;
                    // send notification
                    notificationManager.notify(notificationID, notificationBuilder.build());

                    // to be included to the action
                    context.queueLaunchActivityMessage(notificationID);
                    // new StartWearableActivityTask().execute();
                }
                break;
            case IN_ALARM:
                if (presentValue != alarmValue) {
                    alarmState = State.NORMAL;
                    // delete notification (if unseen)
                    notificationManager.cancel(notificationID);
                }
        }

    }

}
