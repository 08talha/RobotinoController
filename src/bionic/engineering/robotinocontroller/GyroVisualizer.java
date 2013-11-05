package bionic.engineering.robotinocontroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

/*
 * Copyright (C) 2011 Adam NybŠck
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class GyroVisualizer extends View {
    private static final float RADIANS_TO_DEGREES = (float) (180d / Math.PI);
    private static final float RADIUS = 150;
    private Paint mGyroPaint = new Paint();
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
        mGyroPaint.setStrokeWidth(5);
        mGyroPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float midX = getWidth() / 2f;		//Finds center of phonescreen
        float midY = getHeight() / 2f;

        // Gyroscope
        canvas.save();
        canvas.rotate(mZ * RADIANS_TO_DEGREES, midX, midY);
        canvas.drawLine(midX, midY - RADIUS, midX, midY + RADIUS, mGyroPaint);
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
