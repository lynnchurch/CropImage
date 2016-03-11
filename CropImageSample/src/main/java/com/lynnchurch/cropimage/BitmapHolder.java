package com.lynnchurch.cropimage;

import android.graphics.Bitmap;

/**
 * 用于传递Bitmap对象
 */
public class BitmapHolder
{
    private Bitmap mBitmap;
    private static BitmapHolder mHolder;

    private BitmapHolder()
    {

    }

    public static BitmapHolder getInstance()
    {
        if (null == mHolder)
        {
            mHolder = new BitmapHolder();
        }
        return mHolder;
    }

    public void setBitmap(Bitmap bitmap)
    {
        mBitmap = bitmap;
    }

    public Bitmap getBitmap()
    {
        return mBitmap;
    }
}
