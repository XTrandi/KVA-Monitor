package de.tu_dresden.et.kva_monitor;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.ByteBuffer;

/**
 * This Service runs permanently in background, waiting for any incoming DataClient Messages (Remote
 * Procedure Calls) and handling them on the wear device. Currently they're sent from the
 * accompanying handheld device as a notification action only.
 */
public class ActivityLauncherService extends WearableListenerService {

    static final String PATH_LAUNCH_ACTIVITY = "/launch_activity";

    /**
     * This method is called from the Java VM when the wear device receives a message. It will fire
     * the main wear activity supplying it with the arguments from messageEvent.
     * @param messageEvent Contains the data point that originated the notification. The string is
     *                    converted to a HashCode which is stored in a 4-byte-array.
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(PATH_LAUNCH_ACTIVITY)) {
            Intent startIntent = new Intent(this, WearActivity.class);

            ByteBuffer wrapper = ByteBuffer.wrap( messageEvent.getData() );
            startIntent.putExtra(WearActivity.START_ARGUMENT, wrapper.getInt());
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(startIntent);
        }
    }
}
