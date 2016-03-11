package io.togoto.imagezoomcrop;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.togoto.imagezoomcrop.cropoverlay.CropOverlayView;
import io.togoto.imagezoomcrop.cropoverlay.utils.GOTOConstants;
import io.togoto.imagezoomcrop.cropoverlay.utils.InternalStorageContentProvider;
import io.togoto.imagezoomcrop.cropoverlay.utils.L;
import io.togoto.imagezoomcrop.cropoverlay.utils.Utils;
import io.togoto.imagezoomcrop.photoview.IGetImageBounds;
import io.togoto.imagezoomcrop.photoview.PhotoView;

/**
 * @author GT
 */
public class ImageCropActivity extends Activity
{

    public static final String TAG = "ImageCropActivity";
    private static final int ANCHOR_CENTER_DELTA = 10;

    PhotoView mPhotoView;
    CropOverlayView mCropOverlayView;
    ImageView btnDone;
    ImageView mBtnCancel;
    ImageView rotateLeft;
    ImageView rotateRight;
    private ContentResolver mContentResolver;

    private final int IMAGE_MAX_SIZE = 1024;
    private final Bitmap.CompressFormat mOutputFormat = Bitmap.CompressFormat.PNG;

    //Temp file to save cropped image
    private String mImagePath;
    private Uri mSaveUri = null;
    private Uri mImageUri = null;


    //File for capturing camera images
    private File mFileTemp;
    public static final String TEMP_PHOTO_FILE_NAME = "temp_photo.png";
    public static final int REQUEST_CODE_PICK_GALLERY = 0x1;
    public static final int REQUEST_CODE_TAKE_PICTURE = 0x2;
    public static final int REQUEST_CODE_CROPPED_PICTURE = 0x3;
    public static final String ERROR_MSG = "error_msg";
    public static final String ERROR = "error";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        mContentResolver = getContentResolver();
        mPhotoView = (PhotoView) findViewById(R.id.iv_photo);
        mCropOverlayView = (CropOverlayView) findViewById(R.id.crop_overlay);
        btnDone = (ImageView) findViewById(R.id.done);
        mBtnCancel = (ImageView) findViewById(R.id.cancel);
        rotateLeft = (ImageView) findViewById(R.id.rotateLeft);
        rotateRight = (ImageView) findViewById(R.id.rotateRight);

        btnDone.setOnClickListener(btnDoneListerner);
        mBtnCancel.setOnClickListener(btnCancelListerner);
        rotateLeft.setOnClickListener(btnRotateLeft);
        rotateRight.setOnClickListener(btnRotateRight);

        mPhotoView.setImageBoundsListener(new IGetImageBounds()
        {
            @Override
            public Rect getImageBounds()
            {
                return mCropOverlayView.getImageBounds();
            }
        });


        createTempFile();
        if (savedInstanceState == null || !savedInstanceState.getBoolean("restoreState"))
        {
            String action = getIntent().getStringExtra("ACTION");
            if (null != action)
            {
                switch (action)
                {
                    case GOTOConstants.IntentExtras.ACTION_CAMERA:
                        getIntent().removeExtra("ACTION");
                        takePic();
                        return;
                    case GOTOConstants.IntentExtras.ACTION_GALLERY:
                        getIntent().removeExtra("ACTION");
                        pickImage();
                        return;
                }
            }
        }
        mImagePath = mFileTemp.getPath();
        mSaveUri = Utils.getImageUri(mImagePath);
        mImageUri = Utils.getImageUri(mImagePath);
        init();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    private void init()
    {
        Bitmap bitmap = getBitmap(mImageUri);
        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
        mPhotoView.setImageDrawable(drawable);
        mPhotoView.setMinimumScaleToFit();
        //Initialize the MoveResize text
    }

