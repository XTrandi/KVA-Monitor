package de.tu_dresden.et.kva_monitor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.app.Fragment;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.internal.view.drawer.WearableNavigationDrawerPresenter;
import android.support.wearable.view.drawer.WearableNavigationDrawer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import de.tu_dresden.et.kva_monitor.R;

public class WearActivity extends WearableActivity {

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
            // ToDo: implement FragmentManager
            @Override
            public void onItemSelected(int index) {
                if (currentFragmentID == index) { return; }

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
        currentFragment = new PumpOverFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, currentFragment )
                .commit();


        // ToDo: is this code line sensible? Adapt for ambient mode
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
            // Platzhalter
            return myContext.getDrawable( R.drawable.teleag_icon );
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
