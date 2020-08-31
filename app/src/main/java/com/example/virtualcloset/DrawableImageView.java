package com.example.virtualcloset;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

@SuppressLint("AppCompatCustomView")
public class DrawableImageView extends ImageView implements View.OnTouchListener {
    private float downx = 0;
    private float downy = 0;
    private float upx = 0;
    private float upy = 0;

    public float paintSize = 5;
    public Canvas canvas;
    public Bitmap original_bitmap;
    public Paint paint;
    public Mat mask;
    private Matrix matrix;

    public DrawableImageView(Context context)
    {
        super(context);
        setOnTouchListener(this);
    }

    public DrawableImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setOnTouchListener(this);
    }

    public DrawableImageView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        setOnTouchListener(this);
    }

    public void setNewImage(Bitmap alteredBitmap, Bitmap bmp)
    {
        mask = new Mat (bmp.getHeight(), bmp.getWidth(), CvType.CV_8U, new Scalar(Imgproc.GC_PR_BGD));
        original_bitmap = bmp;
        canvas = new Canvas(alteredBitmap);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(paintSize);
        matrix = new Matrix();
        canvas.drawBitmap(bmp, matrix, paint);
        setImageBitmap(alteredBitmap);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        byte[] buffer = new byte[3];
        switch (action)
        {
            case MotionEvent.ACTION_DOWN:
                paint.setStrokeWidth(paintSize);

                downx = getPointerCoords(event)[0];//event.getX();
                downy = getPointerCoords(event)[1];//event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                paint.setStrokeWidth(paintSize);

                upx = getPointerCoords(event)[0];//event.getX();
                upy = getPointerCoords(event)[1];//event.getY();
                canvas.drawLine(downx, downy, upx, upy, paint);
                Imgproc.line(mask, new Point(downx, downy), new Point(upx, upy),
                        new Scalar(
                                paint.getColor() == Color.parseColor("#1de9b6") ?
                                        (byte)Imgproc.GC_FGD : (byte)Imgproc.GC_BGD
                        ), (int)paintSize);
                invalidate();
                downx = upx;
                downy = upy;
                break;
            case MotionEvent.ACTION_UP:
                paint.setStrokeWidth(paintSize);
                upx = getPointerCoords(event)[0];//event.getX();
                upy = getPointerCoords(event)[1];//event.getY();
                canvas.drawLine(downx, downy, upx, upy, paint);
                Imgproc.line(mask, new Point(downx, downy), new Point(upx, upy),
                        new Scalar(
                                paint.getColor() == Color.parseColor("#1de9b6") ?
                                (byte)Imgproc.GC_FGD : (byte)Imgproc.GC_BGD
                        ), (int)paintSize);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                paint.setStrokeWidth(paintSize);
                break;
            default:
                paint.setStrokeWidth(paintSize);
                break;
        }

        // Draw onto mask
//        if (action == MotionEvent.ACTION_DOWN ||
//                action == MotionEvent.ACTION_UP ||
//                action == MotionEvent.ACTION_MOVE) {
//            buffer[0] = paint.getColor() == Color.parseColor("#1de9b6") ?
//                    (byte)Imgproc.GC_FGD : (byte)Imgproc.GC_BGD;
//            mask.put((int)downx, (int)downy, buffer);
//        }

        return true;
    }

    final float[] getPointerCoords(MotionEvent e)
    {
        final int index = e.getActionIndex();
        final float[] coords = new float[] { e.getX(index), e.getY(index) };
        Matrix matrix = new Matrix();
        getImageMatrix().invert(matrix);
        matrix.postTranslate(getScrollX(), getScrollY());
        matrix.mapPoints(coords);
        return coords;
    }
}
