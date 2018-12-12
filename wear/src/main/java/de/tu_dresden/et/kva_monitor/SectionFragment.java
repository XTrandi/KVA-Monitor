package de.tu_dresden.et.kva_monitor;

import android.app.Fragment;

public abstract class SectionFragment extends Fragment {

    public abstract void onEnterAmbient();
    public abstract void onExitAmbient();
    public abstract void onUpdateAmbient();

}
