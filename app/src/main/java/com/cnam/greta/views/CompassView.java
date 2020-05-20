package com.cnam.greta.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.cnam.greta.R;
import com.cnam.greta.data.entities.UserPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Map;

public class CompassView extends View {

    private Paint mTextPaint, mMainLinePaint, mSecondaryLinePaint, mTertiaryLinePaint;

    private int mTextColor, mBackgroundColor, mLineColor;
    private float mDegrees, mTextSize, mRangeDegrees;

    private UserPosition myPosition;
    private HashMap<String, UserPosition> userLocations;
    private int[] userDirections;
    private String[] userNames;

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CompassView, 0, 0);

        mBackgroundColor = a.getColor(R.styleable.Application_backgroundColor, Color.TRANSPARENT);
        mLineColor = a.getColor(R.styleable.Application_lineColor, Color.WHITE);
        mTextColor = a.getColor(R.styleable.Application_textColor, Color.WHITE);
        mTextSize = a.getDimension(R.styleable.Application_textSize, 15 * getResources().getDisplayMetrics().scaledDensity);
        mDegrees = a.getFloat(R.styleable.CompassView_degrees, 0);
        mRangeDegrees = a.getFloat(R.styleable.CompassView_rangeDegrees, 60.0f);

        a.recycle();

        checkValues();
        init();
    }

    private void checkValues() {
        if ((mDegrees < 0) || (mDegrees > 359))
            throw new IndexOutOfBoundsException(getResources()
                    .getString(R.string.out_index_degrees));
    }

    private void init() {
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mMainLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMainLinePaint.setStrokeWidth(8f);

        mSecondaryLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSecondaryLinePaint.setStrokeWidth(6f);

        mTertiaryLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTertiaryLinePaint.setStrokeWidth(3f);

        userLocations = new HashMap<>();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle b = new Bundle();
        b.putParcelable("instanceState", super.onSaveInstanceState());
        b.putFloat("degrees", mDegrees);

        return b;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle b = (Bundle) state;
            mDegrees = b.getFloat("degrees", 0);

            state = b.getParcelable("instanceState");
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
        mTertiaryLinePaint.setColor(mLineColor);

        canvas.drawColor(mBackgroundColor);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();

        int unitHeight = (height - paddingTop - paddingBottom) / 12;

        float pixDeg = (width - paddingLeft - paddingRight) / mRangeDegrees;

        int minDegrees = Math.round(mDegrees - mRangeDegrees / 2);
        int maxDegrees = Math.round(mDegrees + mRangeDegrees / 2);

        for (int i = -180; i < 540; i += 1) {
            if ((i >= minDegrees) && (i <= maxDegrees)) {

                if(i % 15 == 0){
                    canvas.drawLine(paddingLeft + pixDeg * (i - minDegrees), height - paddingBottom,
                            paddingLeft + pixDeg * (i - minDegrees), 10 * unitHeight + paddingTop,
                            mTertiaryLinePaint);
                }

                if (i % 45 == 0) {
                    canvas.drawLine(paddingLeft + pixDeg * (i - minDegrees),
                            height - paddingBottom, paddingLeft + pixDeg * (i - minDegrees),
                            8 * unitHeight + paddingTop, mSecondaryLinePaint);
                }

                if (i % 90 == 0) {
                    canvas.drawLine(paddingLeft + pixDeg * (i - minDegrees),
                            height - paddingBottom, paddingLeft + pixDeg * (i - minDegrees),
                            6 * unitHeight + paddingTop, mMainLinePaint);

                    String coord = "";
                    switch (i) {
                        case -90:
                        case 270:
                            coord = getResources().getString(R.string.compass_west);
                            break;

                        case 0:
                        case 360:
                            coord = getResources().getString(R.string.compass_north);
                            break;

                        case 90:
                        case 450:
                            coord = getResources().getString(R.string.compass_east);
                            break;

                        case -180:
                        case 180:
                            coord = getResources().getString(R.string.compass_south);
                            break;
                    }

                    canvas.drawText(coord, paddingLeft + pixDeg * (i - minDegrees), 5 * unitHeight + paddingTop, mTextPaint);
                }
            }

            if(userDirections != null && userNames != null){
                for (int j = 0; j < userDirections.length; j++){
                    if(i == userDirections[j]){
                        canvas.drawText(userNames[j], paddingLeft + pixDeg * (i - minDegrees), 5 * unitHeight + paddingTop, mTextPaint);
                    }
                    if(i == userDirections[j] + 360){
                        canvas.drawText(userNames[j], paddingLeft + pixDeg * (i - minDegrees + 360), 5 * unitHeight + paddingTop, mTextPaint);
                    }
                }
            }

        }
    }

    public void setDegrees(float degrees) {
        if ((mDegrees < 0) || (mDegrees >= 360))
            throw new IndexOutOfBoundsException(getResources()
                    .getString(R.string.out_index_degrees) + mDegrees);

        if((mDegrees - degrees > 10) || (mDegrees - degrees < - 10)){
            mDegrees = degrees;
            invalidate();
            requestLayout();
        }
    }

    private float computeBearing(UserPosition from, UserPosition to){
        double longitudeDelta = from.getLongitude() - to.getLongitude();
        double x = Math.cos(to.getLatitude()) * Math.sin(longitudeDelta);
        double y = (Math.cos(from.getLatitude()) * Math.sin(to.getLatitude())) - (Math.sin(from.getLatitude()) * Math.cos(to.getLatitude()) * Math.cos(longitudeDelta));
        double radian = Math.atan2(x, y);
        return (float) (Math.toDegrees(radian) + 360) % 360;
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

    public void setTextColor(int color) {
        mTextColor = color;
        invalidate();
        requestLayout();
    }

    public void setTextSize(int size) {
        mTextSize = size;
        invalidate();
        requestLayout();
    }

    public void setRangeDegrees(float range) {
        if (mRangeDegrees > 360)
            throw new IndexOutOfBoundsException(getResources().getString(
                    R.string.out_index_range_degrees)
                    + mRangeDegrees);

        mRangeDegrees = range;
        invalidate();
        requestLayout();
    }

    public void setLocation(UserPosition myPosition){
        this.myPosition = myPosition;
        reComputeDirections();
        invalidate();
        requestLayout();
    }

    public void addUserLocation(String hashId, UserPosition userPosition){
        userLocations.put(hashId, userPosition);
        reComputeDirections();
        invalidate();
        requestLayout();
    }

    public void removeUser(String key) {
        userLocations.remove(key);
        reComputeDirections();
        invalidate();
        requestLayout();
    }

    private void reComputeDirections(){
        if(myPosition != null && userLocations != null && userLocations.size() != 0){
            userDirections = new int[userLocations.size()];
            userNames = new String[userLocations.size()];
            int i = 0;
            for (Map.Entry<String, UserPosition> userLocation : userLocations.entrySet()){
                userDirections[i] = (int) computeBearing(myPosition, userLocation.getValue());
                userNames[i] = userLocation.getValue().getUsername();
                i++;
            }
        }
    }
}