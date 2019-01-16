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

/**
 * Generalised view for any field devices. This class provides a short textual description above the
 * placeholding field device and an analog display from 0... 100 % that can be turned on or off.
 * When using this in layout resources they are limited to it's width and will be placed mid
 * vertically.
 */
public abstract class FieldDeviceView extends View {
    /**
     * Stroke width for all shapes in active and ambient mode
     */
    static final int STROKE_WIDTH           = 3;
    static final int STROKE_WIDTH_AMBIENT   = 1;

    /**
     * Ratio of the analog view to scale within the derived field device's width
     */
    static final float ANALOG_SIZE_SCALED   = 0.9f;

    /**
     * UI elements for text and analog view
     */
    protected Paint textPaint;
    protected String text;

    protected ShapeDrawable inputFeedback;
    protected Paint inputFeedbackPaint;

    private ShapeDrawable shapeAnalogHolder;    // static drawable
    private ShapeDrawable shapeAnalogDisplay;   // dynamic drawable according to the analog value

    /**
     * Coordinates for the field devices. These have to be defined in the inherited class. They are
     * used to determine placement of text and analog view.
     */
    protected int coordLeft, coordTop, coordRight, coordBottom;

    /**
     * indicating whether the interaction area is pressed
     */
    protected boolean isPressed;
    /**
     * Indicating ambient mode. This variable CAN be overridden by subclasses if special behaviour
     * is required.
     */
    protected boolean ambientMode = false;
    /**
     * Condition for highlighting background when the interaction area is pressed.
     */
    protected boolean interactionEnabled = true;
    /**
     * Condition whether to show the analog value and the value itself.
     */
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

    // constructor
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
    }

    public abstract void setPressed(float x, float y);

    /**
     * Method to determine whether the user is still in the interaction area.
     * @return Corresponding action should be invoked.
     */
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

        // text is displayed 4dp above field device

        // draw text
        if (text != null) {
            canvas.drawText(text,
                    getWidth()/2,coordTop - 4 * getResources().getDisplayMetrics().density,
                    textPaint);
        }

        // draw background highlight similar to a "button"
        if (isPressed && interactionEnabled) {
            inputFeedback.draw(canvas);
        }

        // draw analog value as triangle
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

    /**
     * General method to alter any view's, drawable's, text's paint to follow ambient theme
     * @param paint The object whose properties are applied on
     */
    protected void setAmbientPaint(Paint paint) {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_WIDTH_AMBIENT);
        paint.setAntiAlias(false);
    }
    /**
     * General method to alter any view's, drawable's, text's paint to follow standard theme
     * @param paint The object whose properties are applied on
     */
    protected void setStandardActivePaint(Paint paint) {
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setAntiAlias(true);
    }

    public void enableInteraction(boolean value) {
        this.interactionEnabled = value;
    }

    /**
     * Positions the analog display.
     * Call this method after inflating a fragment, so that its views' heights and widths were set.
     */
    public void resize() {
        int width = this.getWidth();
        float size = ANALOG_SIZE_SCALED * getWidth(); // max width of the analog display
        int top = coordBottom + (int) (4 * getResources().getDisplayMetrics().density);

        shapeAnalogHolder.setBounds(
                (int) ( (width - size) / 2 ),
                top,
                (int) ( (width + size) / 2 ),
                top + (int) (size / 2) );

        int left, bottom;
        // width of the analog value (fill area)
        int sizeForValue = (int) (ANALOG_SIZE_SCALED * getWidth() * analogValue / 100);

        Rect rect = shapeAnalogHolder.getBounds();

        left = rect.left;
        bottom = rect.bottom;

        shapeAnalogDisplay.setBounds(
                left,
                bottom - sizeForValue/2,
                left + sizeForValue,
                bottom);

        this.invalidate();
    }

    /**
     * Fills the analog value from left to right according to the input.s
     * @param analogValue 0... 100, unit %
     */
    public void setAnalogValue(int analogValue) {
        if (this.analogValue != analogValue) {
            this.analogValue = analogValue;

            Rect rect = shapeAnalogHolder.getBounds();
            int left, bottom;

            left = rect.left;
            bottom = rect.bottom;

            int sizeForValue = (rect.right - left) * analogValue / 100;

            shapeAnalogDisplay.setBounds(
                    left,
                    bottom - sizeForValue/2,
                    left + sizeForValue,
                    bottom);

            this.invalidate();
        }
    }

    /**
     * Whether to show the analog value
     * @param value
     */
    public void displayAnalogValue(boolean value) {
        if (displayAnalogValue != value) {
            this.displayAnalogValue = value;
            this.invalidate();
        }
    }
}
