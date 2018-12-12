package de.tu_dresden.et.kva_monitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;


public class PipeView extends FieldDeviceView {

    private static int PIPE_RADIUS_PX = 5;

    private ShapeDrawable shapeWater;
    private Paint paintPipe;

    private boolean state = false;

    public PipeView(Context context) {
        super(context);
        InitPipeView();
    }

    public PipeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        InitPipeView();
    }

    // here unused
    @Override
    public void setPressed(float x, float y) { }

    private void InitPipeView() {
        Path pathPipeLines = new Path();
        pathPipeLines.moveTo(0, 0);
        pathPipeLines.lineTo(100, 0);
        pathPipeLines.moveTo(0, 100);
        pathPipeLines.lineTo(100, 100);

        paintPipe = new Paint();

        shapeWater = new ShapeDrawable( new RectShape() );
        shapeWater.getPaint().setColor(Color.BLUE);

        onExitAmbient();
    }

    public void resize(float yCenter) {
        int width = this.getWidth();
        int height = this.getHeight();

        coordLeft   = 0;
        coordRight  = width;
        coordTop    = (int) (height * yCenter - PIPE_RADIUS_PX);
        coordBottom = (int) (height * yCenter + PIPE_RADIUS_PX);

        shapeWater.setBounds(coordLeft, coordTop, coordRight, coordBottom);
        this.invalidate();
    }

    public void setState(boolean state) {
        if ( this.state != state) {
            this.state = state;
            this.invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if ( !ambientMode && state) { shapeWater.draw(canvas); }
        canvas.drawLine(coordLeft, coordTop, coordRight, coordTop, paintPipe);
        canvas.drawLine(coordLeft, coordBottom, coordRight, coordBottom, paintPipe);
    }

    @Override
    public void onEnterAmbient() {
        setAmbientPaint(paintPipe);

        super.onEnterAmbient();
    }

    @Override
    public void onExitAmbient() {
        setStandardActivePaint( paintPipe );

        super.onExitAmbient();
    }

    @Override
    public void onUpdateAmbient() {

    }
}
