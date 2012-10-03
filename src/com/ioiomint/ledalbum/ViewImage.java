package com.ioiomint.ledalbum;

//import ioio.lib.api.RgbLedMatrix;  //this was conflicting with android.graphics.Matrix so work around is to use the long name below in code
//import ioio.lib.api.RgbLedMatrix.Matrix;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Random;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.SensorManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;


public class ViewImage extends IOIOActivity {
		private ioio.lib.api.RgbLedMatrix.Matrix KIND; 
		private android.graphics.Matrix matrix2;
        private String filename;
        private static final String TAG = "Album";	  	
	  	private int z = 1;
	  	private short[] frame_ = new short[512];
	  	private short[] rgb_;
	  	public static final Bitmap.Config FAST_BITMAP_CONFIG = Bitmap.Config.RGB_565;
	  	private Bitmap frame1;
	  	private byte[] BitmapBytes;
	  	private byte[] BitmayArray;
	  	private byte[] dotArray;
	  	private InputStream BitmapInputStream;
	  	private ByteBuffer bBuffer;
	  	private ShortBuffer sBuffer;
	  	private SensorManager mSensorManager;
	  	private Random randomGenerator = new Random();
	  	private Bitmap canvasBitmap;
	  	private Bitmap originalImage;
	  	private int width_original;
	  	private int height_original; 	  
	  	private float scaleWidth; 
	  	private float scaleHeight; 	  	
	  	private Bitmap resizedBitmap;  	
	  	private int i = 0;
	  	private int deviceFound = 0;
	  	private Handler mHandler;
      
      
      @Override
      public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mHandler = new Handler(); //used by toast
            System.gc();
            Intent i = getIntent();
            Bundle extras = i.getExtras();
            BitmapFactory.Options bfo = new BitmapFactory.Options();
           // bfo.inSampleSize = 2; //had to remove this as it was scaling it by 2 reduction
            filename = extras.getString("filename");
            ImageView iv = new ImageView(getApplicationContext());
            originalImage = BitmapFactory.decodeFile(filename, bfo);
            iv.setImageBitmap(originalImage);
            setContentView(iv);
      
	        KIND = ioio.lib.api.RgbLedMatrix.Matrix.ADAFRUIT_32x16;
	    	// BitmapInputStream = getResources().openRawResource(R.raw.eightball2);	
		     frame_ = new short [KIND.width * KIND.height];
			 BitmapBytes = new byte[KIND.width * KIND.height *2]; //512 * 2 = 1024 or 1024 * 2 = 2048
	   		// originalImage = BitmapFactory.decodeResource(getResources(),R.drawable.puma);
	   		 
	   		 width_original = originalImage.getWidth();
	   		 height_original = originalImage.getHeight();
	   		 
	   		// showToast("height:" + height_original);
	   		// showToast("width:" + width_original);
	   		 
	   		 if (width_original != KIND.width || height_original != KIND.height) {
	   			 //the iamge is not the right dimensions, so we need to re-size
	   			// showToast("we did HAVE to re-size");
	   			 scaleWidth = ((float) KIND.width) / width_original;
		   		 scaleHeight = ((float) KIND.height) / height_original;
		   		 // create matrix for the manipulation
		   		 matrix2 = new Matrix();
		   		 // resize the bit map
		   		 matrix2.postScale(scaleWidth, scaleHeight);
		   		 resizedBitmap = Bitmap.createBitmap(originalImage, 0, 0, width_original, height_original, matrix2, true);
		   		 canvasBitmap = Bitmap.createBitmap(KIND.width, KIND.height, Config.RGB_565); 
		   		 Canvas canvas = new Canvas(canvasBitmap);
		   	   	 canvas.drawBitmap(resizedBitmap, 0, 0, null);
		   		 ByteBuffer buffer = ByteBuffer.allocate(KIND.width * KIND.height *2); //Create a new buffer
		   		 canvasBitmap.copyPixelsToBuffer(buffer); //copy the bitmap 565 to the buffer		
		   		 BitmapBytes = buffer.array(); //copy the buffer into the type array
	   		 }
	   		 else {
	   			 // //the image is already the right dimensions, no needs to resize
	   			 canvasBitmap = Bitmap.createBitmap(KIND.width, KIND.height, Config.RGB_565); 
	   			 //showToast("we did NOT have to re-size");
	   	   		 Canvas canvas = new Canvas(canvasBitmap);
		   	   	 canvas.drawBitmap(originalImage, 0, 0, null);
		   		 ByteBuffer buffer = ByteBuffer.allocate(KIND.width * KIND.height *2); //Create a new buffer
		   		 canvasBitmap.copyPixelsToBuffer(buffer); //copy the bitmap 565 to the buffer		
		   		 BitmapBytes = buffer.array(); //copy the buffer into the type array
	   		 }	   		
	   		 
	   		 //now let's scale this to the right size
	   		 //	BitmapInputStream = getResources().openRawResource(R.raw.clover);
	            
	   		loadImage();   
	            
	      }
      
  	
      
      public void loadImage() {
  	//	try {
  	//		int n = BitmapInputStream.read(BitmapBytes, 0, BitmapBytes.length); // reads
  																				// the
  																				// input
  																				// stream
  																				// into
  																				// a
  																				// byte
  																				// array
  		//	Arrays.fill(BitmapBytes, n, BitmapBytes.length, (byte) 0);
  	//	} catch (IOException e) {
  	//		e.printStackTrace();
  	//	}

  		int y = 0;
  		for (int i = 0; i < frame_.length; i++) {
  			frame_[i] = (short) (((short) BitmapBytes[y] & 0xFF) | (((short) BitmapBytes[y + 1] & 0xFF) << 8));
  			y = y + 2;
  		}

  	}
      
      
      class IOIOThread extends BaseIOIOLooper {
  		private ioio.lib.api.RgbLedMatrix matrix_;

  		@Override
  		protected void setup() throws ConnectionLostException {
  			matrix_ = ioio_.openRgbLedMatrix(KIND);
  			deviceFound = 1; //if we went here, then we are connected over bluetooth or USB
  			//connectTimer.cancel(); //we can stop this since it was found
  		}

  		@Override
  		public void loop() throws ConnectionLostException {
  		
  			matrix_.frame(frame_); //writes whatever is in bitmap raw 565 file buffer to the RGB LCD
  					
  			}	
  		}

  	@Override
  	protected IOIOLooper createIOIOLooper() {
  		return new IOIOThread();
  	}
      
  	public void showToast(final String msg) {
  	  	mHandler.post(new Runnable() {
  	        @Override
  	        public void run() {
  	            Toast toast = Toast.makeText(ViewImage.this, msg, Toast.LENGTH_LONG);
  	            toast.show();
  	        }
  	    });
  	}
      
}