package bionic.engineering.robotinocontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;

public class GyroVisualizer extends View {
    private static final float RADIANS_TO_DEGREES = (float) (180d / Math.PI);
    
    private Paint mGyroPaint, mGyroPaintY;
    private float mX, mY, mZ, mRadius;

    public GyroVisualizer(Context context){
    	super(context, null, 0);
        mGyroPaint = new Paint();
        mGyroPaintY = new Paint();
        
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
        mRadius = midY - 5f;	// Height of the circle

        // Gyroscope
        canvas.save();
        canvas.rotate(mZ * RADIANS_TO_DEGREES, midX, midY);
        canvas.drawLine(midX, midY, midX, midY - mRadius, mGyroPaintY);	
        canvas.drawLine(midX, midY, midX, midY + mRadius, mGyroPaint);
        canvas.drawLine(midX - mRadius, midY, midX + mRadius, midY, mGyroPaint);
        canvas.drawCircle(midX, midY, mRadius, mGyroPaint);
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
