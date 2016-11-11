package course.examples.Graphics.CanvasBubbleSurfaceView;

import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

/* @author A. Porter
 * Revised by S. Anderson
 */
public class BubbleActivity extends Activity {

	BubbleView mBubbleView;
	RelativeLayout relativeLayout;
	String frameString;
	public int count = 1;
	public float isLessThanTenSeconds = System.currentTimeMillis();

	/** Simply create layout and view. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		relativeLayout = (RelativeLayout) findViewById(R.id.subframe);
		// decode resource into a bitmap
		mBubbleView = new BubbleView(getApplicationContext(), BitmapFactory.decodeResource(getResources(), R.drawable.b256));

		relativeLayout.addView(mBubbleView);
	}

	private void runThread(){
		runOnUiThread (new Thread(new Runnable() {
			public void run() {
				TextView textView = (TextView) findViewById(R.id.framerateText);
				textView.setText(frameString);
			}
		}));
	}

	@Override
	public boolean onTouchEvent(MotionEvent event){
		super.onTouchEvent(event);
		int action = event.getAction();
		float x = event.getX();
		float y = event.getY();
		float otherX = mBubbleView.getmX();
		float otherY = mBubbleView.getmY();
		switch(action) {
			case MotionEvent.ACTION_DOWN:
				if (x < otherX + 500 && x >= otherX && y < otherY && y >= otherY - 500) {
					count++;
					relativeLayout.removeAllViewsInLayout();
					mBubbleView = new BubbleView(getApplicationContext(), BitmapFactory.decodeResource(getResources(), R.drawable.b256));
					relativeLayout.addView(mBubbleView);
				}
				break;
		}



		Log.d("SCORE", "" + count);


		return false;
	}


	/*
	  SurfaceView is dedicated drawing surface in the view hierarchy.
      SurfaceHolder.Callback determines changes to SurfaceHolder via surfaceXXX
      callbacks.
	 */
	private class BubbleView extends SurfaceView implements
			SurfaceHolder.Callback {

		private long beginTimeMillis, endTimeMillis;
		private final Bitmap mBitmap;
		private int mBitmapHeightAndWidth, mBitmapHeightAndWidthAdj;
		private final DisplayMetrics mDisplay;
		private final int mDisplayWidth, mDisplayHeight;
		private float mX, mY, mDx, mDy, mRotation;
		private final SurfaceHolder mSurfaceHolder;
		private final Paint mPainter = new Paint(); // control style and color
		private Thread mDrawingThread;

		private static final int MOVE_STEP = 1;
		private static final float ROT_STEP = 0.5f;

		public BubbleView(Context context, Bitmap bitmap) {
			super(context);
			mBitmapHeightAndWidth = (int) getResources().getDimension(R.dimen.image_height);
			if (count > 1) {
				for (int i = 1; i < count; i++) {
					mBitmapHeightAndWidth -= mBitmapHeightAndWidth / 10;
				}
			}

			this.mBitmap = Bitmap.createScaledBitmap(bitmap,
					mBitmapHeightAndWidth / 5, mBitmapHeightAndWidth / 5, false);

			mBitmapHeightAndWidthAdj = mBitmapHeightAndWidth / 2;

			mDisplay = new DisplayMetrics();
			// get display width/height
			BubbleActivity.this.getWindowManager().getDefaultDisplay()
					.getMetrics(mDisplay);
			mDisplayWidth = mDisplay.widthPixels;
			mDisplayHeight = mDisplay.heightPixels;

			// Give bubble random coords and speed at creation
			Random r = new Random();
			mX = (float) r.nextInt(mDisplayHeight);
			mY = (float) r.nextInt(mDisplayWidth);
			mDx = (float) r.nextInt(mDisplayHeight) / mDisplayHeight;
			mDx *= r.nextInt(2) == 1 ? MOVE_STEP : -1 * MOVE_STEP;
			mDy = (float) r.nextInt(mDisplayWidth) / mDisplayWidth;
			mDy *= r.nextInt(2) == 1 ? MOVE_STEP : -1 * MOVE_STEP;
			mDx *= 10.0f;
			mDy *= 10.0f;
			if (count > 1) {
				for (int i = 1; i < count; i++) {
					mDx += mDx / 10;
					mDy += mDy / 10;
				}
			}
			mRotation = 1000.0f * r.nextFloat();

			mPainter.setAntiAlias(true); // smooth edges of bitmap
			// This will take care of changes to the bitmap
			mSurfaceHolder = getHolder();
			mSurfaceHolder.addCallback(this);
		}


		/** drawing and rotation */
		private void drawBubble(Canvas canvas) {
			canvas.drawColor(Color.DKGRAY);
			mRotation += ROT_STEP;
			canvas.rotate(mRotation, mY + mBitmapHeightAndWidthAdj, mX
					+ mBitmapHeightAndWidthAdj);
			canvas.drawBitmap(mBitmap, mY, mX, mPainter);
		}

		/** True iff bubble can move. */
		private boolean move() {

			mX += mDx;
			mY += mDy;

			if (mX < 0 - mBitmapHeightAndWidth
					|| mX > mDisplayHeight + mBitmapHeightAndWidth
					|| mY < 0 - mBitmapHeightAndWidth
					|| mY > mDisplayWidth + mBitmapHeightAndWidth) {
				Log.d("COMPLETE", "GAME OVER");
				return false;
			} else {
				return true;
			}
		}

		private float getmX(){
			return mX;
		}

		private float getmY(){
			return mY;
		}

		/** Does nothing for surface change */
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
		}

		/** When surface created, this creates its thread AND starts it running. */
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// Run as separate thread.
			mDrawingThread = new Thread(new Runnable() {
				public void run() {
					Canvas canvas = null;
					// While bubble within view, lock and draw.
					while (!Thread.currentThread().isInterrupted() && move()) {
						beginTimeMillis = System.currentTimeMillis();
						canvas = mSurfaceHolder.lockCanvas();

						if (null != canvas) { // Lock canvas while updating bitmap
							drawBubble(canvas);
							mSurfaceHolder.unlockCanvasAndPost(canvas);
						}
						if ((System.currentTimeMillis() - isLessThanTenSeconds) > 10) {
							count = 0;
							isLessThanTenSeconds = System.currentTimeMillis();
						}

						endTimeMillis = System.currentTimeMillis() - beginTimeMillis;
						frameString = getString(R.string.report_framerate, Long.toString(endTimeMillis));
						runThread();
				}

				}
			});
			mDrawingThread.start();

		}
		/** Surface destroyed; stop thread. */
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (null != mDrawingThread)
				mDrawingThread.interrupt();
		}

	}
}