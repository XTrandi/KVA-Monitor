package de.tu_dresden.et.kva_monitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.view.View;

public abstract class FieldDeviceView extends View {
    static final int STROKE_WIDTH           = 3;
    static final int STROKE_WIDTH_AMBIENT   = 1;

    static final float ANALOG_SIZE_SCALED   = 0.9f;

    protected Paint textPaint;
    protected String text;

    protected ShapeDrawable inputFeedback;
    protected Paint inputFeedbackPaint;
    private ShapeDrawable shapeAnalogHolder;
    private ShapeDrawable shapeAnalogDisplay;

    protected int coordLeft, coordTop, coordRight, coordBottom;
    protected boolean isPressed;

    protected boolean ambientMode = false;
    protected boolean interactionEnabled = true;
    private boolean displayAnalogValue;
    private int analogValue;


    public FieldDeviceView(Context context) {
        super(context);
        init();
    }

    public FieldDeviceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        inputFeedbackPaint = new Paint();
        inputFeedbackPaint.setColor(Color.GRAY);
        inputFeedbackPaint.setAlpha(100);

        Path pathAnalogLines = new Path();
        pathAnalogLines.moveTo(0, 50);
        pathAnalogLines.lineTo(100, 0);
        pathAnalogLines.lineTo(100, 50);
        pathAnalogLines.lineTo(0, 50);

        shapeAnalogHolder = new ShapeDrawable( new PathShape(pathAnalogLines, 100, 50) );
        shapeAnalogDisplay = new ShapeDrawable( new PathShape(pathAnalogLines, 100, 50) );

        // This line is not needed since it is advised in subclasses to be called at a later stage anyway
        // this.onExitAmbient();

    }

    public abstract void setPressed(float x, float y);

    public boolean getPressed() {
        if (isPressed && interactionEnabled) {
            isPressed = false;
            this.invalidate();
            return true;
        }
        else {
            return false;
        }
    }

    public void setTextPaint(Paint paint) {
        this.textPaint = paint;
    }

    public void setText(String text) {
        if ( ! text.equals(this.text) ) {
            this.text = text;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (text != null) {
            canvas.drawText(text,
                    getWidth()/2,coordTop - 4 * getResources().getDisplayMetrics().density,
                    textPaint);
        }

        if (isPressed && interactionEnabled) {
            inputFeedback.draw(canvas);
        }

        if (displayAnalogValue) {
            shapeAnalogDisplay.draw(canvas);
            shapeAnalogHolder.draw(canvas);
        }
    }

    /**
     * When overriding this method, call the super callback at the end (after defining all paints)
     */
    public void onEnterAmbient() {
        setAmbientPaint( shapeAnalogHolder.getPaint() );
        setAmbientPaint( shapeAnalogDisplay.getPaint() );

        this.ambientMode = true;
        this.invalidate();
    }
    /**
     * When overriding this method, call the super callback at the end (after defining all paints)
     */
    public void onExitAmbient() {
        setStandardActivePaint( shapeAnalogHolder.getPaint() );
        Paint paint = shapeAnalogDisplay.getPaint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);

        this.ambientMode = false;
        this.invalidate();
    }
    public abstract void onUpdateAmbient();

    protected void setAmbientPaint(Paint paint) {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_WIDTH_AMBIENT);
        paint.setAntiAlias(false);
    }

    protected void setStandardActivePaint(Paint paint) {
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setAntiAlias(true);
    }

    public void enableInteraction(boolean value) {
        this.interactionEnabled = value;
    }

    public void setAnalogValue(int analogValue) {
        if (this.analogValue != analogValue) {
            this.analogValue = analogValue;

            int left, bottom;
            int size = (int) (ANALOG_SIZE_SCALED * getWidth() * analogValue / 100);

            Rect rect = shapeAnalogHolder.getBounds();

            left = rect.left;
            bottom = rect.bottom;

            shapeAnalogDisplay.setBounds(left, bottom - size/2, left + size, bottom);
            this.invalidate();
        }
    }

    public void displayAnalogValue(boolean value) {
        this.displayAnalogValue = value;
        if (value) {
            int width = this.getWidth();
            float size = ANALOG_SIZE_SCALED * getWidth();
            int top = coordBottom + (int) (4 * getResources().getDisplayMetrics().density);

            shapeAnalogHolder.setBounds(
                    (int) ( (width - size) / 2 ),
                    top,
                    (int) ( (width + size) / 2 ),
                    top + (int) (size / 2) );
        }
    }
}
