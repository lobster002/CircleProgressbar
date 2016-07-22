package com.bangnizhao.www.circleprogressbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by Sky on 2016/7/19.
 */
public class CircleProgressbar extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Paint textPaint = null;
    private Paint circlePaint = null;
    private Paint bgPaint = null;
    private Paint dotTextPaint;

    private int halfWidthSize;
    private int halfHeightSize;
    private int circleRadius = (int) dip2px(143);//内圆半径
    float insideCircleRadius = circleRadius - dip2px(5);

    private volatile String centerText = "";
    private SurfaceHolder holder;

    private float angle;

    private int textColor = Color.parseColor("#666666"); //说实话 这个颜色很难看
    private int bgColor = Color.parseColor("#f2f2f2");
    private int white = Color.parseColor("#FFFFFF");

    /*
    * 一分钟  就是 60 * 1000 毫秒转一圈  根据系统时间差值 计算旋转角度
    * */
    private volatile long theStartTime;
    private volatile long startTime;
    private volatile long endTime;

    private Object lock = new Object();//所有涉及到时间的地方 必须加此锁  不如频繁设置进度 会出现死锁

    private int DEFAULT_TEXT_SIZE = (int) sp2px(40);//默认中间的文字大小
    private int DOT_SIZE = (int) dip2px(8);//默认圆点大小
    private int MAX_VALUE = 60 * 60; //进度设置最大值  设置之后根据设置值 与最大值的比较  计算偏转角度


    private Thread mDrawThread;//工作线程  切记不要在此线程内更新其他UI界面

    private DrawCircleListener listener;//转慢一圈时的回调

    private float DOT_TEXTSIZE = sp2px(10);// 默认小红点中文字大小


    public synchronized void setCenterText(String text) {
        this.centerText = text;
    }

    public CircleProgressbar(Context context) {
        super(context);
        init();
    }

    public CircleProgressbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleProgressbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        holder = getHolder();
        holder.addCallback(this);
    }

    private void draw() {
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(bgColor);//重置画布
        drawCircle(canvas); //画圆环
        drawBackground(canvas);//画白色内圆
        drawDot(canvas);//在满足条件情况下 画小圆点
        drawText(canvas);//写正中间文本
        holder.unlockCanvasAndPost(canvas);//提交 更新界面
    }


    /*
    * 初始化
    * */
    private void initData() {

        if (0 == halfWidthSize) {
            int widthSize = getMeasuredWidth();
            halfWidthSize = widthSize >> 1;
        }
        if (0 == halfHeightSize) {
            int heightSize = getMeasuredHeight();
            halfHeightSize = heightSize >> 1;
        }

        if (halfWidthSize > halfHeightSize) {
            int tmp = halfWidthSize;
            halfWidthSize = halfHeightSize;
            halfHeightSize = tmp;
        }

        if (null == textPaint) {
            textPaint = new Paint();
            textPaint.setColor(textColor);
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(DEFAULT_TEXT_SIZE);
        }

        if (null == dotTextPaint) {
            dotTextPaint = new Paint();
            dotTextPaint.setColor(Color.WHITE);
            dotTextPaint.setTextSize(DOT_TEXTSIZE);
            dotTextPaint.setAntiAlias(true);
        }

        if (null == circlePaint) {
            circlePaint = new Paint();
            circlePaint.setStrokeWidth(dip2px(10));
            circlePaint.setAntiAlias(true);
            circlePaint.setColor(Color.GREEN);
        }

        if (null == bgPaint) {
            bgPaint = new Paint();
            bgPaint.setColor(bgColor);
            bgPaint.setAntiAlias(true);
        }
    }


    private void drawText(Canvas canvas) {
        if (TextUtils.isEmpty(centerText)) {
            return;
        }
        Rect textBound = new Rect();
        textPaint.getTextBounds(centerText, 0, centerText.length(), textBound);
        float textwidth = textPaint.measureText(centerText);
        float textheight = textBound.height();
        canvas.drawText(centerText, halfWidthSize - (textwidth / 2.0f), halfHeightSize + (textheight / 2.0f), textPaint);
    }

    private void drawCircle(Canvas canvas) {
        long deltaTime = (startTime - theStartTime);
        RectF oval = new RectF(halfWidthSize - circleRadius, halfHeightSize - circleRadius, halfWidthSize + circleRadius, halfHeightSize + circleRadius);
        angle = (float) (deltaTime * 0.006);//偏转角度
        circlePaint.setStyle(Paint.Style.STROKE);
        float fraction = angle / 360.f;
        canvas.drawCircle(halfWidthSize, halfHeightSize, circleRadius, bgPaint);
        circlePaint.setColor(evaluate(fraction, Color.GREEN, Color.RED));
        canvas.drawArc(oval, -90, angle, false, circlePaint);
    }

    private void drawDot(Canvas canvas) {
        if (angle < 180.0f) {//30秒之前  即偏转180度之前 直接返回
            return;
        }
        int radius = (int) ((int) insideCircleRadius + dip2px(5));
        float deltaY = (float) (radius * Math.sin(Math.toRadians(angle + 90)));
        float deltaX = (float) (radius * Math.cos(Math.toRadians(angle + 90)));
        float x = halfWidthSize - deltaX;
        float y = halfHeightSize - deltaY;

        circlePaint.setStyle(Paint.Style.FILL);

        int num = (int) ((360 - angle) / 6 + 0.5f);
        String text = String.valueOf(num);
        if (angle >= 330) {//此时要求圆点有变小的渐变
            float scale = DOT_SIZE - ((angle - 330.0f) / 5);
            canvas.drawCircle(x, y, dip2px(scale), circlePaint);
        } else {
            canvas.drawCircle(x, y, DOT_SIZE, circlePaint);
        }

        Rect textBound = new Rect();
        dotTextPaint.getTextBounds(text, 0, text.length(), textBound);
        float textwidth = dotTextPaint.measureText(text);
        float textheight = textBound.height();
        if (0 != num) {//0的时候显示没意义
            canvas.drawText(text, x - (textwidth / 2), y + (textheight / 2), dotTextPaint);
        }
    }

    private void drawBackground(Canvas canvas) {
        bgPaint.setColor(white);
        canvas.drawCircle(halfWidthSize, halfHeightSize, insideCircleRadius, bgPaint);
        bgPaint.setColor(bgColor);
    }

    public void setCurrentValue(int value) {
        if (value < 0) {
            value = 0;
        }

        if (value >= MAX_VALUE) {
            value %= MAX_VALUE;
        }

        synchronized (lock) {
            startTime = System.currentTimeMillis();
            theStartTime = startTime;
            long defaultVaule = value * 60 * 1000 / MAX_VALUE;
            theStartTime -= defaultVaule;
        }
    }

    private float dip2px(float dp) {
        float scale = getResources().getDisplayMetrics().density;
        return scale * dp + 0.5f;
    }

    public float sp2px(float spValue) {
        final float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return spValue * fontScale + 0.5f;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        if (null == mDrawThread) {
            mDrawThread = new Thread(this);
            mDrawThread.start();
        }
        initData();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopThread();
        mDrawThread = null;
    }


    private volatile boolean isRunning = true;

    private void stopThread() {
        isRunning = false;
    }

    @Override
    public void run() {
        try {
            while (isRunning) {
                synchronized (lock) {
                    startTime = System.currentTimeMillis();
                }
                if ((startTime - theStartTime) >= 60 * 1000) {
                    theStartTime = startTime;
                    if (null != listener) {
                        listener.onCircleComplete(angle);
                    }
                }
                draw();
//                synchronized (lock) {
//                    endTime = System.currentTimeMillis();
//                }
//                Log.e("TAG", "FPS:" + (1000 / (endTime - startTime)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*
    * 圆满一圈是的回调接口
    * 不要在里边更新UI  切记
    * */
    public void setDrawCircleListener(DrawCircleListener listener) {
        this.listener = listener;
    }

    /**
     * 颜色渐变的方法  有问题你们自己去改
     */
    public int evaluate(float fraction, int startValue, int endValue) {
        /*
        * 这里最好不要用 interger  频繁的装箱拆箱 相当耗费性能
        * */
        int startInt = startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24) |
                ((startR + (int) (fraction * (endR - startR))) << 16) |
                ((startG + (int) (fraction * (endG - startG))) << 8) |
                ((startB + (int) (fraction * (endB - startB))));
    }

}
