package bionic.engineering.robotinocontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class GyroVisualizer extends View {
    private static final float RADIANS_TO_DEGREES = (float) (180d / Math.PI);
    private static final float RADIUS = 250;
    private Paint mGyroPaint = new Paint();
    private Paint mGyroPaintY = new Paint();
    private float mX, mY, mZ;

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
        mGyroPaint.setStrokeWidth(10);
        mGyroPaint.setAntiAlias(true);
        
        mGyroPaintY.setColor(0x7700FF00);
        mGyroPaintY.setStyle(Style.STROKE);
        mGyroPaintY.setStrokeWidth(10);
        mGyroPaintY.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float midX = getWidth() / 2f;		//Finds center of phonescreen
        float midY = getHeight() / 2f;

        // Gyroscope
        canvas.save();
        canvas.rotate(mZ * RADIANS_TO_DEGREES, midX, midY);
        canvas.drawLine(midX, midY, midX, midY - RADIUS, mGyroPaintY);	
        canvas.drawLine(midX, midY, midX, midY + RADIUS, mGyroPaint);
        canvas.drawLine(midX - RADIUS, midY, midX + RADIUS, midY, mGyroPaint);
        canvas.drawCircle(midX, midY, RADIUS, mGyroPaint);
        canvas.restore();

        canvas.drawCircle(midX + mY * 350, midY + mX * 350, 10, mGyroPaint);
        invalidate();
    }

    public void setGyroRotation(float x, float y, float z) {
        mX = x;
        mY = y;
        mZ = z;
    }
}
