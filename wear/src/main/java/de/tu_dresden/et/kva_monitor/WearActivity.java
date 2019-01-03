package de.tu_dresden.et.kva_monitor;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;


public class WearActivity extends WearableActivity {

    static final String START_ARGUMENT      = "Start_Argument";

    static final int START_ARGUMENT_DEFAULT = 0;
    static final int START_ARGUMENT_LH1     = "LH1".hashCode();
    static final int START_ARGUMENT_LH2     = "LH2".hashCode();
    static final int START_ARGUMENT_LH3     = "LH3".hashCode();
    static final int START_ARGUMENT_LL1     = "LL1".hashCode();
    static final int START_ARGUMENT_LL2     = "LL2".hashCode();
    static final int START_ARGUMENT_LL3     = "LL3".hashCode();

    private WearableNavigationDrawerView myNavigationDrawer;
    private int currentFragmentID = -1;

    private SectionFragment currentFragment;
    private TextView timeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wear);

        timeView = findViewById(R.id.time_view);

        myNavigationDrawer = findViewById(R.id.navigation_drawer);
        myNavigationDrawer.setAdapter(new NavigationAdapter(this));
        myNavigationDrawer.addOnItemSelectedListener(new WearableNavigationDrawerView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int index) {
                if (currentFragmentID == index) { return; }

                currentFragmentID = index;

                switch(index) {
                    case 0:
                        currentFragment = new PumpOverFragment();
                        break;
                    default:
                        return;
                }

                currentFragmentID = index;

                // switch view by loading new fragment
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, currentFragment)
                       .commit();

                Log.d("Wear", String.valueOf(index)); // confirmed
            }
        });
        // ToDo: set Default View - maybe navigation drawer to be pulled the best idea?

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



        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, currentFragment )
                .commit();

        // Enables Always-on
        setAmbientEnabled();
    }

    private final class NavigationAdapter
            extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {

        private final Context myContext;

        NavigationAdapter(final Context context) {
            myContext = context;
        }

        // ToDo: Add navigation tab's titles, short icons and everything else
        @Override
        public String getItemText(int index) {
            String subtext = null;

            switch (index) {
                case 0:
                    subtext = getString(R.string.pump_over);
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
                    return myContext.getDrawable( R.drawable.teleag_icon );
            }

        }

        // ToDo: Keep in mind what actions you actually implement
        @Override
        public int getCount() {
            return 3;
        }

    }


    // send ambient mode towards fragment and its views
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        Log.d("Wear", "Entering ambient mode");
        if (currentFragment != null) {
            currentFragment.onEnterAmbient();
            timeView.setVisibility(View.VISIBLE);
            this.setTime();
        }
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

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

    private void setTime() {
        String sTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
        timeView.setText(sTime);
    }

}
