package com.ioiomint.ledalbum;



import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient; 

/**
 * Displays images from an SD card.
 */
@SuppressLint({ "ParserError", "NewApi" })
public class ImageThumbnailsActivity extends IOIOActivity implements OnItemClickListener  {

    //look into internet loads
	//make an ledalbum directory, copy some files in there
    private int columnIndex;   
	private ioio.lib.api.RgbLedMatrix.Matrix KIND;  //have to do it this way because there is a matrix library conflict
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
  	
  	private SharedPreferences prefs;
	private String OKText;
	private boolean r_rated;
	private Resources resources;
	private String app_ver;	
	private int matrix_model;
	private final String tag = "LEDAlbum";	
	
	///********** Timers
	private ConnectTimer connectTimer; 
	private MediaScanTimer mediascanTimer; 
	private ImageDisplayDurationTimer imagedisplaydurationTimer;
	private PauseBetweenImagesDurationTimer pausebetweenimagesdurationTimer;
	//****************
	
	private boolean scanAllPics;
	private String setupInstructionsString; 
	private String setupInstructionsStringTitle;
	private int countdownCounter;
	private static final int countdownDuration = 20;
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private GestureDetector gestureDetector;
	private  OnTouchListener gestureListener;
	private int size;  //the number of pictures
	private int slideshowPosition = 0;
	private String imagePath;
	
	/**
     * Grid view holding the images.
     */
    private GridView sdcardImages;
    /**
     * Image adapter for the grid view.
     */
    private ImageAdapter imageAdapter;
    /**
     * Display used for getting the width of the screen. 
     */
    private Display display;
    
    private String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
 //   private String basepath = extStorageDirectory + "/ledalbum";
    private String basepath = extStorageDirectory;
    private String artpath = "/media";
    
    private Cursor cursor;
    private String[] projection;
    
     private Context context;
     private Context frameContext;
   // private MediaScannerConnection mConnection;
   // private String mPath;
  //  private String mMimeType; 
     
