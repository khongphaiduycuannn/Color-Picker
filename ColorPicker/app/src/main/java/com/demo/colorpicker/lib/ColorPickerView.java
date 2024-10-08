package com.demo.colorpicker.lib;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {

    private final static int DEFAULT_BORDER_COLOR = 0xFF6E6E6E;
    private final static int DEFAULT_SLIDER_COLOR = 0xFFBDBDBD;

    private final static int HUE_PANEL_HEIGHT_DP = 10;
    private final static int ALPHA_PANEL_HEIGHT_DP = 10;
    private final static int PANEL_SPACING_DP = 10;
    private final static int CIRCLE_TRACKER_RADIUS_DP = 5;
    private final static int SLIDER_TRACKER_SIZE_DP = 6;
    private final static int SLIDER_TRACKER_OFFSET_DP = 2;

    private final static int BORDER_WIDTH_PX = 1;

    private int huePanelHeightPx;

    private int alphaPanelHeightPx;

    private int panelSpacingPx;

    private int circleTrackerRadiusPx;

    private int sliderTrackerOffsetPx;

    private int sliderTrackerSizePx;

    private Paint satValPaint;
    private Paint satValTrackerPaint;

    private Paint alphaPaint;
    private Paint alphaTextPaint;
    private Paint hueAlphaTrackerPaint;
    private Paint hueAlphaTrackerFillPaint;
    private Paint hueAlphaTrackerShadowPaint;

    private Paint borderPaint;

    private Shader valShader;
    private Shader satShader;
    private Shader alphaShader;

    private BitmapCache satValBackgroundCache;

    private BitmapCache hueBackgroundCache;

    private int alpha = 0xff;
    private float hue = 360f;
    private float sat = 0f;
    private float val = 0f;

    private boolean showAlphaPanel = true;
    private String alphaSliderText = null;
    private int sliderTrackerColor = DEFAULT_SLIDER_COLOR;
    private int borderColor = DEFAULT_BORDER_COLOR;

    private int mRequiredPadding;

    private Rect drawingRect;

    private Rect satValRect;
    private Rect hueRect;
    private Rect alphaRect;

    private Point startTouchPoint = null;

    private AlphaPatternDrawable alphaPatternDrawable;
    private OnColorChangedListener onColorChangedListener;

    public ColorPickerView(Context context) {
        this(context, null);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putParcelable("instanceState", super.onSaveInstanceState());
        state.putInt("alpha", alpha);
        state.putFloat("hue", hue);
        state.putFloat("sat", sat);
        state.putFloat("val", val);
        state.putBoolean("show_alpha", showAlphaPanel);
        state.putString("alpha_text", alphaSliderText);

        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {

        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;

            alpha = bundle.getInt("alpha");
            hue = bundle.getFloat("hue");
            sat = bundle.getFloat("sat");
            val = bundle.getFloat("val");
            showAlphaPanel = bundle.getBoolean("show_alpha");
            alphaSliderText = bundle.getString("alpha_text");

            state = bundle.getParcelable("instanceState");
        }
        super.onRestoreInstanceState(state);
    }

    private void init(Context context) {
        applyThemeColors(context);

        huePanelHeightPx = DrawingUtils.dpToPx(getContext(), HUE_PANEL_HEIGHT_DP);
        alphaPanelHeightPx = DrawingUtils.dpToPx(getContext(), ALPHA_PANEL_HEIGHT_DP);
        panelSpacingPx = DrawingUtils.dpToPx(getContext(), PANEL_SPACING_DP);
        circleTrackerRadiusPx = DrawingUtils.dpToPx(getContext(), CIRCLE_TRACKER_RADIUS_DP);
        sliderTrackerSizePx = DrawingUtils.dpToPx(getContext(), SLIDER_TRACKER_SIZE_DP);
        sliderTrackerOffsetPx = DrawingUtils.dpToPx(getContext(), SLIDER_TRACKER_OFFSET_DP);

        mRequiredPadding = DrawingUtils.dpToPx(getContext(), 6f);

        initPaintTools();

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    private void applyThemeColors(Context c) {

        final TypedValue value = new TypedValue();
        TypedArray a = c.obtainStyledAttributes(value.data, new int[]{android.R.attr.textColorSecondary});

        if (borderColor == DEFAULT_BORDER_COLOR) {
            borderColor = a.getColor(0, DEFAULT_BORDER_COLOR);
        }

        if (sliderTrackerColor == DEFAULT_SLIDER_COLOR) {
            sliderTrackerColor = a.getColor(0, DEFAULT_SLIDER_COLOR);
        }

        a.recycle();
    }

    private void initPaintTools() {

        satValPaint = new Paint();
        satValTrackerPaint = new Paint();
        hueAlphaTrackerPaint = new Paint();
        hueAlphaTrackerFillPaint = new Paint();
        hueAlphaTrackerShadowPaint = new Paint();
        alphaPaint = new Paint();
        alphaTextPaint = new Paint();
        borderPaint = new Paint();

        satValTrackerPaint.setStyle(Style.STROKE);
        satValTrackerPaint.setStrokeWidth(DrawingUtils.dpToPx(getContext(), 1.5f));
        satValTrackerPaint.setAntiAlias(true);

        hueAlphaTrackerPaint.setColor(sliderTrackerColor);
        hueAlphaTrackerPaint.setStyle(Style.FILL);

        hueAlphaTrackerFillPaint.setColor(Color.WHITE);
        hueAlphaTrackerFillPaint.setStyle(Style.STROKE);
        hueAlphaTrackerFillPaint.setStrokeWidth(DrawingUtils.dpToPx(getContext(), 1.5f));
        hueAlphaTrackerFillPaint.setAntiAlias(true);

        hueAlphaTrackerShadowPaint.setColor(Color.BLACK);
        hueAlphaTrackerShadowPaint.setStyle(Style.STROKE);
        hueAlphaTrackerShadowPaint.setStrokeWidth(DrawingUtils.dpToPx(getContext(), 1.5f));
        hueAlphaTrackerShadowPaint.setAntiAlias(true);
        hueAlphaTrackerShadowPaint.setMaskFilter(new BlurMaskFilter(
                DrawingUtils.dpToPx(getContext(), 2),
                BlurMaskFilter.Blur.OUTER));

        alphaTextPaint.setColor(0xff1c1c1c);
        alphaTextPaint.setTextSize(DrawingUtils.dpToPx(getContext(), 14));
        alphaTextPaint.setAntiAlias(true);
        alphaTextPaint.setTextAlign(Align.CENTER);
        alphaTextPaint.setFakeBoldText(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawingRect.width() <= 0 || drawingRect.height() <= 0) {
            return;
        }

        drawSatValPanel(canvas);
        drawHuePanel(canvas);
        drawAlphaPanel(canvas);
    }

    private void drawSatValPanel(Canvas canvas) {
        final Rect rect = satValRect;

        if (BORDER_WIDTH_PX > 0) {
            borderPaint.setColor(borderColor);
            canvas.drawRoundRect(drawingRect.left, drawingRect.top, rect.right + BORDER_WIDTH_PX, rect.bottom + BORDER_WIDTH_PX
                    , (float) huePanelHeightPx / 2, (float) huePanelHeightPx / 2, borderPaint);
        }

        if (valShader == null) {
            valShader =
                    new LinearGradient(rect.left, rect.top, rect.left, rect.bottom, 0xffffffff, 0xff000000, TileMode.CLAMP);
        }

        if (satValBackgroundCache == null || satValBackgroundCache.value != hue) {

            if (satValBackgroundCache == null) {
                satValBackgroundCache = new BitmapCache();
            }

            if (satValBackgroundCache.bitmap == null) {
                satValBackgroundCache.bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Config.ARGB_8888);
            }

            if (satValBackgroundCache.canvas == null) {
                satValBackgroundCache.canvas = new Canvas(satValBackgroundCache.bitmap);
            }

            int rgb = Color.HSVToColor(new float[]{hue, 1f, 1f});

            satShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top, 0xffffffff, rgb, TileMode.CLAMP);

            ComposeShader mShader = new ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY);
            satValPaint.setShader(mShader);

            satValBackgroundCache.canvas.drawRoundRect(0, 0, satValBackgroundCache.bitmap.getWidth(),
                    satValBackgroundCache.bitmap.getHeight(), (float) huePanelHeightPx / 2, (float) huePanelHeightPx / 2, satValPaint);

            satValBackgroundCache.value = hue;
        }

        canvas.drawBitmap(satValBackgroundCache.bitmap, null, rect, null);

        Point p = satValToPoint(sat, val);

        satValTrackerPaint.setColor(Color.WHITE);
        canvas.drawCircle(p.x, p.y, circleTrackerRadiusPx, hueAlphaTrackerShadowPaint);
        canvas.drawCircle(p.x, p.y, circleTrackerRadiusPx, satValTrackerPaint);
    }

    private void drawHuePanel(Canvas canvas) {
        final Rect rect = hueRect;

        if (BORDER_WIDTH_PX > 0) {
            borderPaint.setColor(borderColor);

            canvas.drawRoundRect(rect.left - BORDER_WIDTH_PX, rect.top - BORDER_WIDTH_PX, rect.right + BORDER_WIDTH_PX,
                    rect.bottom + BORDER_WIDTH_PX, huePanelHeightPx, huePanelHeightPx, borderPaint);
        }

        if (hueBackgroundCache == null) {
            hueBackgroundCache = new BitmapCache();
            hueBackgroundCache.bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Config.ARGB_8888);
            hueBackgroundCache.canvas = new Canvas(hueBackgroundCache.bitmap);

            int[] hueColors = new int[(int) (rect.width() + 0.5f)];
            float w = 360f;
            for (int i = 0; i < hueColors.length; i++) {
                hueColors[i] = Color.HSVToColor(new float[]{w, 1f, 1f});
                w -= 360f / hueColors.length;
            }

            Paint paint = new Paint();
            paint.setColor(borderColor);
            hueBackgroundCache.canvas.drawRoundRect(0, 0, hueBackgroundCache.bitmap.getWidth(), hueBackgroundCache.bitmap.getHeight()
                    , huePanelHeightPx, huePanelHeightPx, paint);

            Paint linePaint = new Paint();
            linePaint.setStrokeWidth(0);
            linePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            for (int i = 0; i < hueColors.length; i++) {
                linePaint.setColor(hueColors[i]);
                hueBackgroundCache.canvas.drawLine(i, 0, i, hueBackgroundCache.bitmap.getHeight(), linePaint);
            }
        }

        canvas.drawBitmap(hueBackgroundCache.bitmap, null, rect, null);

        Point p = hueToPoint(hue);
        float x = (float) p.x;
        float y = (float) (p.y + DrawingUtils.dpToPx(getContext(), HUE_PANEL_HEIGHT_DP) / 2);

        canvas.drawCircle(x, y, circleTrackerRadiusPx, hueAlphaTrackerShadowPaint);
        canvas.drawCircle(x, y, circleTrackerRadiusPx, hueAlphaTrackerPaint);
        canvas.drawCircle(x, y, circleTrackerRadiusPx, hueAlphaTrackerFillPaint);
    }

    private void drawAlphaPanel(Canvas canvas) {

        if (!showAlphaPanel || alphaRect == null || alphaPatternDrawable == null) return;

        final RectF rect = new RectF(alphaRect);

        if (BORDER_WIDTH_PX > 0) {
            borderPaint.setColor(borderColor);
            canvas.drawRoundRect(rect.left - BORDER_WIDTH_PX, rect.top - BORDER_WIDTH_PX, rect.right + BORDER_WIDTH_PX,
                    rect.bottom + BORDER_WIDTH_PX, alphaPanelHeightPx, alphaPanelHeightPx, borderPaint);
        }

        alphaPatternDrawable.draw(canvas);

        float[] hsv = new float[]{hue, sat, val};
        int color = Color.HSVToColor(hsv);
        int acolor = Color.HSVToColor(0, hsv);

        alphaShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top, color, acolor, TileMode.CLAMP);

        alphaPaint.setShader(alphaShader);

        canvas.drawRoundRect(rect, alphaPanelHeightPx, alphaPanelHeightPx, alphaPaint);

        if (alphaSliderText != null && !alphaSliderText.equals("")) {
            canvas.drawText(alphaSliderText, rect.centerX(), rect.centerY() + DrawingUtils.dpToPx(getContext(), 4),
                    alphaTextPaint);
        }

        Point p = alphaToPoint(alpha);
        float x = (float) p.x;
        float y = (float) (p.y + DrawingUtils.dpToPx(getContext(), HUE_PANEL_HEIGHT_DP) / 2);

        canvas.drawCircle(x, y, circleTrackerRadiusPx, hueAlphaTrackerShadowPaint);
        canvas.drawCircle(x, y, circleTrackerRadiusPx, hueAlphaTrackerPaint);
        canvas.drawCircle(x, y, circleTrackerRadiusPx, hueAlphaTrackerFillPaint);
    }

    private Point hueToPoint(float hue) {

        final Rect rect = hueRect;
        final float width = rect.width();

        Point p = new Point();

        p.x = (int) (width - (hue * width / 360f) + rect.left);
        p.y = rect.top;

        return p;
    }

    private Point satValToPoint(float sat, float val) {

        final Rect rect = satValRect;
        final float height = rect.height();
        final float width = rect.width();

        Point p = new Point();

        p.x = (int) (sat * width + rect.left);
        p.y = (int) ((1f - val) * height + rect.top);

        return p;
    }

    private Point alphaToPoint(int alpha) {

        final Rect rect = alphaRect;
        final float width = rect.width();

        Point p = new Point();

        p.x = (int) (width - (alpha * width / 0xff) + rect.left);
        p.y = rect.top;

        return p;
    }

    private float[] pointToSatVal(float x, float y) {

        final Rect rect = satValRect;
        float[] result = new float[2];

        float width = rect.width();
        float height = rect.height();

        if (x < rect.left) {
            x = 0f;
        } else if (x > rect.right) {
            x = width;
        } else {
            x = x - rect.left;
        }

        if (y < rect.top) {
            y = 0f;
        } else if (y > rect.bottom) {
            y = height;
        } else {
            y = y - rect.top;
        }

        result[0] = 1.f / width * x;
        result[1] = 1.f - (1.f / height * y);

        return result;
    }

    private float pointToHue(float x) {

        final Rect rect = hueRect;

        final int width = rect.width();

        if (x < rect.left) {
            x = 0;
        } else if (x > rect.right) {
            x = width;
        } else {
            x = x - rect.left;
        }

        return 360f - (x * 360f / width);
    }

    private int pointToAlpha(int x) {

        final Rect rect = alphaRect;
        final int width = rect.width();

        if (x < rect.left) {
            x = 0;
        } else if (x > rect.right) {
            x = width;
        } else {
            x = x - rect.left;
        }

        return 0xff - (x * 0xff / width);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean update = false;

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                startTouchPoint = new Point((int) event.getX(), (int) event.getY());
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_MOVE:
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_UP:
                startTouchPoint = null;
                update = moveTrackersIfNeeded(event);
                break;
        }

        if (update) {
            if (onColorChangedListener != null) {
                int color = Color.HSVToColor(alpha, new float[]{hue, sat, val});
                hueAlphaTrackerPaint.setColor(color);
                onColorChangedListener.onColorChanged(color);
            }
            invalidate();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean moveTrackersIfNeeded(MotionEvent event) {
        if (startTouchPoint == null) {
            return false;
        }

        boolean update = false;

        int startX = startTouchPoint.x;
        int startY = startTouchPoint.y;

        if (hueRect.contains(startX, startY)) {
            hue = pointToHue(event.getX());

            update = true;
        } else if (satValRect.contains(startX, startY)) {
            float[] result = pointToSatVal(event.getX(), event.getY());

            sat = result[0];
            val = result[1];

            update = true;
        } else if (alphaRect != null && alphaRect.contains(startX, startY)) {
            alpha = pointToAlpha((int) event.getX());

            update = true;
        }

        return update;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int finalWidth;
        int finalHeight;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int widthAllowed = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int heightAllowed = MeasureSpec.getSize(heightMeasureSpec) - getPaddingBottom() - getPaddingTop();

        if (widthMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.EXACTLY) {
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                int h = widthAllowed + panelSpacingPx + huePanelHeightPx;

                if (showAlphaPanel) {
                    h += panelSpacingPx + alphaPanelHeightPx;
                }

                if (h > heightAllowed) {
                    finalHeight = heightAllowed;
                } else {
                    finalHeight = h;
                }

                finalWidth = widthAllowed;
            } else if (heightMode == MeasureSpec.EXACTLY && widthMode != MeasureSpec.EXACTLY) {

                int w = (heightAllowed - panelSpacingPx - huePanelHeightPx);

                if (showAlphaPanel) {
                    w -= (panelSpacingPx + alphaPanelHeightPx);
                }

                if (w > widthAllowed) {
                    finalWidth = widthAllowed;
                } else {
                    finalWidth = w;
                }

                finalHeight = heightAllowed;
            } else {
                finalWidth = widthAllowed;
                finalHeight = heightAllowed;
            }
        } else {
            int widthNeeded = (heightAllowed - panelSpacingPx - huePanelHeightPx);

            int heightNeeded = (widthAllowed + panelSpacingPx + huePanelHeightPx);

            if (showAlphaPanel) {
                widthNeeded -= (panelSpacingPx + alphaPanelHeightPx);
                heightNeeded += panelSpacingPx + alphaPanelHeightPx;
            }

            boolean widthOk = false;
            boolean heightOk = false;

            if (widthNeeded <= widthAllowed) {
                widthOk = true;
            }

            if (heightNeeded <= heightAllowed) {
                heightOk = true;
            }

            if (widthOk && heightOk) {
                finalWidth = widthAllowed;
                finalHeight = heightNeeded;
            } else if (!heightOk && widthOk) {
                finalHeight = heightAllowed;
                finalWidth = widthNeeded;
            } else if (!widthOk && heightOk) {
                finalHeight = heightNeeded;
                finalWidth = widthAllowed;
            } else {
                finalHeight = heightAllowed;
                finalWidth = widthAllowed;
            }
        }

        setMeasuredDimension(finalWidth + getPaddingLeft() + getPaddingRight(),
                finalHeight + getPaddingTop() + getPaddingBottom());
    }

    private int getPreferredWidth() {
        int width = DrawingUtils.dpToPx(getContext(), 200);

        return (width + huePanelHeightPx + panelSpacingPx);
    }

    private int getPreferredHeight() {
        int height = DrawingUtils.dpToPx(getContext(), 200);

        if (showAlphaPanel) {
            height += panelSpacingPx + alphaPanelHeightPx;
        }
        return height;
    }

    @Override
    public int getPaddingTop() {
        return Math.max(super.getPaddingTop(), mRequiredPadding);
    }

    @Override
    public int getPaddingBottom() {
        return Math.max(super.getPaddingBottom(), mRequiredPadding);
    }

    @Override
    public int getPaddingLeft() {
        return Math.max(super.getPaddingLeft(), mRequiredPadding);
    }

    @Override
    public int getPaddingRight() {
        return Math.max(super.getPaddingRight(), mRequiredPadding);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        drawingRect = new Rect();
        drawingRect.left = getPaddingLeft();
        drawingRect.right = w - getPaddingRight();
        drawingRect.top = getPaddingTop();
        drawingRect.bottom = h - getPaddingBottom();

        valShader = null;
        satShader = null;
        alphaShader = null;

        satValBackgroundCache = null;
        hueBackgroundCache = null;

        setUpSatValRect();
        setUpHueRect();
        setUpAlphaRect();
    }

    private void setUpSatValRect() {
        final Rect dRect = drawingRect;

        int left = dRect.left + BORDER_WIDTH_PX;
        int top = dRect.top + BORDER_WIDTH_PX;
        int bottom = dRect.bottom - BORDER_WIDTH_PX - panelSpacingPx - huePanelHeightPx;
        int right = dRect.right - BORDER_WIDTH_PX;

        if (showAlphaPanel) {
            bottom -= (alphaPanelHeightPx + panelSpacingPx);
        }

        satValRect = new Rect(left, top, right, bottom);
    }

    private void setUpHueRect() {
        final Rect dRect = drawingRect;

        int left = dRect.left + BORDER_WIDTH_PX;
        int top = dRect.bottom - huePanelHeightPx + BORDER_WIDTH_PX;
        int bottom = dRect.bottom - BORDER_WIDTH_PX;
        int right = dRect.right - BORDER_WIDTH_PX;

        if (showAlphaPanel) {
            top -= (alphaPanelHeightPx + panelSpacingPx);
            bottom -= (alphaPanelHeightPx + panelSpacingPx);
        }

        hueRect = new Rect(left, top, right, bottom);
    }

    private void setUpAlphaRect() {

        if (!showAlphaPanel) return;

        final Rect dRect = drawingRect;

        int left = dRect.left + BORDER_WIDTH_PX;
        int top = dRect.bottom - alphaPanelHeightPx + BORDER_WIDTH_PX;
        int bottom = dRect.bottom - BORDER_WIDTH_PX;
        int right = dRect.right - BORDER_WIDTH_PX;

        alphaRect = new Rect(left, top, right, bottom);

        alphaPatternDrawable = new AlphaPatternDrawable(DrawingUtils.dpToPx(getContext(), 4));
        alphaPatternDrawable.setBounds(Math.round(alphaRect.left), Math.round(alphaRect.top), Math.round(alphaRect.right),
                Math.round(alphaRect.bottom));
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        onColorChangedListener = listener;
    }

    public int getColor() {
        return Color.HSVToColor(alpha, new float[]{hue, sat, val});
    }

    public void setColor(int color) {
        setColor(color, false);
    }

    public void setColor(int color, boolean callback) {

        int alpha = Color.alpha(color);
        int red = Color.red(color);
        int blue = Color.blue(color);
        int green = Color.green(color);

        float[] hsv = new float[3];

        Color.RGBToHSV(red, green, blue, hsv);

        this.alpha = alpha;
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];

        if (callback && onColorChangedListener != null) {
            onColorChangedListener.onColorChanged(Color.HSVToColor(this.alpha, new float[]{hue, sat, val}));
        }

        invalidate();
    }

    public void setAlphaSliderVisible(boolean visible) {
        if (showAlphaPanel != visible) {
            showAlphaPanel = visible;

            valShader = null;
            satShader = null;
            alphaShader = null;
            hueBackgroundCache = null;
            satValBackgroundCache = null;

            requestLayout();
        }
    }

    public int getSliderTrackerColor() {
        return sliderTrackerColor;
    }

    public void setSliderTrackerColor(int color) {
        sliderTrackerColor = color;
        hueAlphaTrackerPaint.setColor(sliderTrackerColor);
        invalidate();
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int color) {
        borderColor = color;
        invalidate();
    }

    public void setAlphaSliderText(int res) {
        String text = getContext().getString(res);
        setAlphaSliderText(text);
    }

    public String getAlphaSliderText() {
        return alphaSliderText;
    }

    public void setAlphaSliderText(String text) {
        alphaSliderText = text;
        invalidate();
    }

    public interface OnColorChangedListener {

        void onColorChanged(int newColor);
    }

    private class BitmapCache {

        public Canvas canvas;
        public Bitmap bitmap;
        public float value;
    }
}