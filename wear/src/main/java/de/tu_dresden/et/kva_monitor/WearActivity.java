package de.tu_dresden.et.kva_monitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class WearActivity extends WearableActivity implements DataClient.OnDataChangedListener {

    static final String START_ARGUMENT      = "Start_Argument";

    /**
     * Calculating the hash codes of possible data points that caused a notification
     */
    static final int START_ARGUMENT_DEFAULT = 0;
    static final int START_ARGUMENT_LH1     = "LH1".hashCode();
    static final int START_ARGUMENT_LH2     = "LH2".hashCode();
    static final int START_ARGUMENT_LH3     = "LH3".hashCode();
    static final int START_ARGUMENT_LL1     = "LL1".hashCode();
    static final int START_ARGUMENT_LL2     = "LL2".hashCode();
    static final int START_ARGUMENT_LL3     = "LL3".hashCode();

    // Polling interval to check connection status
    static final int POLLING_INTERVAL_SEC       = 5;
    // Maximum allowed difference between reply time from OPC server (with phone as intermediate)
    // and watch time
    static final int MAX_TIME_DIFFERENCE_SEC    = 60;

    private int currentFragmentID = -1;

    private SectionFragment currentFragment;
    private TextView timeView;
    private ImageView connectionStatusView;

    private ScheduledExecutorService scheduler;
    private DataClient myDataClient;

    private long lastReplyTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wear);

        // retrieve layout's views
        timeView = findViewById(R.id.time_view);
        connectionStatusView = findViewById(R.id.connection_status_view);

        WearableNavigationDrawerView myNavigationDrawer = findViewById(R.id.navigation_drawer);
        myNavigationDrawer.setAdapter(new NavigationAdapter(this));
        myNavigationDrawer.addOnItemSelectedListener(new WearableNavigationDrawerView.OnItemSelectedListener() {

            /**
             * determine which fragment to display
             * @param index the selected item
             */
            @Override
            public void onItemSelected(int index) {
                if (currentFragmentID == index) { return; }

                currentFragmentID = index;

                switch(index) {
                    case 0:
                        currentFragment = new PumpOverFragment();
                        break;
                    case 1:
                        currentFragment = new TemperatureControlFragment();
                        break;
                    default:
                        return;
                }

                // switch view by loading new fragment
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, currentFragment)
                       .commit();

                Log.d("Wear", String.valueOf(index)); // confirmed
            }
        });

        // if this activity was not directly started by the app-icon but fired by the
        // ActivityLauncherService
        Intent intent = getIntent();
        int argValue = intent.getIntExtra(START_ARGUMENT, START_ARGUMENT_DEFAULT);
        Bundle args = new Bundle();

        // High limit water level reached. Define drain tanks and possible target tanks
        if ( argValue == START_ARGUMENT_LH1 ) {
            args.putInt(PumpOverFragment.ARGUMENT_SOURCE_TANK, 0);
            args.putInt(PumpOverFragment.ARGUMENT_TARGET_TANK, 1); // or 2
            currentFragment = new PumpOverFragment();
            currentFragment.setArguments(args);

        } else if ( argValue == START_ARGUMENT_LH2 ) {
            args.putInt(PumpOverFragment.ARGUMENT_SOURCE_TANK, 1);
            args.putInt(PumpOverFragment.ARGUMENT_TARGET_TANK, 2); // or 0
            currentFragment = new PumpOverFragment();
            currentFragment.setArguments(args);

        } else if ( argValue == START_ARGUMENT_LH3 ) {
            args.putInt(PumpOverFragment.ARGUMENT_SOURCE_TANK, 2);
            args.putInt(PumpOverFragment.ARGUMENT_TARGET_TANK, 0); // or 1
            currentFragment = new PumpOverFragment();
            currentFragment.setArguments(args);

        }
        // Low limit water level reached. Define target tank and possible source tank
        else if ( argValue == START_ARGUMENT_LL1 ) {
            args.putInt(PumpOverFragment.ARGUMENT_SOURCE_TANK, 2); // or 1
            args.putInt(PumpOverFragment.ARGUMENT_TARGET_TANK, 0);
            currentFragment = new PumpOverFragment();
            currentFragment.setArguments(args);

        } else if ( argValue == START_ARGUMENT_LL2 ) {
            args.putInt(PumpOverFragment.ARGUMENT_SOURCE_TANK, 0); // or 2
            args.putInt(PumpOverFragment.ARGUMENT_TARGET_TANK, 1);
            currentFragment = new PumpOverFragment();
            currentFragment.setArguments(args);

        } else if ( argValue == START_ARGUMENT_LL3 ) {
            args.putInt(PumpOverFragment.ARGUMENT_SOURCE_TANK, 1); // or 0
            args.putInt(PumpOverFragment.ARGUMENT_TARGET_TANK, 2);
            currentFragment = new PumpOverFragment();
            currentFragment.setArguments(args);

        }
        // Nothing specified
        else {
            currentFragment = new PumpOverFragment();
        }

        // display initial fragment
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, currentFragment )
                .commit();

        // Check connection status (to phone via bluetooth + OPC server via HTTP) periodically
        myDataClient = Wearable.getDataClient(this);

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate( new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long timeDifference = Calendar.getInstance().getTime().getTime() - lastReplyTime;
                        if (timeDifference > MAX_TIME_DIFFERENCE_SEC * 1000) {
                            // connection timeout exceed, inform user with system icon
                            connectionStatusView.setVisibility(View.VISIBLE);
                        }
                        else {
                            connectionStatusView.setVisibility(View.INVISIBLE);
                        }
                    }
                });


            }
        }, POLLING_INTERVAL_SEC, POLLING_INTERVAL_SEC, TimeUnit.SECONDS);


        // Enables Always-on mode
        setAmbientEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myDataClient.addListener(this);
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        for (DataEvent event: dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(SectionFragment.PATH_WEAR_UI) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    String sReplyTime = dataMap.getString("ReplyTime");

                    // ISO 8601 time zone is not available on API 23 or lower, therefore the time
                    // zone is hardcoded
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+01:00'");

                    try {
                        Date date = format.parse(sReplyTime);
                        lastReplyTime = date.getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        myDataClient.removeListener(this);
    }

    // Wearable navigation drawer UI on top of the activity
    private final class NavigationAdapter
            extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {

        private final Context myContext;

        NavigationAdapter(final Context context) {
            myContext = context;
        }

        @Override
        public String getItemText(int index) {
            String subtext = null;

            switch (index) {
                case 0:
                    subtext = getString(R.string.pump_over);
                    break;
                case 1:
                    subtext = getString(R.string.temperature_control);
                    break;
                default:
                    break;
            }

            return subtext;
        }

        @Override
        public Drawable getItemDrawable(int index) {
            switch(index) {
                case 0:
                    return myContext.getDrawable( R.drawable.ic_pump_over);
                default:
                    return myContext.getDrawable( R.drawable.ic_temperature_control );
            }

        }

        @Override
        public int getCount() {
            return 2;
        }

    }


    // send ambient mode towards child fragment and its views
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        connectionStatusView.setColorFilter(Color.WHITE);

        if (currentFragment != null) {
            currentFragment.onEnterAmbient();
            timeView.setVisibility(View.VISIBLE);
            this.setTime();
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        connectionStatusView.setColorFilter(Color.BLACK);

        if (currentFragment != null) {
            currentFragment.onExitAmbient();
            timeView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();

        if (currentFragment != null) {
            currentFragment.onUpdateAmbient();
            this.setTime();
        }
    }

    /**
     * sets the time during ambient mode in default format
     */
    private void setTime() {
        String sTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
        timeView.setText(sTime);
    }

    @Override
    protected void onDestroy() {
        scheduler.shutdown();
        super.onDestroy();
    }
}
