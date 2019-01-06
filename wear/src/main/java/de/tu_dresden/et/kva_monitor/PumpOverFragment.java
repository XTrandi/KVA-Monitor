package de.tu_dresden.et.kva_monitor;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;


public class PumpOverFragment extends SectionFragment {

    static final String ARGUMENT_SOURCE_TANK = "Argument_SourceTank";
    static final String ARGUMENT_TARGET_TANK = "Argument_TargetTank";

    private static final int MAX_WATER_LEVEL = 280;

    // ID=0 <--> Tank1, ID=1 <--> Tank2, ID=2 <--> Tank 3
    // SourceTank1 -> P1, SourceTank2 -> P2, SourceTank3 -> P3
    private int sourceTankID         = 0;
    private int targetTankID         = 1;

    private float[] waterLevel       = new float[3];
    private boolean [] pumpState     = new boolean[3];
    private int pumpStateAnalog;
    private boolean [] drainValve    = new boolean[3];
    private boolean [] sourceValve   = new boolean[3];

    private ShapeTankView sourceTankView;
    private ShapeTankView targetTankView;
    private ShapePumpView pumpView;
    private PipeView leftPipeView;
    private PipeView rightPipeView;

    private static final float Y_CENTER_RATE = 0.66f;

    Paint textPaint;
    static final int TEXT_SIZE_PX   = 14;


    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {


        final View view = inflater.inflate(R.layout.pump_over_frame, container, false);

        Bundle bundle = this.getArguments();
        if ( bundle!=null ) {
            sourceTankID = bundle.getInt(ARGUMENT_SOURCE_TANK);
            targetTankID = bundle.getInt(ARGUMENT_TARGET_TANK);
        }


        // Define text font
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        // scale text size from pixel (px) to scalable pixels (sp <--> dp for fonts)
        textPaint.setTextSize( TEXT_SIZE_PX * getResources().getDisplayMetrics().density );
        // not sure if this codeline is supported...
        //textPaint.setTypeface(
        //        Typeface.create("Roboto Condensed", Typeface.NORMAL) );

        sourceTankView = view.findViewById(R.id.source_tank_view);
        targetTankView = view.findViewById(R.id.target_tank_view);
        pumpView       = view.findViewById(R.id.pump_view);
        leftPipeView   = view.findViewById(R.id.left_pipe_view);
        rightPipeView  = view.findViewById(R.id.right_pipe_view);

        sourceTankView.setTextPaint(textPaint);
        targetTankView.setTextPaint(textPaint);
        pumpView.setTextPaint(textPaint);

        sourceTankView.setText("B" + String.valueOf(sourceTankID+1) );
        targetTankView.setText("B" + String.valueOf(targetTankID+1) );
        pumpView.setText("P" + String.valueOf(sourceTankID+1) );

        class InspectTouchArea implements View.OnTouchListener {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                FieldDeviceView fieldDeviceView = (FieldDeviceView) view;

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        fieldDeviceView.setPressed( event.getX(), event.getY() );
                        break;
                    case MotionEvent.ACTION_MOVE:
                        fieldDeviceView.setPressed( event.getX(), event.getY() );
                        break;

                    case MotionEvent.ACTION_UP:
                        if ( fieldDeviceView.getPressed() ) {
                            
                            if (fieldDeviceView.equals(sourceTankView)) {
                                sourceTankID = (sourceTankID + 1) % 3;
                                sourceTankView.setWaterLevel( (int) waterLevel[sourceTankID] );
                                sourceTankView.setText("B" + String.valueOf(sourceTankID+1) );
                                sourceTankView.invalidate();

                                pumpView.setPumpState( pumpState[sourceTankID] );
                                if (sourceTankID == 2) {
                                    pumpView.displayAnalogValue(true);
                                    pumpView.setAnalogValue(pumpStateAnalog);
                                }
                                else {
                                    pumpView.displayAnalogValue(false);
                                }
                                pumpView.setText("P" + String.valueOf(sourceTankID+1) );
                                pumpView.invalidate();

                                leftPipeView.setState( drainValve[sourceTankID] );
                            }
                            else if (fieldDeviceView.equals(targetTankView)) {
                                targetTankID = (targetTankID + 1) % 3;
                                targetTankView.setWaterLevel( (int) waterLevel[targetTankID] );
                                targetTankView.setText("B" + String.valueOf(targetTankID+1) );
                                targetTankView.invalidate();

                                rightPipeView.setState( sourceValve[targetTankID] );
                            }
                            else if (fieldDeviceView.equals(pumpView)) {
                                // Prepare data map to contain data items to send
                                PutDataMapRequest putDataMapRequest =
                                        PutDataMapRequest.create(PATH_OPC_REQUEST);
                                DataMap dataMap = putDataMapRequest.getDataMap();

                                // set data
                                /*
                                Use of string arrays enables handheld listener to universally send
                                XML data requests independently of the required service. Unique
                                key identifiers for the data map can be used.
                                */
                                dataMap.putStringArray("item_names", new String[]{
                                        "Behaelter_A_FL",
                                        "Behaelter_B_FL",
                                        "Auto_Fuellst_FL",
                                        "Start_Umpumpen_FL"
                                });
                                dataMap.putStringArray("item_types", new String[]{
                                        "int",
                                        "int",
                                        "boolean",
                                        "boolean"
                                });
                                dataMap.putStringArray("item_values", new String[]{
                                        String.valueOf( sourceTankID+1 ),
                                        String.valueOf( targetTankID+1),
                                        String.valueOf( true ),
                                        String.valueOf( !pumpState[sourceTankID] )
                                });

                                PutDataRequest request = putDataMapRequest.asPutDataRequest();
                                request.setUrgent();
                                Task<DataItem> putDataTask = myDataClient.putDataItem(request);
                                // Optionally a Success / Failure listener may be added to the task
                            }

                            if (sourceTankID == targetTankID) {
                                leftPipeView.setVisibility( View.INVISIBLE );
                                rightPipeView.setVisibility( View.INVISIBLE );
                                pumpView.enableInteraction(false);
                            }
                            else {
                                leftPipeView.setVisibility( View.VISIBLE );
                                rightPipeView.setVisibility( View.VISIBLE );
                                pumpView.enableInteraction(true);
                            }

                            break;
                        }

                    default:
                        break;
                }

                return true;
            }
        }

        sourceTankView.setOnTouchListener( new InspectTouchArea() );
        targetTankView.setOnTouchListener( new InspectTouchArea() );
        pumpView.setOnTouchListener( new InspectTouchArea() );

        sourceTankView.setMaxWaterLevel(MAX_WATER_LEVEL);
        targetTankView.setMaxWaterLevel(MAX_WATER_LEVEL);

        // Inflating layout is ready, resizing possible
        view.post(new Runnable() {
            @Override
            public void run() {
                sourceTankView.resize();
                targetTankView.resize();
                pumpView.resize(Y_CENTER_RATE, 1f);
                leftPipeView.resize(Y_CENTER_RATE);
                rightPipeView.resize(Y_CENTER_RATE);
            }
        });

        //myDataClient = Wearable.getDataClient(getContext());
        super.onCreateView(inflater, container, savedInstanceState);

        return view;
    }

    // Reads data from DataItem, stores and transfer them to the UserInterface
    protected void readOutData(DataItem item) {

        if (item.getUri().getPath().compareTo(PATH_WEAR_UI) == 0) {
            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
            waterLevel[0]   = dataMap.getFloat("Fuellstand1_Ist");
            waterLevel[1]   = dataMap.getFloat("Fuellstand2_Ist");
            waterLevel[2]   = dataMap.getFloat("Fuellstand3_Ist");
            pumpState[0]    = dataMap.getBoolean("P1");
            pumpState[1]    = dataMap.getBoolean("P2");
            pumpStateAnalog = dataMap.getInt("P3");
            pumpState[2]    = (pumpStateAnalog > 0);
            drainValve[0]   = dataMap.getFloat("V1") > 0;
            drainValve[1]   = dataMap.getFloat("V2") > 0;
            drainValve[2]   = dataMap.getBoolean("Y7");
            sourceValve[0]  = dataMap.getBoolean("Y1");
            sourceValve[1]  = dataMap.getBoolean("Y2");
            sourceValve[2]  = dataMap.getBoolean("Y5") || dataMap.getBoolean("Y6");

            sourceTankView.setWaterLevel( (int) waterLevel[sourceTankID] );
            targetTankView.setWaterLevel( (int) waterLevel[targetTankID] );
            pumpView.setPumpState( pumpState[sourceTankID] );

            if (sourceTankID == 2) {
                pumpView.displayAnalogValue(true);
                pumpView.setAnalogValue(pumpStateAnalog);
            }

            leftPipeView.setState( drainValve[sourceTankID] );
            rightPipeView.setState( sourceValve[targetTankID] );
        }
    }


    @Override
    public void onEnterAmbient() {
        sourceTankView.onEnterAmbient();
        targetTankView.onEnterAmbient();
        pumpView.onEnterAmbient();
        leftPipeView.onEnterAmbient();
        rightPipeView.onEnterAmbient();

        textPaint.setColor(Color.WHITE);
    }

    @Override
    public void onExitAmbient() {
        sourceTankView.onExitAmbient();
        targetTankView.onExitAmbient();
        pumpView.onExitAmbient();
        leftPipeView.onExitAmbient();
        rightPipeView.onExitAmbient();

        textPaint.setColor(Color.BLACK);
    }

    @Override
    public void onUpdateAmbient() {

    }
}
