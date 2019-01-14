package de.tu_dresden.et.kva_monitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;

public class ShapePumpView extends FieldDeviceView {

    private ShapeDrawable shapeCircle;
    private ShapeDrawable pumpLines;
    private ShapeDrawable shapePumpStateBorder;
    private ShapeDrawable shapePumpState; // described as triangle with color change


    private boolean pumpState;
    private boolean pumpInteractionEnabled = true;

    public ShapePumpView(Context context) {
        super(context);
        InitShapePumpView();
    }

    public ShapePumpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        InitShapePumpView();
    }

    private void InitShapePumpView() {
        pumpState = false;

        shapeCircle  = new ShapeDrawable( new OvalShape() );

        // Defining static pump lines
        Path pathPumpLines = new Path();
        pathPumpLines.moveTo(50, 0);
        pathPumpLines.lineTo(100, 50);
        pathPumpLines.lineTo(50, 100);
        //myPath.arcTo(0, 0, 100, 100, 90,180, false);

        pumpLines = new ShapeDrawable( new PathShape(pathPumpLines, 100, 100) );


        // Defining pump state shape
        Path pathPumpState = new Path();
        pathPumpState.moveTo(45, 50);
        pathPumpState.lineTo(25, 30);
        pathPumpState.lineTo(25, 70);
        pathPumpState.lineTo(45, 50);

        shapePumpState = new ShapeDrawable(
                new PathShape(pathPumpState, 100, 100) );
        shapePumpState.getPaint().setColor(Color.GREEN);

        shapePumpStateBorder = new ShapeDrawable(
                new PathShape(pathPumpState, 100, 100) );

        inputFeedback = new ShapeDrawable( new OvalShape() );
        inputFeedback.getPaint().set(inputFeedbackPaint);
        // Define the paints
        onExitAmbient();
    }

    public void resize(float yCenter, float span) {
        int width  = this.getWidth();
        int height = this.getHeight();
        float size = span * width;


        coordLeft   = (int) ( width - size + STROKE_WIDTH ) / 2;
        coordRight  = (int) ( width + size - STROKE_WIDTH) / 2;
        coordTop    = (int) ( ( yCenter * height) - (size - STROKE_WIDTH) / 2 );
        coordBottom = (int) ( ( yCenter * height) + (size - STROKE_WIDTH) / 2 );

        shapeCircle.setBounds(coordLeft, coordTop, coordRight, coordBottom);
        pumpLines.setBounds(coordLeft, coordTop, coordRight, coordBottom);
        shapePumpState.setBounds(coordLeft, coordTop, coordRight, coordBottom);
        shapePumpStateBorder.setBounds(coordLeft, coordTop, coordRight, coordBottom);
        inputFeedback.setBounds(coordLeft, coordTop, coordRight, coordBottom);

        super.resize();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        shapeCircle.draw(canvas);
        pumpLines.draw(canvas);

        if ( ambientMode ) {
            if ( pumpState ) {
                shapePumpStateBorder.draw(canvas);
            }
        }
        else  {
            if ( pumpState ) {
                shapePumpState.draw(canvas);
            }
            shapePumpStateBorder.draw(canvas);
        }

        super.onDraw(canvas);
    }

    @Override
    public void onEnterAmbient() {
        setAmbientPaint( shapeCircle.getPaint() );
        setAmbientPaint( pumpLines.getPaint() );
        setAmbientPaint( shapePumpStateBorder.getPaint() );

        super.onEnterAmbient();
    }

    @Override
    public void onExitAmbient() {
        setStandardActivePaint( shapeCircle.getPaint() );
        setStandardActivePaint( pumpLines.getPaint() );
        setStandardActivePaint( shapePumpStateBorder.getPaint() );

        super.onExitAmbient();
    }

    @Override
    public void onUpdateAmbient() {

    }

    @Override
    public void setPressed(float x, float y) {
        int radius  = (coordRight - coordLeft) / 2;
        int xCenter = (coordRight + coordLeft) / 2;
        int yCenter = (coordTop + coordBottom) / 2;

        boolean areaPressed = ( Math.pow(x - xCenter, 2) + Math.pow(y - yCenter, 2) ) < Math.pow(radius, 2);
        if (isPressed != areaPressed) {
            isPressed = areaPressed && interactionEnabled;
            this.invalidate();
        }
    }

    public void setPumpState(boolean pumpState) {
        if (pumpState != this.pumpState) {
            this.pumpState = pumpState;
            this.interactionEnabled = this.pumpInteractionEnabled || pumpState;
            this.invalidate();
        }
    }

    // Override the enableInteraction variable, so the pump can always be shutdown (but not turned
    // on) even when interaction is disabled.

    @Override
    public void enableInteraction(boolean value) {
        this.pumpInteractionEnabled = value;
        this.interactionEnabled = value || pumpState;
    }

}