     private boolean slideShowMode;
     private int imageDisplayDuration;
     private int pauseBetweenImagesDuration;
     private GridView sdcard_;
     private TextView firstTimeSetupCounter_;
     

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); //force only portrait mode
        // Request progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.sdcard);
        display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
      //  sdcard_ = (Gridview) findViewById(R.id.sdcard); 
       sdcardImages = (GridView) findViewById(R.id.sdcard);
       firstTimeSetupCounter_ = (TextView) findViewById(R.id.firstTimeSetupCounter);
       
    // Gesture detection 
       gestureDetector = new GestureDetector(new MyGestureDetector());
       gestureListener = new View.OnTouchListener() {
    	   public boolean onTouch(View v, MotionEvent event) { 
    		   return gestureDetector.onTouchEvent(event);
    		   }
    	   };
       
        
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        try
        {
            app_ver = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        }
        catch (NameNotFoundException e)
        {
            Log.v(tag, e.getMessage());
        }
        
        //******** preferences code
        resources = this.getResources();
        setPreferences();
        //***************************
        
        connectTimer = new ConnectTimer(30000,5000); //pop up a message if it's not connected by this timer
 		connectTimer.start(); //this timer will pop up a message box if the device is not found
 		
 		imagedisplaydurationTimer = new ImageDisplayDurationTimer(imageDisplayDuration*1000,1000); //how long the image should display
 		pausebetweenimagesdurationTimer = new PauseBetweenImagesDurationTimer(pauseBetweenImagesDuration*1000,1000); //how long to show a blank screen before showing the next image
 		setupInstructionsString = getResources().getString(R.string.setupInstructionsString);
        setupInstructionsStringTitle = getResources().getString(R.string.setupInstructionsStringTitle);
        
        context = getApplicationContext();
        
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {

            extStorageDirectory = Environment.getExternalStorageDirectory().toString();
	           
            	// File artdir = new File(basepath + "/Android/data/com.ioiomint.ledalbum/files");
            	File artdir = new File(basepath + "/ledalbum");
	            if (!artdir.exists()) { //no directory so let's now start the one time setup
	            	sdcardImages.setVisibility(View.INVISIBLE); //hide the images as they're not loaded so we can show a splash screen instead
	            	//showToast(getResources().getString(R.string.oneTimeSetupString));
	            	artdir.mkdirs();
	                copyArt(); 
	                countdownCounter = (countdownDuration - 1); //because shorttoast is 2 seconds
	                mediascanTimer = new MediaScanTimer(countdownDuration*1000,1000); //pop up a message if it's not connected by this timer
 		            mediascanTimer.start(); //we need a delay here to give the me
	               
	            }
	            else { //the directory was already there so no need to copy files or do a media re-scan so just continue on
	            	continueOnCreate();
	            }

        } else if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED_READ_ONLY)) {

            showToast("Sorry, your device does not have an accessible SD card, this app will not work");//Or use your own method ie: Toast
        }
    }
    
    private void MediaScanCompleted() {
         continueOnCreate();
    }
    
    private void continueOnCreate() {
    	 sdcardImages.setVisibility(View.VISIBLE);
    	 setupViews();
         setProgressBarIndeterminateVisibility(true); 
         loadImages();
     
  		//BitmapBytes = new byte[KIND.width * KIND.height *2]; //512 * 2 = 1024 or 1024 * 2 = 2048
        // KIND = ioio.lib.api.RgbLedMatrix.Matrix.ADAFRUIT_32x16;
     	//BitmapInputStream = getResources().openRawResource(R.raw.selectpic);	
 	    //frame_ = new short [KIND.width * KIND.height];
 		//BitmapBytes = new byte[KIND.width * KIND.height *2]; //512 * 2 = 1024 or 1024 * 2 = 2048
    }
    
    
    private void CopyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", e.getMessage());
        }
        for (int i = 0; i < files.length; i++) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open("ledalbum/" + files[i]);
                // out = new FileOutputStream(basepath + "/ledalbum/" + files[i]);
                // File artdir = new File(extStorageDirectory +"/media/ledalbum/");
                 out = new FileOutputStream(basepath + artpath + "/ledalbum/" + files[i]);
               // in = assetManager.open(files[i]);
               // out = new FileOutputStream(extStorageDirectory + "/yourAppName/txt/" + files[i]);
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
            } catch (Exception e) {
                Log.e("tag", e.getMessage());
            }
        }
    }
    
    private void copyArt() {
    	
    	AssetManager assetManager = getResources().getAssets();
        String[] files = null;
        try {
            files = assetManager.list("ledalbum");
        } catch (Exception e) {
            Log.e("read clipart ERROR", e.toString());
            e.printStackTrace();
        }
        for(int i=0; i<files.length; i++) {
            InputStream in = null;
            OutputStream out = null;
            try {
              in = assetManager.open("ledalbum/" + files[i]);
              //out = new FileOutputStream(basepath + "/Android/data/com.ioiomint.ledalbum/files/" + files[i]);
              out = new FileOutputStream(basepath + "/ledalbum/" + files[i]);
              copyFile(in, out);
              in.close();
              in = null;
              out.flush();
              out.close();
              out = null;    
            
             
            MediaScannerConnection.scanFile(context,  //here is where we register the newly copied file to the android media content DB via forcing a media scan
	                        new String[] { basepath + "/ledalbum/" + files[i] }, null,
	                        new MediaScannerConnection.OnScanCompletedListener() {
	                    public void onScanCompleted(String path, Uri uri) {
	                        Log.i("ExternalStorage", "Scanned " + path + ":");
	                        Log.i("ExternalStorage", "-> uri=" + uri);
	                        
	                    }
	          });
           
            } catch(Exception e) {
                Log.e("copy clipart ERROR", e.toString());
                e.printStackTrace();
            }       
        }
        
    }
    
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
          out.write(buffer, 0, read);
        }
    }


    
    /**
     * Free up bitmap related resources.
     */
    protected void onDestroy() {
        super.onDestroy();
        final GridView grid = sdcardImages;
        final int count = grid.getChildCount();
        ImageView v = null;
        for (int i = 0; i < count; i++) {
            v = (ImageView) grid.getChildAt(i);
            ((BitmapDrawable) v.getDrawable()).setCallback(null);
        }
        
        connectTimer.cancel();  //if user closes the program, need to kill this timer or we'll get a crash
        imagedisplaydurationTimer.cancel();
 		pausebetweenimagesdurationTimer.cancel();
       // mediascanTimer.cancel(); 
        
    }
    /**
     * Setup the grid view.
     */
    private void setupViews() {
        //sdcardImages = (GridView) findViewById(R.id.sdcard);
        sdcardImages.setClipToPadding(false);
        sdcardImages.setNumColumns(display.getWidth()/95);
        sdcardImages.setOnItemClickListener(ImageThumbnailsActivity.this);
        
      //  sdcardImages.setOnClickListener((OnClickListener) ImageThumbnailsActivity.this);
        sdcardImages.setOnTouchListener(gestureListener);
        
        imageAdapter = new ImageAdapter(getApplicationContext()); 
        sdcardImages.setAdapter(imageAdapter);
    }
    /**
     * Load images.
     */
    private void loadImages() {
        final Object data = getLastNonConfigurationInstance();
        if (data == null) {
        	//new LoadImagesfromSDCard.set
        	//static Test t = new Test();
        	//public static void main(String[] args){
        	//t.Set(3);
        	
            new LoadImagesFromSDCard().execute();
        } else {
            final LoadedImage[] photos = (LoadedImage[]) data;
            if (photos.length == 0) {
                new LoadImagesFromSDCard().execute();
            }
            for (LoadedImage photo : photos) {
                addImage(photo);
            }
        }
    }
    /**
     * Add image(s) to the grid view adapter.
     * 
     * @param value Array of LoadedImages references
     */
    private void addImage(LoadedImage... value) {
        for (LoadedImage image : value) {
            imageAdapter.addPhoto(image);
            imageAdapter.notifyDataSetChanged();
        }
    }
    
    /**
     * Save bitmap images into a list and return that list. 
     * 
     * @see android.app.Activity#onRetainNonConfigurationInstance()
     */
    @Override
    public Object onRetainNonConfigurationInstance() {
        final GridView grid = sdcardImages;
        final int count = grid.getChildCount();
        final LoadedImage[] list = new LoadedImage[count];

        for (int i = 0; i < count; i++) {
            final ImageView v = (ImageView) grid.getChildAt(i);
            list[i] = new LoadedImage(((BitmapDrawable) v.getDrawable()).getBitmap());
        }

        return list;
    }
    
    /**
     * Async task for loading the images from the SD card. 
     * 
     * @author Mihai Fonoage
     *
     */
    class LoadImagesFromSDCard extends AsyncTask<Object, LoadedImage, Object> {
        
        /**
         * Load images from SD Card in the background, and display each image on the screen. 
         *  
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @SuppressLint("NewApi")
		@Override
        protected Object doInBackground(Object... params) {
            //setProgressBarIndeterminateVisibility(true); 
            Bitmap bitmap = null;
            Bitmap newBitmap = null;
            Uri uri = null;  
            
            String[] projection = {MediaStore.Images.Thumbnails._ID};
            if (scanAllPics == true) {
            	// Set up an array of the Thumbnail Image ID column we want
	            // Create the cursor pointing to the SDCard
	            cursor = managedQuery( MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
	                    projection, // Which columns to return
	                    null,       // Return all rows
	                    null,       
	                    null); 
            }
           else {
                 cursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
                 projection, 
                 MediaStore.Images.Media.DATA + " like ? ",
                 new String[] {"%ledalbum%"},  
                 null);
            }	
            
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails._ID);
            size = cursor.getCount();
            //showToast("numbr of pics: " + size);
            // If size is 0, there are no images on the SD Card.
            if (size == 0) {
                //No Images available, post some message to the user
            }
            int imageID = 0;
            for (int i = 0; i < size; i++) {
                cursor.moveToPosition(i);
                imageID = cursor.getInt(columnIndex);
                uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + imageID);
                try {
                    bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                    if (bitmap != null) {
                        newBitmap = Bitmap.createScaledBitmap(bitmap, 70, 70, true);
                        bitmap.recycle();
                        if (newBitmap != null) {
                            publishProgress(new LoadedImage(newBitmap));
                        }
                    }
                } catch (IOException e) {
                    //Error fetching image, try to recover
                }
            }
          //  cursor.close(); //this was causing crashing
            return null;
        }
        /**
         * Add a new LoadedImage in the images grid.
         *
         * @param value The image.
         */
        @Override
        public void onProgressUpdate(LoadedImage... value) {
            addImage(value);
        }
        /**
         * Set the visibility of the progress bar to false.
         * 
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Object result) {
            setProgressBarIndeterminateVisibility(false);
        }
    }

    /**
     * Adapter for our image files. 
     * 
     * @author Mihai Fonoage
     *
     */
    class ImageAdapter extends BaseAdapter {

        private Context mContext; 
        private ArrayList<LoadedImage> photos = new ArrayList<LoadedImage>();

        public ImageAdapter(Context context) { 
            mContext = context; 
        } 

        public void addPhoto(LoadedImage photo) { 
            photos.add(photo); 
        } 

        public int getCount() { 
            return photos.size(); 
        } 

        public Object getItem(int position) { 
            return photos.get(position); 
        } 

        public long getItemId(int position) { 
            return position; 
        } 

        public View getView(int position, View convertView, ViewGroup parent) { 
            final ImageView imageView; 
            if (convertView == null) { 
                imageView = new ImageView(mContext); 
            } else { 
                imageView = (ImageView) convertView; 
            } 
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(8, 8, 8, 8);
            imageView.setImageBitmap(photos.get(position).getBitmap());
            return imageView; 
        } 
    }

    /**
     * A LoadedImage contains the Bitmap loaded for the image.
     */
    private static class LoadedImage {
        Bitmap mBitmap;

        LoadedImage(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.mainmenu, menu);
       return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
       
		
      if (item.getItemId() == R.id.menu_instructions) {
 	    	AlertDialog.Builder alert=new AlertDialog.Builder(this);
 	      	alert.setTitle(setupInstructionsStringTitle).setIcon(R.drawable.icon).setMessage(setupInstructionsString).setNeutralButton(OKText, null).show();
 	   }
    	
	  if (item.getItemId() == R.id.menu_about) {
		  
		    AlertDialog.Builder alert=new AlertDialog.Builder(this);
	      	alert.setTitle(getString(R.string.menu_about_title)).setIcon(R.drawable.icon).setMessage(getString(R.string.menu_about_summary) + "\n\n" + getString(R.string.versionString) + " " + app_ver).setNeutralButton(OKText, null).show();	
	   }
    	
    	if (item.getItemId() == R.id.menu_prefs)
       {
    		
    		Intent intent = new Intent()
       				.setClass(this,
       				com.ioiomint.ledalbum.preferences.class);   
    				this.startActivityForResult(intent, 0);
       }
    	
    	if (item.getItemId() == R.id.menu_rescan)
        {
     		
     		Intent intent = new Intent()
        				.setClass(this,
        				com.ioiomint.ledalbum.rescan.class);   
     				this.startActivityForResult(intent, 1);
        }   	
    	
       return true;
    }
    
    


@Override
    public void onActivityResult(int reqCode, int resCode, Intent data) //we'll go into a reset after this
    {
    	super.onActivityResult(reqCode, resCode, data);    	
    	setPreferences(); //very important to have this here, after the menu comes back this is called, we'll want to apply the new prefs without having to re-start the app
    	
    	if (reqCode == 0 || reqCode == 1) //then we came back from the preferences menu so re-load all images from the sd card
        {
    		imagedisplaydurationTimer.cancel(); //we may have been running a slideshow so kill it
    	    pausebetweenimagesdurationTimer.cancel();
    		setupViews();
    	    setProgressBarIndeterminateVisibility(true); 
    	    loadImages();      
        }
    } 
    
    private void setPreferences() //here is where we read the shared preferences into variables
    {
     SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);     
    
     scanAllPics = prefs.getBoolean("pref_scanAll", false);
     slideShowMode = prefs.getBoolean("pref_slideshowMode", false);
     
     imageDisplayDuration = Integer.valueOf(prefs.getString(   
  	        resources.getString(R.string.pref_imageDisplayDuration),
  	        resources.getString(R.string.imageDisplayDurationDefault)));   
     
     pauseBetweenImagesDuration = Integer.valueOf(prefs.getString(   
  	        resources.getString(R.string.pref_pauseBetweenImagesDuration),
  	        resources.getString(R.string.pauseBetweenImagesDurationDefault)));  
     
     matrix_model = Integer.valueOf(prefs.getString(   //the selected RGB LED Matrix Type
    	        resources.getString(R.string.selected_matrix),
    	        resources.getString(R.string.matrix_default_value))); 
     
     switch (matrix_model) {  //get this from the preferences
     case 0:
    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32;
    	 BitmapInputStream = getResources().openRawResource(R.raw.selectpic32);
    	 break;
     case 1:
    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x16;
    	 BitmapInputStream = getResources().openRawResource(R.raw.selectpic);
    	 break;
     case 2:
    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.ADAFRUIT_32x16;
    	 BitmapInputStream = getResources().openRawResource(R.raw.selectpic);
    	 break;
     default:	    		 
    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32;
    	 BitmapInputStream = getResources().openRawResource(R.raw.selectpic32);
     }
         
     frame_ = new short [KIND.width * KIND.height];
	 BitmapBytes = new byte[KIND.width * KIND.height *2]; //512 * 2 = 1024 or 1024 * 2 = 2048
	 
		try {
   			int n = BitmapInputStream.read(BitmapBytes, 0, BitmapBytes.length); // reads
   																				// the
   																				// input
   																				// stream
   																				// into
   																				// a
   																				// byte
   																				// array
   			Arrays.fill(BitmapBytes, n, BitmapBytes.length, (byte) 0);
   		} catch (IOException e) {
   			e.printStackTrace();
   		}

   		int y = 0;
   		for (int i = 0; i < frame_.length; i++) {
   			frame_[i] = (short) (((short) BitmapBytes[y] & 0xFF) | (((short) BitmapBytes[y + 1] & 0xFF) << 8));
   			y = y + 2;
   		}
	 
	// canvasBitmap = Bitmap.createBitmap(KIND.width, KIND.height, Config.RGB_565); 
	// Canvas canvas = new Canvas(canvasBitmap);
	// canvas.drawBitmap(originalImage, 0, 0, null);
   //  ByteBuffer buffer = ByteBuffer.allocate(KIND.width * KIND.height *2); //Create a new buffer
	// canvasBitmap.copyPixelsToBuffer(buffer); //copy the bitmap 565 to the buffer		
	// BitmapBytes = buffer.array(); //copy the buffer into the type array
   //  loadImage(); 
     
     
 }
      
    
   
	
    
    public class ConnectTimer extends CountDownTimer
	{

		public ConnectTimer(long startTime, long interval)
			{
				super(startTime, interval);
			}

		@Override
		public void onFinish()
			{
				if (deviceFound == 0) {
					showNotFound (); 					
				}
				
			}

		@Override
		public void onTick(long millisUntilFinished)				{
			//not used
		}
	}
    
    public class MediaScanTimer extends CountDownTimer
   	{

   		public MediaScanTimer(long startTime, long interval)
   			{
   				super(startTime, interval);
   			}

   		@Override
   		public void onFinish()
   			{
   				
   				MediaScanCompleted();
   				countdownCounter = countdownDuration; //reset the counter
   			}

   		@Override
   		public void onTick(long millisUntilFinished)				{
   			//showToastShort("ONE TIME SETUP: Copying stock pictures to your SD card. Please Wait..." + countdownCounter);
   			setfirstTimeSetupCounter(Integer.toString(countdownCounter));
   			//showToastShort(getResources().getString(R.string.oneTimeSetupString) + " " + countdownCounter);
   			countdownCounter--;
   		}
   	}
	
	private void showNotFound() {	
		AlertDialog.Builder alert=new AlertDialog.Builder(this);
		alert.setTitle(getResources().getString(R.string.notFoundString)).setIcon(R.drawable.icon).setMessage(getResources().getString(R.string.bluetoothPairingString)).setNeutralButton(getResources().getString(R.string.OKText), null).show();	
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
    
    private void showToast(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast toast = Toast.makeText(ImageThumbnailsActivity.this, msg, Toast.LENGTH_LONG);
                toast.show();
			}
		});
	}  
    
    private void showToastShort(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast toast = Toast.makeText(ImageThumbnailsActivity.this, msg, Toast.LENGTH_SHORT);
                toast.show();
			}
		});
	}  
    
    private void setfirstTimeSetupCounter(final String str) {
		runOnUiThread(new Runnable() {
			public void run() {
				firstTimeSetupCounter_.setText(str);
			}
		});
	}

    /**
     * Adapter for our image files.
     */
 
 
    
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {        
        
    	 // Get the data location of the image
        String[] projection = {MediaStore.Images.Media.DATA};
        
        if (scanAllPics == true) {
            
            cursor = managedQuery( MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, // Which columns to return
                null,       // Return all rows
                null,
                null);
        }
        else {
        	cursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, 
                MediaStore.Images.Media.DATA + " like ? ",
                new String[] {"%ledalbum%"},  
                null);
        } 
        
        showToast("on click");
        
        columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToPosition(position);
        // Get image filename
        imagePath = cursor.getString(columnIndex);
        System.gc();
         
        //***** was calling the IOIO piece from a new intent but removed this *******
        // Intent intent = new Intent(getApplicationContext(), ViewImage.class);
        // intent.putExtra("filename", imagePath);
        // startActivity(intent);
        
        WriteImagetoMatrix();
       
    }
    
    private void WriteImagetoMatrix() {
    	  originalImage = BitmapFactory.decodeFile(imagePath);   		 
    		 width_original = originalImage.getWidth();
    		 height_original = originalImage.getHeight();
    		 
    		 if (width_original != KIND.width || height_original != KIND.height) {
    			 //the iamge is not the right dimensions, so we need to re-size
    			 scaleWidth = ((float) KIND.width) / width_original;
 	   		 scaleHeight = ((float) KIND.height) / height_original;
 	   		 // create matrix for the manipulation
 	   		 matrix2 = new Matrix();
 	   		 // resize the bit map
 	   		 matrix2.postScale(scaleWidth, scaleHeight);
 	   		 resizedBitmap = Bitmap.createBitmap(originalImage, 0, 0, width_original, height_original, matrix2, true);
 	   		 canvasBitmap = Bitmap.createBitmap(KIND.width, KIND.height, Config.RGB_565); 
 	   		 Canvas canvas = new Canvas(canvasBitmap);
 	   		 canvas.drawRGB(0,0,0);
 	   	   	 canvas.drawBitmap(resizedBitmap, 0, 0, null);
 	   		 ByteBuffer buffer = ByteBuffer.allocate(KIND.width * KIND.height *2); //Create a new buffer
 	   		 canvasBitmap.copyPixelsToBuffer(buffer); //copy the bitmap 565 to the buffer		
 	   		 BitmapBytes = buffer.array(); //copy the buffer into the type array
    		 }
    		 else {
    			 // //the image is already the right dimensions, no needs to resize
    			 canvasBitmap = Bitmap.createBitmap(KIND.width, KIND.height, Config.RGB_565); 
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
    
    
    private void SlideShow() {
    	
    	 String[] projection = {MediaStore.Images.Media.DATA};
	        
	        if (scanAllPics == true) {
	            
	            cursor = managedQuery( MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
	                projection, // Which columns to return
	                null,       // Return all rows
	                null,
	                null);
	        }
	        else {
	        	cursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
	                projection, 
	                MediaStore.Images.Media.DATA + " like ? ",
	                new String[] {"%ledalbum%"},  
	                null);
	        } 
	        
	       // showToast("started slideshow");
	        
	        columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	        
	        if (slideshowPosition > size) { //let's make sure we haven't reached the end
	        	slideshowPosition = 0;
	        }
	        
	        cursor.moveToPosition(slideshowPosition);
	        slideshowPosition++; //increment it so we can play the next one
	        
	        // Get image filename
	        imagePath = cursor.getString(columnIndex);
	        System.gc();
	        showToastShort("path: " + imagePath);
	        WriteImagetoMatrix();
	        
	        imagedisplaydurationTimer.start(); //the image will stay on for as long as this timer;
    	
    }
    
    private void stopSlideShow() { //stop the slideshow
    	imagedisplaydurationTimer.cancel();
 		pausebetweenimagesdurationTimer.cancel();
 		
    	
    }
    
    private void clearMatrixImage() {
    	//let's claear the image
    	
    	//then let's start another timer to load the next image
    	pausebetweenimagesdurationTimer.start();  //how long the rgb matrix should be of before showing the next image
    }
    
    public class ImageDisplayDurationTimer extends CountDownTimer
	{

		public ImageDisplayDurationTimer(long startTime, long interval)
			{
				super(startTime, interval);
			}

		@Override
		public void onFinish()
			{
			//imagedisplaydurationTimer.cancel();
			clearMatrixImage();
				
			}

		@Override
		public void onTick(long millisUntilFinished)				{
			//not used
		}
	}
 
 public class PauseBetweenImagesDurationTimer extends CountDownTimer
	{

		public PauseBetweenImagesDurationTimer(long startTime, long interval)
			{
				super(startTime, interval);
			}

		@Override
		public void onFinish()
			{
			SlideShow(); //we've paused long enough, show the next image
				
			}

		@Override
		public void onTick(long millisUntilFinished)				{
			//not used
		}
	}
    
    
    
    class MyGestureDetector extends SimpleOnGestureListener {       
    	@Override        
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) { 
    		
    		try {                
    			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
    				return false;                // right to left swipe
    			if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
    				Toast.makeText(ImageThumbnailsActivity.this, "Left Swipe", Toast.LENGTH_SHORT).show();
    				stopSlideShow();
    				sdcardImages.setOnItemClickListener(ImageThumbnailsActivity.this);
    				//add onclick listener back?
    				
    				
    				}  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) { 
    					Toast.makeText(ImageThumbnailsActivity.this, "Right Swipe", Toast.LENGTH_SHORT).show();
    					SlideShow(); //start or resume the slideshow
    					//sdcardImages.removeOnItemClickListener(ImageThumbnailsActivity.this);
    					//sdcardImages.remo
    					//remove onclick listener?
    					
    					
    					
    					}            } catch (Exception e) {                // nothing
    						
    					}            return false;
    				}
    		}
    
}
    
    
    
    
    




























