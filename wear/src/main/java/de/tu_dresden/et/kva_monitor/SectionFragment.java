package de.tu_dresden.et.kva_monitor;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;

public abstract class SectionFragment extends Fragment implements DataClient.OnDataChangedListener {

    static final String PATH_WEAR_UI        = "/wear_UI";
    static final String PATH_OPC_REQUEST    = "/OPC_request";

    DataClient myDataClient;

    /*
    In ambient mode the UI is supposed to be updated via onUpdateAmbient (about every 1 minute).
    Therefore the fragment does not listen to changes during ambient mode but will retrieve them
    manually on ambient update demand.
     */

    public void onEnterAmbient() {
        myDataClient.removeListener(this);
    }

    public void onExitAmbient() {
        myDataClient.addListener(this);
    }

    public void onUpdateAmbient() {
        this.onResume();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        myDataClient = Wearable.getDataClient(getContext());
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Load initial data.
        Task<DataItemBuffer> task = myDataClient.getDataItems();
        task.addOnSuccessListener(
                new OnSuccessListener<DataItemBuffer>() {
                    @Override
                    public void onSuccess(DataItemBuffer dataItemBuffer) {
                        Log.d("Wear", "Startup receive successful");
                        for (DataItem dataItem: dataItemBuffer) {
                            readOutData(dataItem);
                        }
                        dataItemBuffer.release();
                    }
                }
        );
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Wear", e.getMessage());
                        // Data Client could not be accessed, handheld is not reachable.
                    }
                }
        );

        // Listen for changed data
        myDataClient.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister data client for changed data
        myDataClient.removeListener(this);
    }

    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        for (DataEvent event: dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                readOutData( event.getDataItem() );
            }
        }
    }

    protected abstract void readOutData(DataItem item);

}
