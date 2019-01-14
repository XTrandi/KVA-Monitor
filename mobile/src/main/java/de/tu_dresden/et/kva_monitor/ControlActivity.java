package de.tu_dresden.et.kva_monitor;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class ControlActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean serviceStarted = false;

        // check whether this package's CommService is running and retrieve its intent
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if( CommService.class.getName().equals(service.service.getClassName())) {
                serviceStarted = true;
                break;
            }
        }

        final Intent shutdownServiceIntent = new Intent(this, CommService.class);
        shutdownServiceIntent.putExtra("stop_service", serviceStarted);

        final String toastMessage;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        //dialogBuilder.setView( setPointNumberPicker );
        if (serviceStarted) {
            dialogBuilder.setTitle( getString(R.string.stop_service_title) );
            dialogBuilder.setMessage(getString(R.string.stop_service_message));
            toastMessage = getString(R.string.service_stopped);
        }
        else {
            dialogBuilder.setTitle(getString(R.string.start_service_title));
            dialogBuilder.setMessage(getString(R.string.start_service_message));
            toastMessage = getString(R.string.service_started);
        }

        dialogBuilder.setNegativeButton(getString(R.string.cancel), null);
        dialogBuilder.setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startService(shutdownServiceIntent);
                Toast toast = Toast.makeText(ControlActivity.this, toastMessage, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
        dialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ControlActivity.this.finish();
            }
        });

        dialogBuilder.create().show();

    }

}
