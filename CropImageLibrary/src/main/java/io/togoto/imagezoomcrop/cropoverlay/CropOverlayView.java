package io.togoto.imagezoomcrop.cropoverlay;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import io.togoto.imagezoomcrop.R;
import io.togoto.imagezoomcrop.cropoverlay.edge.Edge;
import io.togoto.imagezoomcrop.cropoverlay.utils.PaintUtil;
import io.togoto.imagezoomcrop.photoview.IGetImageBounds;


/**
 * @author GT Modified/stripped down Code from cropper library : https://github.com/edmodo/cropper
 */
public class CropOverlayView extends View implements IGetImageBounds
{
    private static final String TAG = CropOverlayView.class.getSimpleName();
    //Defaults
    private boolean DEFAULT_GUIDELINES = true;
    private int DEFAULT_MARGINSIDE = 50;

    // we are cropping square image so width and height will always be equal
    private static final int DEFAULT_CORNER_RADIUS = 0;
    private static final int DEFAULT_OVERLAY_COLOR = Color.argb(168, 0, 0, 0);

    // The Paint used to draw the white rectangle around the crop area.
    private Paint mBorderPaint;

    // The Paint used to draw the guidelines within the crop area.
    private Paint mGuidelinePaint;

    private Path mClipPath;

    // The bounding box around the Bitmap that we are cropping.
    private RectF mBitmapRect;

    private int cropHeight;
    private int cropWidth;
    private int initCropHeight;
    private int initCropWidth;


    private boolean mGuidelines;
    private int mMarginTop;
    private int mMarginSide;
    private int mCornerRadius;
    private int mOverlayColor;
    private boolean mCropCircle = true;
    private Context mContext;
    private int mHeight;
    private int mWidth;
    private float mRatio = 1;

    public CropOverlayView(Context context)
    {
        this(context, null);
    }

    public CropOverlayView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CropOverlayView, 0, 0);
        try
        {
            mGuidelines = ta.getBoolean(R.styleable.CropOverlayView_guideLines, DEFAULT_GUIDELINES);
            mMarginSide = ta.getDimensionPixelSize(R.styleable.CropOverlayView_marginSide, DEFAULT_MARGINSIDE);
            final float defaultRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, DEFAULT_CORNER_RADIUS, mContext.getResources().getDisplayMetrics());
            mCornerRadius = ta.getDimensionPixelSize(R.styleable.CropOverlayView_cornerRadius, (int) defaultRadius);
            mOverlayColor = ta.getColor(R.styleable.CropOverlayView_overlayColor, DEFAULT_OVERLAY_COLOR);
        } finally
        {
            ta.recycle();
        }
        mHeight = context.getResources().getDisplayMetrics().heightPixels;
        mWidth = context.getResources().getDisplayMetrics().widthPixels;
        initCropWidth = mWidth - 2 * mMarginSide;
        initCropHeight = initCropWidth;
        cropWidth = initCropWidth;
        cropHeight = initCropHeight;
        init(mContext);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = h;
        mWidth = w;
        initCropWindow();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        //BUG FIX : Turn of hardware acceleration. Clip path doesn't work with hardware acceleration
        //BUG FIX : Will have to do it here @ View level. Activity level not working on HTC ONE X
        //http://stackoverflow.com/questions/8895677/work-around-canvas-clippath-that-is-not-supported-in-android-any-more/8895894#8895894
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        canvas.save();
        mBitmapRect.left = Edge.LEFT.getCoordinate();
        mBitmapRect.top = Edge.TOP.getCoordinate();
        mBitmapRect.right = Edge.RIGHT.getCoordinate();
        mBitmapRect.bottom = Edge.BOTTOM.getCoordinate();

        // 对裁剪区域进行高亮显示
        if (mCropCircle)
        {
            mClipPath.addCircle(mWidth / 2, mHeight / 2, mWidth / 2 - mBitmapRect.left, Path.Direction.CW);
        } else
        {
            mClipPath.addRoundRect(mBitmapRect, mCornerRadius, mCornerRadius, Path.Direction.CW);
        }
        canvas.clipPath(mClipPath, Region.Op.DIFFERENCE);
        canvas.drawColor(mOverlayColor);
        mClipPath.reset();
        canvas.restore();
        // 绘制裁剪的描边
        if (mCropCircle)
        {
            canvas.drawCircle(mWidth / 2, mHeight / 2, mWidth / 2 - mBitmapRect.left, mBorderPaint);
        } else
        {
            canvas.drawRoundRect(mBitmapRect, mCornerRadius, mCornerRadius, mBorderPaint);
        }

        //GT :  Drop shadow not working right now. Commenting the code now
