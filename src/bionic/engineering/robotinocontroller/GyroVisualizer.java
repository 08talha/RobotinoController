package bionic.engineering.robotinocontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class GyroVisualizer extends View {

    private static final float RADIANS_TO_DEGREES = (float) (180d / Math.PI);
    private static final float RADIUS = 150;

    private Paint mGyroPaint = new Paint();

    private float mGyroRotationX, mGyroRotationY, mGyroRotationZ;

    public GyroVisualizer(Context context) {
        this(context, null);
    }

    public GyroVisualizer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GyroVisualizer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mGyroPaint.setColor(0x77000000);
        mGyroPaint.setStyle(Style.STROKE);
        mGyroPaint.setStrokeWidth(5);
        mGyroPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        

        float midX = getWidth() / 2f;
        float midY = getHeight() / 2f;

        // Gyroscope
        canvas.save();
        canvas.rotate(mGyroRotationZ * RADIANS_TO_DEGREES, midX, midY);
        canvas.drawLine(midX, midY - RADIUS, midX, midY + RADIUS, mGyroPaint);
        canvas.drawLine(midX - RADIUS, midY, midX + RADIUS, midY, mGyroPaint);
        canvas.drawCircle(midX, midY, RADIUS, mGyroPaint);
        canvas.restore();

        canvas.drawCircle(midX + mGyroRotationY * 350, midY + mGyroRotationX * 350, 10, mGyroPaint);
        invalidate();
    }

    public void setGyroRotation(float x, float y, float z) {
        mGyroRotationX = x;
        mGyroRotationY = y;
        mGyroRotationZ = z;
    }
}
