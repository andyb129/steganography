package com.stealthcotper.steg;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.stealthcopter.steganography.Steg;

import net.alhazmy13.mediapicker.Image.ImagePicker;

import java.io.File;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

  private static final int PIXEL_DELAY = 1000; // 5 seconds
  private static final int[] mPixelDensityRangeArray = {12, 10, 36, 50, 70};
  private static final int MSG_ENCODE = 101;
  private static final int MSG_DECODE = 102;
  private static final int MSG_ENCODE_ERROR = 103;
  private static final int MSG_DECODE_ERROR = 104;

  private String mPath;
  private Handler mHandler;
  private Runnable mPixelEffectRunnable;
  private Pixelate mImageView;
  private Bitmap mEncodedBitmap;
  private TextView mEncodeTextView;
  private TextView mHelpTextView;
  private MainActivity mActivity;
  private int mPixelDensityIndex;
  private Snackbar mEncodedTextSnackbar;
  private Snackbar mDecodedTextSnackbar;
  private String mEncodeText;
  private String mDecodedMessage;

  private Handler mHandlerThreadFinished = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      fadeBackPixelEffect();
      switch (msg.what)
      {
        case MSG_ENCODE:
          break;
        case MSG_DECODE:
          mDecodedTextSnackbar = showSnackbar(String.format(getString(R.string.decoded_text_display),
                  mDecodedMessage), Snackbar.LENGTH_INDEFINITE);
          break;
        case MSG_ENCODE_ERROR:
          showSnackbar(getString(R.string.encoded_error), Snackbar.LENGTH_LONG);
          break;
        case MSG_DECODE_ERROR:
          showSnackbar(getString(R.string.decoded_error), Snackbar.LENGTH_LONG);
          break;
      }
    }
  };

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initView();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_add_image) {
      new ImagePicker.Builder(MainActivity.this)
              .mode(ImagePicker.Mode.GALLERY)
              .build();
    } else if (id == R.id.action_encode_message) {
      promptUserForText();
    } else if (id == R.id.action_decode_message) {
      decodeImage();
    }

    return super.onOptionsItemSelected(item);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == ImagePicker.IMAGE_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
      mPath = data.getStringExtra(ImagePicker.EXTRA_IMAGE_PATH);
      loadImage();
    }
  }

  private void initView() {
    mActivity = this;

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    mEncodeTextView = (TextView) findViewById(R.id.content_main_text_view_encode);
    mHelpTextView = (TextView) findViewById(R.id.content_main_text_view_help);
    mImageView = (Pixelate) findViewById(R.id.content_main_image_view);
    mImageView.requestFocus();
    setSupportActionBar(toolbar);
    getSupportActionBar().setElevation(0);
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        shareImage();
      }
    });
  }

  private void promptUserForText() {
    if (mImageView.getDrawable()!=null) {
      final AlertDialog.Builder alert = new AlertDialog.Builder(this);
      LayoutInflater inflater = getLayoutInflater();
      View dialogView = inflater.inflate(R.layout.text_dialog_element, null);
      final EditText editTextView = (EditText) dialogView.findViewById(R.id.text_dialog_element_edit_text);
      alert.setMessage(getString(R.string.encode_text_message));
      alert.setView(dialogView);
      alert.setPositiveButton(getString(R.string.encode_text_ok), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          // action here for OK
          String textValue = editTextView.getText().toString();
          if (!textValue.equalsIgnoreCase("")) {
            mEncodeText = textValue;
            setEncodeTextVisibility(true);
            startPixelEffect(true);
            go(true);
            mEncodedTextSnackbar = showSnackbar(String.format(getString(R.string.encoded_string_display), textValue),
                    Snackbar.LENGTH_INDEFINITE);
            dialog.dismiss();
          }
        }
      });
      alert.setNegativeButton(getString(R.string.encode_text_cancel), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          dialog.dismiss();
        }
      });
      alert.show();
    } else {
      showSnackbar(getString(R.string.encode_no_image_error), Snackbar.LENGTH_LONG);
    }
  }

  private void decodeImage() {
    if (mImageView.getDrawable()!=null) {
      go(false);
    } else {
      showSnackbar(getString(R.string.encode_no_image_error), Snackbar.LENGTH_LONG);
    }
  }

  private void shareImage() {
    if (mEncodedBitmap != null) {
      String path = MediaStore.Images.Media.insertImage(getContentResolver(), mEncodedBitmap,
              "Steganography image", null);
      //String path = getDirPath(this);
      Uri uri = Uri.parse(path);

      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("image/jpeg");
      intent.putExtra(Intent.EXTRA_STREAM, uri);
      startActivity(Intent.createChooser(intent, "Share Image"));
    } else {
      showSnackbar(getString(R.string.encode_no_image_error), Snackbar.LENGTH_LONG);
    }
  }

  public static String getDirPath(Context ctx) {
    File sdDir = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    return new File(sdDir, ctx.getResources().getString(R.string.app_name)).getPath();
  }

  private Snackbar showSnackbar(String text, int snackbarState) {
    Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator_layout),
            text, snackbarState);
    snackbar.show();
    return snackbar;
  }

  private void loadImage() {
    //mEncodedBitmap = null;
    setEncodeTextVisibility(false);
    mHelpTextView.setVisibility(View.GONE);
    if (mDecodedTextSnackbar!=null) mDecodedTextSnackbar.dismiss();
    if (mEncodedTextSnackbar!=null) mEncodedTextSnackbar.dismiss();
    mImageView.setImageBitmap(BitmapFactory.decodeFile(mPath));

    Drawable mDrawable = mImageView.getDrawable();
    mEncodedBitmap = ((BitmapDrawable) mDrawable).getBitmap();
  }

  private void setEncodeTextVisibility(boolean isVisible) {
    mEncodeTextView.setVisibility(isVisible ? View.VISIBLE : View.GONE);
  }

  private void startPixelEffect(final boolean isRandom) {
    mPixelDensityIndex = 0;
    mHandler = new Handler();
    mPixelEffectRunnable = new Runnable() {
      @Override
      public void run() {
        try {
          if (isRandom) {
            mImageView.pixelate(new Random().nextInt(44) + 6);
          } else {
            mImageView.pixelate(mPixelDensityRangeArray[mPixelDensityIndex]);
            mPixelDensityIndex += 1;
          }
        } finally {
          if (isRandom || mPixelDensityIndex < mPixelDensityRangeArray.length) {
            mHandler.postDelayed(mPixelEffectRunnable, PIXEL_DELAY);
          } else {
            mImageView.clear();
            mEncodeTextView.setText(R.string.encoded_ready_for_share);
          }
        }
      }
    };
    mPixelEffectRunnable.run();
  }

  private void fadeBackPixelEffect() {
    if (mHandler!=null) {
      mHandler.removeCallbacks(mPixelEffectRunnable);
      startPixelEffect(false);
    }
  }

  private void go(final boolean isEncode){
    new Thread(new Runnable() {
      @Override public void run() {
        try {
          //Test.runTests();
          if (isEncode) {
            attemptEncoding();
          } else {
            attemptDecoding();
          }
        } catch (Exception e) {
          callBackToUiThread(isEncode ? MSG_ENCODE_ERROR : MSG_DECODE_ERROR, null);
          e.printStackTrace();
        }
      }
    }).start();
  }

  private void attemptEncoding() throws Exception {

    //String hiddenMessage = "Hello this is a hidden message";
    String hiddenMessage = mEncodeText;

    //Bitmap mBitmap = BitmapHelper.createTestBitmap(200, 200);
    Drawable mDrawable = mImageView.getDrawable();
    Bitmap mBitmap = ((BitmapDrawable) mDrawable).getBitmap();
    mEncodedBitmap = Steg.withInput(mBitmap).encode(hiddenMessage).intoBitmap();

    //call back to ui thread
    callBackToUiThread(MSG_ENCODE, mEncodedBitmap);
  }

  private void attemptDecoding() throws Exception {

    Drawable mDrawable = mImageView.getDrawable();
    Bitmap mBitmap = ((BitmapDrawable) mDrawable).getBitmap();
    mDecodedMessage = Steg.withInput(mBitmap).decode().intoString();
    Log.d(getClass().getSimpleName(), "Decoded Message: " + mDecodedMessage);

    //call back to ui thread
    callBackToUiThread(MSG_DECODE, mDecodedMessage);
  }

  private void callBackToUiThread(int msgType, Object obj) {
    Message msg = Message.obtain(mHandlerThreadFinished, msgType, obj);
    msg.sendToTarget();
  }

}
