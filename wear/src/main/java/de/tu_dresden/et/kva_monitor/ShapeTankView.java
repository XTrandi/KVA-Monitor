package de.tu_dresden.et.kva_monitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
/**
 * View to display a tank on the UI
 */
public class ShapeTankView extends FieldDeviceView {

    /**
     * width:height ratio of the tank
     */
    private static final int TANK_WIDTH_RATIO   = 3;
    private static final int TANK_HEIGHT_RATIO  = 5;

    /**
     * UI elements
     */
    private ShapeDrawable shapeContainer;
    private ShapeDrawable shapeWaterLevel;
    private int maxWaterLevel;
    private int waterLevel;

    public ShapeTankView(Context context) {
        super(context);
        InitShapeTankView();
    }

    public ShapeTankView(Context context, AttributeSet attrs){
        super(context, attrs);
        InitShapeTankView();
    }

    // constructor
    private void InitShapeTankView() {
        shapeContainer  = new ShapeDrawable( new RectShape() );
        shapeWaterLevel = new ShapeDrawable( new RectShape() );
        inputFeedback = new ShapeDrawable( new RectShape() );
        inputFeedback.getPaint().set(inputFeedbackPaint);

        // Define paints
        onExitAmbient();
    }

    /**
     * Positions the tank.  Call this method fter the UI thread finished inflating the parent
     * fragment.
     */
    @Override
    public void resize() {
        int width  = this.getWidth();
        int height = this.getHeight();
        int containerHeight = width * TANK_HEIGHT_RATIO / TANK_WIDTH_RATIO;

        coordLeft   = STROKE_WIDTH/2;
        coordRight  = width - STROKE_WIDTH/2;
        coordTop    = (height - containerHeight) / 2;
        coordBottom = (height + containerHeight) / 2;

        shapeContainer.setBounds(coordLeft, coordTop, coordRight, coordBottom);

        inputFeedback.setBounds(coordLeft, coordTop, coordRight, coordBottom);

        setWaterLevel(this.waterLevel, true);

        super.resize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        shapeWaterLevel.draw(canvas);
        shapeContainer.draw(canvas);

        super.onDraw(canvas);
    }

    @Override
    public void onEnterAmbient() {
        setAmbientPaint( shapeWaterLevel.getPaint() );
        setAmbientPaint( shapeContainer.getPaint() );

        super.onEnterAmbient();
    }

    @Override
    public void onExitAmbient() {
        setStandardActivePaint( shapeContainer.getPaint() );

        Paint paint = shapeWaterLevel.getPaint();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        super.onExitAmbient();
    }

    @Override
    public void onUpdateAmbient() {

    }

    public void setMaxWaterLevel(int maxWaterLevel) {
        this.maxWaterLevel = maxWaterLevel;
    }

    /**
     * Sets the water level, scaled to the maxWaterLevel
     * @param waterLevel
     */
    public void setWaterLevel(int waterLevel) {
        setWaterLevel(waterLevel, false);
    }

    private void setWaterLevel(int waterLevel, boolean forceRedraw) {
        if ( waterLevel != this.waterLevel || forceRedraw ) {
            this.waterLevel = waterLevel;

            // avoid exception when layout has not been inflated yet
            if (coordBottom - coordTop == 0) { return; }

            int waterHeight = (coordBottom - coordTop) * waterLevel / maxWaterLevel;
            shapeWaterLevel.setBounds(
                    coordLeft,
                    coordBottom - waterHeight,
                    coordRight,
                    coordBottom);

            this.invalidate();
        }
    }

    /**
     * View was touched. Check whether the interactive area was touched
     * @param x horizontal coordinate
     * @param y vertical coordinate
     */
    public void setPressed(float x, float y) {
        boolean areaPressed = (
                coordLeft <= x && x <= coordRight &&
                coordTop  <= y && y <= coordBottom);

        if (isPressed != areaPressed) {
            isPressed = areaPressed;
            this.invalidate();
        }
    }

}
