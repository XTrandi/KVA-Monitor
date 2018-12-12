package de.tu_dresden.et.kva_monitor;

import android.app.ActivityManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class ControlActivity extends AppCompatActivity {

    Button toggleButton;
    ToggleButton connectionButton;

    Intent commService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        // define UI widgets
        toggleButton = (Button) findViewById(R.id.toggleButton);
        connectionButton = (ToggleButton) findViewById(R.id.connectionButton);

        connectionButton.setChecked(false);

        connectionButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton View, boolean isChecked) {
                if (isChecked) {
                    if (commService == null) {
                        commService = new Intent(ControlActivity.this, CommService.class);
                    }
                    startService(commService);
                } else {
                    stopService(commService);
                }
            }
        });

        // check whether this package's CommService is running and retrieve its intent
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if( CommService.class.getName().equals(service.service.getClassName())) {

                // TODO: cleaner solution for retrieving and not overwriting the intent (low priority)
                commService = new Intent(this, CommService.class);
                connectionButton.toggle();
            }
        }

    }

    public void toggleCommService(View view) {
    }
}
