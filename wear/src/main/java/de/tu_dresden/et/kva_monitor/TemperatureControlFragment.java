package de.tu_dresden.et.kva_monitor;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;

import java.text.NumberFormat;

public class TemperatureControlFragment extends SectionFragment{

    private static final int MAX_WATER_LEVEL = 280;

    private ShapeTankView tankView;
    private ImageView motorView;
    private ImageView heatExchangerView;
    private TextView temperatureTextView;
    private TextView temperatureSetPointTextView;
    private ImageButton controlStateButton;

    private ImageView stirView;
    private View motorStirConnectorView;
    private ImageView temperatureSensorView;
    private View temperatureConnectorView;

    private boolean ambientMode = false;

    private boolean heatingState = false;
    private boolean stirrerState = false;
    private boolean controlState = false;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.temperature_control_frame, container, false);

        // dynamic / active UI elements
        tankView                    = view.findViewById(R.id.tank_view);
        motorView                   = view.findViewById(R.id.motor_view);
        heatExchangerView           = view.findViewById(R.id.heater_view);
        temperatureTextView         = view.findViewById(R.id.temperature_textView);
        temperatureSetPointTextView = view.findViewById(R.id.temperature_setpoint_textView);
        controlStateButton          = view.findViewById(R.id.control_button);

        tankView.setMaxWaterLevel(MAX_WATER_LEVEL);
        motorView.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.CLEAR);

        // static / semidynamic UI elements
        stirView                    = view.findViewById(R.id.stir_view);
        motorStirConnectorView      = view.findViewById(R.id.motor_stir_connector_view);
        temperatureSensorView       = view.findViewById(R.id.temperature_sensor_view);
        temperatureConnectorView    = view.findViewById(R.id.temperature_sensor_connector_view);

        view.post(new Runnable() {
            @Override
            public void run() {
                tankView.resize();
            }
        });

        super.onCreateView(inflater, container, savedInstanceState);

        return view;
    }

    @Override
    public void onEnterAmbient() {
        ambientMode = true;
        tankView.onEnterAmbient();
        temperatureTextView.setTextColor(Color.WHITE);
        temperatureSetPointTextView.setVisibility(View.INVISIBLE);
        temperatureConnectorView.setBackgroundColor(Color.WHITE);
        temperatureSensorView.setColorFilter(Color.WHITE);
        controlStateButton.setVisibility(View.INVISIBLE);

        motorView.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.CLEAR);

        updateUI();
    }

    @Override
    public void onExitAmbient() {
        ambientMode = false;
        tankView.onExitAmbient();
        temperatureTextView.setTextColor(Color.BLACK);
        temperatureSetPointTextView.setVisibility(View.VISIBLE);
        temperatureConnectorView.setBackgroundColor(Color.BLACK);
        temperatureSensorView.setColorFilter(Color.BLACK);
        controlStateButton.setVisibility(View.VISIBLE);

        motorView.setColorFilter(Color.BLACK);
        stirView.setColorFilter(Color.BLACK);
        motorStirConnectorView.setBackgroundColor(Color.BLACK);
        heatExchangerView.setColorFilter(Color.BLACK);

        updateUI();
    }

    @Override
    public void onUpdateAmbient() {

    }

    @Override
    protected void readOutData(DataItem item) {
        if (item.getUri().getPath().compareTo(PATH_WEAR_UI) == 0) {
            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
            temperatureTextView.setText(
                    String.format("%s °C", NumberFormat.getInstance().
                            format(dataMap.getFloat("Temperatur_Ist") ) ) );

            temperatureSetPointTextView.setText(
                    String.format("%s °C", dataMap.getInt("Temperatur_Soll_FL")) );

            tankView.setWaterLevel( (int) dataMap.getFloat("Fuellstand3_Ist") );

            heatingState = dataMap.getBoolean("W");
            stirrerState = dataMap.getBoolean("M");
            heatingState = true;
            controlState = dataMap.getBoolean("Start_Temperatur_FL");

            updateUI();
        }
    }

    private void updateUI () {
        if ( ambientMode ) {
            if (stirrerState) {
                motorView.setColorFilter(Color.WHITE);
                stirView.setColorFilter(Color.WHITE);
                motorStirConnectorView.setBackgroundColor(Color.WHITE);
            } else {
                motorView.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                stirView.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                motorStirConnectorView.setBackgroundColor(Color.TRANSPARENT);
            }

            if (heatingState) {
                heatExchangerView.setColorFilter(Color.WHITE);
            } else {
                heatExchangerView.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }

        }
        else {
            if (stirrerState) {
                motorView.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP);
            } else {
                motorView.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.CLEAR);
            }

            if (heatingState) {
                heatExchangerView.setColorFilter(Color.GREEN);
            } else {
                heatExchangerView.setColorFilter(Color.BLACK);
            }
        }

        if (controlState) { controlStateButton.setImageResource(R.drawable.button_stop); }
        else              { controlStateButton.setImageResource(R.drawable.button_run); }
    }


}
