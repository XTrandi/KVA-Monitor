package de.tu_dresden.et.kva_monitor;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class NotificationIntentService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public NotificationIntentService(String name) {
        super(name);
    }

    public NotificationIntentService() {
        super("NotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle args = intent.getExtras();

        ByteBuffer buffer = ByteBuffer.allocate(4);
        int notificationID =args.getInt(CommService.PATH_LAUNCH_ACTIVITY);
        buffer.putInt( notificationID );



        try {
            // retrieve connected nodes (in this case all wearable devices)
            Task<List<Node>> nodeListTask = Wearable.getNodeClient(this).getConnectedNodes();
            List<Node> nodes = Tasks.await(nodeListTask);
            for (Node node: nodes)  {
                Task<Integer> sendMessageTask =
                        Wearable.getMessageClient(this).sendMessage(
                                node.getId(), CommService.PATH_LAUNCH_ACTIVITY, buffer.array());
                Tasks.await(sendMessageTask);
                // dismiss notification after finishing task (alternatively use onSuccessListener)
                NotificationManagerCompat.from(this).cancel(notificationID);

            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
