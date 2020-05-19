package com.cnam.greta.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import com.cnam.greta.R;

public class AltimeterView extends View {

    private final static String ALTITUDE_KEY = "altitude";
    private final static String INSTANCE_KEY = "instanceState";

    private Paint mTextPaint, mMainLinePaint, mSecondaryLinePaint, mTerciaryLinePaint, mMarkerPaint;
    private Path pathMarker;

    private int mTextColor, mBackgroundColor, mLineColor, mMarkerColor;
    private float mAltitude, mTextSize, mRange;
    private boolean mShowMarker;

    private float minAltitudeValue;
    private float maxAltitudeValue;

    private int[] colors;
    private float[] hsv;
    private GradientDrawable gradientDrawable;


    public AltimeterView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AltimeterView, 0, 0);

        mBackgroundColor = a.getColor(R.styleable.AltimeterView_backgroundColor, Color.BLACK);
        mMarkerColor = a.getColor(R.styleable.AltimeterView_markerColor, Color.RED);
        mShowMarker = a.getBoolean(R.styleable.AltimeterView_showMarker, true);
        mLineColor = a.getColor(R.styleable.AltimeterView_lineColor, Color.WHITE);
        mTextColor = a.getColor(R.styleable.AltimeterView_textColor, Color.WHITE);
        mTextSize = a.getDimension(R.styleable.AltimeterView_textSize, 15 * getResources().getDisplayMetrics().scaledDensity);
        mAltitude = a.getFloat(R.styleable.AltimeterView_meters, 0);
        mRange = a.getFloat(R.styleable.AltimeterView_rangeMeters, 500);
        minAltitudeValue = a.getFloat(R.styleable.AltimeterView_minAltitudeValue, 0);
        maxAltitudeValue = a.getFloat(R.styleable.AltimeterView_maxAltitudeValue, 4000);
        a.recycle();
        init();
    }

    private void init() {
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mMainLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMainLinePaint.setStrokeWidth(8f);

        mSecondaryLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSecondaryLinePaint.setStrokeWidth(6f);

        mTerciaryLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTerciaryLinePaint.setStrokeWidth(3f);

        mMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMarkerPaint.setStyle(Paint.Style.FILL);
        pathMarker = new Path();

        colors = new int[(int) (mRange / 25) + 1];
        hsv = new float[3];
        gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle b = new Bundle();
        b.putParcelable(INSTANCE_KEY, super.onSaveInstanceState());
        b.putFloat(ALTITUDE_KEY, mAltitude);
        return b;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle b = (Bundle) state;
            mAltitude = b.getFloat(ALTITUDE_KEY, 0);
            state = b.getParcelable(INSTANCE_KEY);
        }
        super.onRestoreInstanceState(state);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        int minWidth = (int) Math.floor(50 * getResources().getDisplayMetrics().density);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;

        } else {
            result = minWidth + getPaddingLeft() + getPaddingRight();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }

        return result;
    }

    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        int minHeight = (int) Math.floor(30 * getResources().getDisplayMetrics().density);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;

        } else {
            result = minHeight + getPaddingTop() + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(mTextSize);

        mMainLinePaint.setColor(mLineColor);
        mSecondaryLinePaint.setColor(mLineColor);
        mTerciaryLinePaint.setColor(mLineColor);

        mMarkerPaint.setColor(mMarkerColor);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();

        int unitHeight = (height - paddingTop - paddingBottom) / 12;

        float pixDeg = (width - paddingLeft - paddingRight) / mRange;

        int minAltitude = Math.round(mAltitude - mRange / 2);
        int maxAltitude = Math.round(mAltitude + mRange / 2);

        if (mShowMarker) {
            pathMarker.moveTo((float) width / 2, 3 * unitHeight + paddingTop);
            pathMarker.lineTo(((float) width / 2) + 20, paddingTop);
            pathMarker.lineTo(((float) width / 2) - 20, paddingTop);
            pathMarker.close();
            canvas.drawPath(pathMarker, mMarkerPaint);
        }

        for (int i = minAltitude, j = 0; i <= maxAltitude; i++) {
            if (i % 100 == 0) {
                canvas.drawText(String.valueOf(i), paddingLeft + pixDeg * (i - minAltitude), ((float) height / 2) + (mTextSize / 3), mTextPaint);
            }
            if(i % 25 == 0){
                canvas.drawLine(paddingLeft + pixDeg * (i - minAltitude), height - paddingBottom,
                        paddingLeft + pixDeg * (i - minAltitude), 10 * unitHeight + paddingTop,
                        mTerciaryLinePaint);

                hsv[0] = ((float) i/ (maxAltitudeValue - minAltitudeValue)) * 360;
                hsv[1] = 1.0f;
                hsv[2] = 0.8f;
                colors[j] = Color.HSVToColor(255, hsv);
                j++;
            }
        }
        gradientDrawable.setColors(colors);
        setBackground(gradientDrawable);
    }

    public void setAltitude(float meters) {
        if(mAltitude != meters){
            mAltitude = meters;
            invalidate();
            requestLayout();
        }
    }

    public float getmAltitude() {
        return mAltitude;
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        invalidate();
        requestLayout();
    }

    public void setLineColor(int color) {
        mLineColor = color;
        invalidate();
        requestLayout();
    }

    public void setMarkerColor(int color) {
        mMarkerColor = color;
        invalidate();
        requestLayout();
    }

    public void setTextColor(int color) {
        mTextColor = color;
        invalidate();
        requestLayout();
    }

    public void setShowMarker(boolean show) {
        mShowMarker = show;
        invalidate();
        requestLayout();
    }

    public void setTextSize(int size) {
        mTextSize = size;
        invalidate();
        requestLayout();
    }

    public void setRange(float range) {
        mRange = range;
        invalidate();
        requestLayout();
    }

    public void setMinAltitudeValue(float minAltitudeValue) {
        this.minAltitudeValue = minAltitudeValue;
    }

    public void setMaxAltitudeValue(float maxAltitudeValue) {
        this.maxAltitudeValue = maxAltitudeValue;
    }
}