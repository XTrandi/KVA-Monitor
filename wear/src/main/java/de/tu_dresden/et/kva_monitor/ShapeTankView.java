package de.tu_dresden.et.kva_monitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.view.View;

public class ShapeTankView extends FieldDeviceView {

    private static final int TANK_WIDTH_RATIO   = 3;
    private static final int TANK_HEIGHT_RATIO  = 5;

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

    // generalised constructor
    private void InitShapeTankView() {
        shapeContainer  = new ShapeDrawable( new RectShape() );
        shapeWaterLevel = new ShapeDrawable( new RectShape() );
        inputFeedback = new ShapeDrawable( new RectShape() );
        onExitAmbient();
    }

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

    public void setWaterLevel(int waterLevel) {
        setWaterLevel(waterLevel, false);
    }

    private void setWaterLevel(int waterLevel, boolean forceRedraw) {
        if ( waterLevel != this.waterLevel || forceRedraw ) {
            this.waterLevel = waterLevel;

            if (coordBottom - coordTop == 0) { return; } // ToDo: why was this statement necessary?

            int waterHeight = (coordBottom - coordTop) * waterLevel / maxWaterLevel;
            shapeWaterLevel.setBounds(
                    coordLeft,
                    coordBottom - waterHeight,
                    coordRight,
                    coordBottom);
            this.invalidate(); // update / refresh this view
        }
    }

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
