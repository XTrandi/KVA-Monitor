package de.tu_dresden.et.kva_monitor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.NumberFormat;

public class TemperatureControlFragment extends SectionFragment{

    private static final int MAX_WATER_LEVEL            = 280;
    private static final int MIN_TEMPERATURE_SETPOINT   = 20;
    private static final int MAX_TEMPERATURE_SETPOINT   = 55;

    private ShapeTankView tankView;
    private ImageView motorView;
    private ImageView heatExchangerView;
    private TextView temperatureTextView;
    private TextView temperatureSetPointTextView;
    private ImageButton controlStateButton;
    private View setPointOpenDialogView;

    private ImageView stirView;
    private View motorStirConnectorView;
    private ImageView temperatureSensorView;
    private View temperatureConnectorView;

    private Dialog dialog;

    //private NumberPicker setPointNumberPicker;

    private boolean ambientMode = false;

    private boolean heatingState = false;
    private boolean stirrerState = false;
    private boolean controlState = false;

    private int temperatureSetPoint;

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.temperature_control_frame, container, false);

        // dialog for setpoint picker
        final NumberPicker setPointNumberPicker = new NumberPicker(getContext());
        setPointNumberPicker.setMaxValue(MAX_TEMPERATURE_SETPOINT);
        setPointNumberPicker.setMinValue(MIN_TEMPERATURE_SETPOINT);
        setPointNumberPicker.setWrapSelectorWheel(false);
        setNumberPickerTextColor(setPointNumberPicker, Color.BLACK);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setView( setPointNumberPicker );
        dialogBuilder.setTitle( getString(R.string.change_setpoint) );
        dialogBuilder.setNegativeButton(getString(R.string.cancel), null);
        dialogBuilder.setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_OPC_REQUEST);
                DataMap dataMap = putDataMapRequest.getDataMap();

                // set data
                /*
                Use of string arrays enables handheld listener to universally send
                XML data requests independently of the required service. Unique
                key identifiers for the data map can be used.
                */
                dataMap.putStringArray("item_names", new String[]{"Temperatur_Soll_FL"});
                dataMap.putStringArray("item_types", new String[]{"int"});
                dataMap.putStringArray("item_values", new String[]{
                        String.valueOf( setPointNumberPicker.getValue() ),});

                PutDataRequest request = putDataMapRequest.asPutDataRequest();
                request.setUrgent();
                myDataClient.putDataItem(request);
            }
        });
        dialog = dialogBuilder.create();

        // dynamic / active UI elements
        tankView                    = view.findViewById(R.id.tank_view);
        motorView                   = view.findViewById(R.id.motor_view);
        heatExchangerView           = view.findViewById(R.id.heater_view);
        temperatureTextView         = view.findViewById(R.id.temperature_textView);
        temperatureSetPointTextView = view.findViewById(R.id.temperature_setpoint_textView);
        controlStateButton          = view.findViewById(R.id.control_button);
        setPointOpenDialogView      = view.findViewById(R.id.setpoint_picker_button);

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

        controlStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_OPC_REQUEST);
                DataMap dataMap = putDataMapRequest.getDataMap();

                // set data
                /*
                Use of string arrays enables handheld listener to universally send
                XML data requests independently of the required service. Unique
                key identifiers for the data map can be used.
                */
                dataMap.putStringArray("item_names", new String[]{
                        "Auto_Temp_FL",
                        "Start_Temperatur_FL"
                });
                dataMap.putStringArray("item_types", new String[]{
                        "boolean",
                        "boolean"
                });
                dataMap.putStringArray("item_values", new String[]{
                        String.valueOf( !controlState ),
                        String.valueOf( !controlState )
                });

                PutDataRequest request = putDataMapRequest.asPutDataRequest();
                request.setUrgent();
                myDataClient.putDataItem(request);
            }
        });

        setPointOpenDialogView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPointNumberPicker.setValue(temperatureSetPoint);
                dialog.show();
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
        setPointOpenDialogView.setVisibility(View.INVISIBLE);

        motorView.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.CLEAR);
        dialog.dismiss();

        updateUI();

        super.onEnterAmbient();
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
        setPointOpenDialogView.setVisibility(View.VISIBLE);

        motorView.setColorFilter(Color.BLACK);
        stirView.setColorFilter(Color.BLACK);
        motorStirConnectorView.setBackgroundColor(Color.BLACK);
        heatExchangerView.setColorFilter(Color.BLACK);

        updateUI();

        super.onExitAmbient();
    }

    @Override
    protected void readOutData(DataItem item) {
        if (item.getUri().getPath().compareTo(PATH_WEAR_UI) == 0) {
            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

            temperatureTextView.setText(
                    String.format("%s °C", dataMap.getFloat("Temperatur_Ist") ) );

            temperatureSetPoint = dataMap.getInt("Temperatur_Soll_FL");
            temperatureSetPointTextView.setText( String.format("%s °C", temperatureSetPoint) );

            tankView.setWaterLevel( (int) dataMap.getFloat("Fuellstand3_Ist") );

            heatingState = dataMap.getBoolean("W");
            stirrerState = dataMap.getBoolean("M");
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


    /*
    Method to change a number picker's text colour.
    Credits to: https://stackoverflow.com/a/22962195
     */
    private static void setNumberPickerTextColor(NumberPicker numberPicker, int color)
    {

        try{
            Field selectorWheelPaintField = numberPicker.getClass()
                    .getDeclaredField("mSelectorWheelPaint");
            selectorWheelPaintField.setAccessible(true);
            ((Paint)selectorWheelPaintField.get(numberPicker)).setColor(color);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        final int count = numberPicker.getChildCount();
        for(int i = 0; i < count; i++){
            View child = numberPicker.getChildAt(i);
            if(child instanceof EditText)
                ((EditText)child).setTextColor(color);
        }
        numberPicker.invalidate();
    }
}
