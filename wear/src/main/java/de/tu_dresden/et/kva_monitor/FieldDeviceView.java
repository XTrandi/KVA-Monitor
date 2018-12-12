package de.tu_dresden.et.kva_monitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.util.AttributeSet;
import android.view.View;

public abstract class FieldDeviceView extends View {
    static final int STROKE_WIDTH           = 3;
    static final int STROKE_WIDTH_AMBIENT   = 1;

    protected Paint textPaint;
    protected String text;

    protected ShapeDrawable inputFeedback;
    private Paint inputFeedbackPaint;

    protected int coordLeft, coordTop, coordRight, coordBottom;
    protected boolean isPressed;

    protected boolean ambientMode = false;

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
        // ToDo: set brightness
    }

    public abstract void setPressed(float x, float y);

    public boolean getPressed() {
        if (isPressed) {
            isPressed = false;
            this.invalidate();
            return true;
        }
        else {
            return false;
        }
    }

    // copy method for transfering paint probably (?) not necessary
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

        if (isPressed && inputFeedback!=null) {
            inputFeedback.getPaint().set(inputFeedbackPaint); // ToDo: put this statement somewhere elegant
            inputFeedback.draw(canvas);
        }
    }

    /**
     * When overriding this method, call the super callback at the end (after defining all paints)
     */
    public void onEnterAmbient() {
        this.ambientMode = true;
        this.invalidate();
    }
    /**
     * When overriding this method, call the super callback at the end (after defining all paints)
     */
    public void onExitAmbient() {
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

}
