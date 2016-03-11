package com.lynnchurch.cropimage;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_CODE_PICK_GALLERY = 1;
    private static final int REQUEST_CODE_CROP = 2;
    ImageView iv_image;
    private boolean mCropCircleImage;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_image = (ImageView) findViewById(R.id.iv_image);
    }

    /**
     * 选择图片
     */
    public void pickImage()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        try
        {
            startActivityForResult(intent, REQUEST_CODE_PICK_GALLERY);
        } catch (ActivityNotFoundException e)
        {
            Toast.makeText(this, "No image source available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 裁剪头像
     *
     * @param v
     */
    public void cropAvatar(View v)
    {
        mCropCircleImage = true;
        pickImage();
    }

    /**
     * 裁剪图片
     *
     * @param v
     */
    public void cropPhoto(View v)
    {
        mCropCircleImage = false;
        pickImage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (RESULT_OK == resultCode)
        {
            switch (requestCode)
            {
                case REQUEST_CODE_PICK_GALLERY:
                    Intent intent = new Intent(MainActivity.this, CropActivity.class);
                    intent.putExtra(CropActivity.IMAGE_PATH, data.getData());
                    intent.putExtra(CropActivity.CROP_CIRCLE_IMAGE, mCropCircleImage);
                    startActivityForResult(intent, REQUEST_CODE_CROP);
                    break;
                case REQUEST_CODE_CROP:
                    iv_image.setImageBitmap(BitmapHolder.getInstance().getBitmap());
                    break;
                default:
            }
        }
    }
}