//        //Draw shadow
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setShadowLayer(12, 0, 0, Color.YELLOW);
//        paint.setAlpha(0);
        if (mGuidelines && !mCropCircle)
        {
            drawRuleOfThirdsGuidelines(canvas);
        }
    }

    /**
     * 设置裁剪框宽与高的比
     *
     * @param ratio
     */
    public void setRatio(float ratio)
    {
        mRatio = ratio;
        init(mContext);
        initCropWindow();
    }

    @Override
    public Rect getImageBounds()
    {
        return new Rect(
                (int) Edge.LEFT.getCoordinate(), (int) Edge.TOP.getCoordinate(),
                (int) Edge.RIGHT.getCoordinate(), (int) Edge.BOTTOM.getCoordinate());
    }

    // Private Methods /////////////////////////////////////////////////////////
    private void init(Context context)
    {
        initCropWindow();
        mBorderPaint = PaintUtil.newBorderPaint(context);
        mGuidelinePaint = PaintUtil.newGuidelinePaint();
        mClipPath = new Path();
    }

    /**
     * 初始化裁剪窗
     */
    private void initCropWindow()
    {
        if (mRatio > 1)
        {
            cropWidth = initCropWidth;
            cropHeight = (int) (cropWidth / mRatio);
        } else
        {
            cropHeight = initCropHeight;
            cropWidth = (int) (cropHeight * mRatio);
        }
        mMarginTop = (mHeight - cropHeight) / 2;
        int edgeT = mMarginTop;
        int edgeB = mMarginTop + cropHeight;
        int edgeL = (mWidth - cropWidth) / 2;
        int edgeR = edgeL + cropWidth;
        Edge.TOP.setCoordinate(edgeT);
        Edge.BOTTOM.setCoordinate(edgeB);
        Edge.LEFT.setCoordinate(edgeL);
        Edge.RIGHT.setCoordinate(edgeR);
        mBitmapRect = new RectF(edgeL, edgeT, edgeR, edgeB);
        Log.i(TAG, "mBitmapRect:" + mBitmapRect);
    }

    private void drawRuleOfThirdsGuidelines(Canvas canvas)
    {

        final float left = Edge.LEFT.getCoordinate();
        final float top = Edge.TOP.getCoordinate();
        final float right = Edge.RIGHT.getCoordinate();
        final float bottom = Edge.BOTTOM.getCoordinate();

        // Draw vertical guidelines.
        final float oneThirdCropWidth = Edge.getWidth() / 3;

        final float x1 = left + oneThirdCropWidth;
        canvas.drawLine(x1, top, x1, bottom, mGuidelinePaint);
        final float x2 = right - oneThirdCropWidth;
        canvas.drawLine(x2, top, x2, bottom, mGuidelinePaint);

        // Draw horizontal guidelines.
        final float oneThirdCropHeight = Edge.getHeight() / 3;

        final float y1 = top + oneThirdCropHeight;
        canvas.drawLine(left, y1, right, y1, mGuidelinePaint);
        final float y2 = bottom - oneThirdCropHeight;
        canvas.drawLine(left, y2, right, y2, mGuidelinePaint);
    }

    /**
     * 设置是否裁剪圆形图
     *
     * @param cropCircle
     */
    public void setCropCircle(boolean cropCircle)
    {
        mCropCircle = cropCircle;
    }
}