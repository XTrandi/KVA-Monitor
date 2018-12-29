package de.tu_dresden.et.kva_monitor;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.ByteBuffer;

public class ActivityLauncherService extends WearableListenerService {

    static final String PATH_LAUNCH_ACTIVITY = "/launch_activity";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(PATH_LAUNCH_ACTIVITY)) {
            Intent startIntent = new Intent(this, WearActivity.class);

            ByteBuffer wrapper = ByteBuffer.wrap( messageEvent.getData() );
            startIntent.putExtra(WearActivity.START_ARGUMENT, wrapper.getInt());
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // ToDo: Check behaviour (in case multiple instances run)
            startActivity(startIntent);
        }
    }
}
