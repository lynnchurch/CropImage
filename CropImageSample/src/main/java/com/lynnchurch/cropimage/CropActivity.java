package com.lynnchurch.cropimage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.FileNotFoundException;
import java.io.InputStream;

import io.togoto.imagezoomcrop.cropoverlay.CropOverlayView;
import io.togoto.imagezoomcrop.photoview.IGetImageBounds;
import io.togoto.imagezoomcrop.photoview.PhotoView;

public class CropActivity extends AppCompatActivity
{
    public static final String IMAGE_PATH = "image_path";
    public static final String CROP_CIRCLE_IMAGE = "crop_circle_image";

    PhotoView mPhotoView;
    CropOverlayView mCropOverlayView;
    RadioGroup rg_ratio;
    Button btn_rotate;
    private Bitmap mBitmap;
    private boolean mCropCircleImage;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        mCropCircleImage = getIntent().getBooleanExtra(CROP_CIRCLE_IMAGE, false);
        Uri path = getIntent().getParcelableExtra(IMAGE_PATH);
        try
        {
            InputStream inputStream = getContentResolver().openInputStream(path);
            mBitmap = BitmapFactory.decodeStream(inputStream);

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        initView();
    }

    private void initView()
    {
        mPhotoView = (PhotoView) findViewById(io.togoto.imagezoomcrop.R.id.iv_photo);
        mCropOverlayView = (CropOverlayView) findViewById(io.togoto.imagezoomcrop.R.id.crop_overlay);
        rg_ratio = (RadioGroup) findViewById(R.id.rg_ratio);
        btn_rotate=(Button)findViewById(R.id.btn_rotate);
        rg_ratio.setVisibility(mCropCircleImage ? View.GONE : View.VISIBLE);
        mCropOverlayView.setCropCircle(mCropCircleImage);
        BitmapDrawable drawable = new BitmapDrawable(getResources(), mBitmap);
        mPhotoView.setImageDrawable(drawable);
        mPhotoView.setMinimumScaleToFit();
        mPhotoView.setImageBoundsListener(new IGetImageBounds()
        {
            @Override
            public Rect getImageBounds()
            {
                return mCropOverlayView.getImageBounds();
            }
        });
        ((RadioButton) findViewById(R.id.rb_1_1)).setChecked(true);
        rg_ratio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                switch (checkedId)
                {
                    case R.id.rb_4_3:
                        mCropOverlayView.setRatio(4f / 3f);
                        break;
                    case R.id.rb_5_4:
                        mCropOverlayView.setRatio(5f / 4f);
                        break;
                    case R.id.rb_1_1:
                        mCropOverlayView.setRatio(1);
                        break;
                    case R.id.rb_4_5:
                        mCropOverlayView.setRatio(4f / 5f);
                        break;
                    case R.id.rb_3_4:
                        mCropOverlayView.setRatio(3f / 4f);
                        break;
                    default:
                }
                mPhotoView.reset();
                mPhotoView.setMinimumScaleToFit();
            }
        });
        btn_rotate.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mPhotoView.setRotationBy(-90);
            }
        });
    }

    /**
     * 取消
     *
     * @param v
     */
    public void cancel(View v)
    {
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * 确定
     *
     * @param v
     */
    public void confirm(View v)
    {
        Bitmap croppedBitmap = mPhotoView.getCroppedImage();
        BitmapHolder.getInstance().setBitmap(croppedBitmap);
        setResult(RESULT_OK);
        finish();
    }
}