    private View.OnClickListener btnDoneListerner = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            boolean saved = saveOutput();
            if (saved)
            {
                //USUALLY Upload image to server here
                Intent intent = new Intent();
                intent.putExtra(GOTOConstants.IntentExtras.IMAGE_PATH, mImagePath);
                setResult(RESULT_OK, intent);
                finish();
            } else
            {
                Toast.makeText(ImageCropActivity.this, "Unable to save Image into your device.", Toast.LENGTH_LONG).show();
            }
        }
    };

    private View.OnClickListener btnCancelListerner = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            setResult(RESULT_CANCELED);
            finish();
        }
    };

    private View.OnClickListener btnRotateLeft = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mPhotoView.setRotationBy(-90);
        }
    };

    private View.OnClickListener btnRotateRight = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mPhotoView.setRotationBy(90);
        }
    };

    private void createTempFile()
    {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
        {
            mFileTemp = new File(this.getExternalFilesDir(null), TEMP_PHOTO_FILE_NAME);
        } else
        {
            mFileTemp = new File(getFilesDir(), TEMP_PHOTO_FILE_NAME);
        }
    }


    private void takePic()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try
        {
            Uri mImageCaptureUri = null;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state))
            {
                mImageCaptureUri = Uri.fromFile(mFileTemp);
            } else
            {
                /*
                 * The solution is taken from here: http://stackoverflow.com/questions/10042695/how-to-get-camera-result-as-a-uri-in-data-folder
	        	 */
                mImageCaptureUri = InternalStorageContentProvider.CONTENT_URI;
            }
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
            takePictureIntent.putExtra("return-data", true);
            startActivityForResult(takePictureIntent, REQUEST_CODE_TAKE_PICTURE);
        } catch (ActivityNotFoundException e)
        {
            Log.e(TAG, "Can't take picture", e);
            Toast.makeText(this, "Can't take picture", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("restoreState", true);
    }

    private void pickImage()
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

    private static void copyStream(InputStream input, OutputStream output) throws IOException
    {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1)
        {
            output.write(buffer, 0, bytesRead);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result)
    {
        super.onActivityResult(requestCode, resultCode, result);
        createTempFile();
        if (requestCode == REQUEST_CODE_TAKE_PICTURE)
        {
            if (resultCode == RESULT_OK)
            {
                mImagePath = mFileTemp.getPath();
                mSaveUri = Utils.getImageUri(mImagePath);
                mImageUri = Utils.getImageUri(mImagePath);
                init();
            } else if (resultCode == RESULT_CANCELED)
            {
                userCancelled();
                return;
            } else
            {
                errored("Error while opening the image file. Please try again.");
                return;
            }

        } else if (requestCode == REQUEST_CODE_PICK_GALLERY)
        {
            if (resultCode == RESULT_CANCELED)
            {
                userCancelled();
                return;
            } else if (resultCode == RESULT_OK)
            {
                try
                {
                    InputStream inputStream = getContentResolver().openInputStream(result.getData()); // Got the bitmap .. Copy it to the temp file for cropping
                    FileOutputStream fileOutputStream = new FileOutputStream(mFileTemp);
                    copyStream(inputStream, fileOutputStream);
                    fileOutputStream.close();
                    inputStream.close();
                    mImagePath = mFileTemp.getPath();
                    mSaveUri = Utils.getImageUri(mImagePath);
                    mImageUri = Utils.getImageUri(mImagePath);
                    init();
                } catch (Exception e)
                {
                    errored("Error while opening the image file. Please try again.");
                    L.e(e);
                    return;
                }
            } else
            {
                errored("Error while opening the image file. Please try again.");
                return;
            }

        }
    }


    private Bitmap getBitmap(Uri uri)
    {
        InputStream in = null;
        Bitmap returnedBitmap = null;
        try
        {
            in = mContentResolver.openInputStream(uri);
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int scale = 1;
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE)
            {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            in = mContentResolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, o2);
            in.close();

            //First check
            ExifInterface ei = new ExifInterface(uri.getPath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    returnedBitmap = rotateImage(bitmap, 90);
                    //Free up the memory
                    bitmap.recycle();
                    bitmap = null;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    returnedBitmap = rotateImage(bitmap, 180);
                    //Free up the memory
                    bitmap.recycle();
                    bitmap = null;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    returnedBitmap = rotateImage(bitmap, 270);
                    //Free up the memory
                    bitmap.recycle();
                    bitmap = null;
                    break;
                default:
                    returnedBitmap = bitmap;
            }
            return returnedBitmap;
        } catch (FileNotFoundException e)
        {
            L.e(e);
        } catch (IOException e)
        {
            L.e(e);
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                super.onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private boolean saveOutput()
    {
        Bitmap croppedImage = mPhotoView.getCroppedImage();
        if (mSaveUri != null)
        {
            OutputStream outputStream = null;
            try
            {
                outputStream = mContentResolver.openOutputStream(mSaveUri);
                if (outputStream != null)
                {
                    croppedImage.compress(mOutputFormat, 100, outputStream);
                }
            } catch (IOException ex)
            {
                ex.printStackTrace();
                return false;
            } finally
            {
                closeSilently(outputStream);
            }
        } else
        {
            Log.e(TAG, "not defined image url");
            return false;
        }
        croppedImage.recycle();
        return true;
    }


    public void closeSilently(Closeable c)
    {
        if (c == null) return;
        try
        {
            c.close();
        } catch (Throwable t)
        {
            // do nothing
        }
    }


    private Bitmap rotateImage(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    public void userCancelled()
    {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    public void errored(String msg)
    {
        Intent intent = new Intent();
        intent.putExtra(ERROR, true);
        if (msg != null)
        {
            intent.putExtra(ERROR_MSG, msg);
        }
        finish();
    }


}
