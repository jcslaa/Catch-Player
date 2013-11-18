package com.ffmpegtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.StrictMode;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.appunite.ffmpeg.FFmpegDisplay;
import com.appunite.ffmpeg.FFmpegError;
import com.appunite.ffmpeg.FFmpegListener;
import com.appunite.ffmpeg.FFmpegPlayer;
import com.appunite.ffmpeg.FFmpegStreamInfo;
import com.appunite.ffmpeg.NotPlayingException;
import com.ffmpegtest.adapter.VideoFileDBAdapter;
import com.ffmpegtest.helpers.AudioFingerPrintHelper;
import com.ffmpegtest.helpers.JSONHelper;
import com.ffmpegtest.helpers.Util;

public class VideoActivity extends Activity implements FFmpegListener, OnClickListener, OnSeekBarChangeListener, OnTouchListener, View.OnSystemUiVisibilityChangeListener
{
	public static FFmpegPlayer mMpegPlayer;
	//////////////////////////////////////////////////
	// UI Variable
	//////////////////////////////////////////////////
	private View mFullLayout;
	private View mVideoView;
	private View mTitleBar;
	private TextView mTitle;
	private View mControlsView;
	private SeekBar mSeekBar;
	private ImageButton mHoldButton;
	private ImageButton mPlayPauseButton;
	private TextView mCurrentTime;
	private TextView mTotalTime;
	
	private View mSeekVariationView;
	private TextView mSeekCurrentTimeValue;
	private TextView mSeekVariationValue;
	private View mVolumeBrightnessVariationView;
	private TextView mVolumeBrightnessValue;
	private ImageView mVolumeBrightnessImage;
	
	private TextView mSubtitleView;
	
	private View mUnHoldButtonView;
	private ImageButton mUnHoldButton;
	
	private ImageView mPPLButton;
	private ViewPager mPPLViewPager;
	private RelativeLayout mPPLLayout;
	private LinearLayout mPageMark;
	
	public static ProgressDialog progess;
	
	//////////////////////////////////////////////////
	// Value Variable
	//////////////////////////////////////////////////
	private int currentTimeS;
	private float brightnessValue;
	private Boolean brightnessCheck;
	private Handler mSeekControlHandler;
	private Handler mControllerHandler;
	private Handler mHoldHandler;
	private int mPrevPosition;
	private Boolean holdCheck;
	private AudioManager mAudioManager;
	private int mAudioMax;
	private float mVolume;
	private boolean onPPL = false;
	private boolean mHold = false;
	private boolean mMove = false;
	private boolean mSeek = false;
	private boolean isFinish = false;
	private boolean mUseSubtitle = false;
	private int seekValue;
	private float mTouchX;
	private float mTouchY;
	private int mAudioStreamNo = FFmpegPlayer.UNKNOWN_STREAM;
	private int mSubtitleStreamNo = FFmpegPlayer.NO_STREAM;
	private boolean mTouchPressed = false;
	ArrayList<String> videoList;
	ArrayList<SubtitleData> parsedSubtitleDataList;
	private File file;
	public static String path;
	private int index;
	private int indexSubtitle;
	private long currentTime;
	private boolean mPlay = false;
	private VideoFileDBAdapter dbAdapter;
	private Util util = Util.getInstance();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DITHER);

		super.onCreate(savedInstanceState);

		getWindow().setBackgroundDrawable(null);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.video_surfaceview);

		mFullLayout = this.findViewById(R.id.full_layout);
		mFullLayout.setOnTouchListener(this);

		mTitleBar = this.findViewById(R.id.title_bar);
		mTitle = (TextView) this.findViewById(R.id.title);

		mControlsView = this.findViewById(R.id.controls);
		mControlsView.setOnTouchListener(this);

		mVolumeBrightnessVariationView = this.findViewById(R.id.volume_brightness_variation_view);
		mVolumeBrightnessValue = (TextView)this.findViewById(R.id.volume_brightness_value);
		mVolumeBrightnessImage = (ImageView)this.findViewById(R.id.volume_brightness_image);

		mSeekVariationView = this.findViewById(R.id.seek_variation_view);
		mSeekCurrentTimeValue = (TextView)this.findViewById(R.id.current_time_value);
		mSeekVariationValue = (TextView)this.findViewById(R.id.seek_variation_value);

		mSeekBar = (SeekBar) this.findViewById(R.id.seek_bar);
		mSeekBar.setOnSeekBarChangeListener(this);

		mPlayPauseButton = (ImageButton) this.findViewById(R.id.play_pause);
		mPlayPauseButton.setOnClickListener(this);
		
		mControllerHandler = new Handler();
		
		mHoldButton = (ImageButton) this.findViewById(R.id.hold_video);
		mHoldButton.setOnClickListener(this);

		mPPLButton = (ImageView) this.findViewById(R.id.ppl_button);
		mPPLButton.setOnClickListener(this);

		mCurrentTime = (TextView) this.findViewById(R.id.current_time);
		mTotalTime = (TextView) this.findViewById(R.id.total_time);

		mVideoView = this.findViewById(R.id.video_view);
		mPPLLayout = (RelativeLayout)this.findViewById(R.id.ppl_view);
		mPageMark = (LinearLayout) this.findViewById(R.id.page_mark);
		mPPLViewPager = (ViewPager) this.findViewById(R.id.view_pager);
		mPPLViewPager.setOnPageChangeListener(new OnPageChangeListener() {    //아이템이 변경되면

			//아이템이 선택이 되었으면
			@Override 
			public void onPageSelected(int position) {
				//이전 페이지에 해당하는 페이지 표시 이미지 변경
				mPageMark.getChildAt(mPrevPosition).setBackgroundResource(R.drawable.page_not);

				//현재 페이지에 해당하는 페이지 표시 이미지 변경    
				mPageMark.getChildAt(position).setBackgroundResource(R.drawable.page_select);
				mPrevPosition = position;                //이전 포지션 값을 현재로 변경
			}
			@Override public void onPageScrolled(int position, float positionOffest, int positionOffsetPixels) {}
			@Override public void onPageScrollStateChanged(int state) {}
		});

		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setAlpha(160);
		mPPLLayout.setBackgroundColor(paint.getColor());

		mUnHoldButtonView = this.findViewById(R.id.unhold_area);
		mUnHoldButton = (ImageButton) this.findViewById(R.id.unhold_button);
		mUnHoldButton.setOnClickListener(this);

		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

		dbAdapter = new VideoFileDBAdapter(this);

		//홀드버튼
		holdCheck = true;
		brightnessCheck = false;

		if(android.os.Build.VERSION.SDK_INT > 9){
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		doBrightnessTouch(0.5f);

		mMpegPlayer = null;
		mMpegPlayer = new FFmpegPlayer((FFmpegDisplay) mVideoView, this);
		mMpegPlayer.setMpegListener(this);
		progess = util.getProgress(this);

		setDataSource();

		mMpegPlayer.resume();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mPlay = false;
		mMpegPlayer.pause();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		this.mMpegPlayer.setMpegListener(null);
		this.mMpegPlayer.stop();
		stop();
	}

	private void setDataSource()
	{
		HashMap<String, String> params = new HashMap<String, String>();

		// set font for ass
		File assFont = new File(Environment.getExternalStorageDirectory(), "DroidSansFallback.ttf");
		params.put("ass_default_font_path", assFont.getAbsolutePath());

		if(videoList == null) {
			Intent intent = getIntent();
			Uri uri = intent.getData();

			if (uri != null)
			{
				path = uri.toString();
			}
			else
			{
				videoList = intent.getStringArrayListExtra(AppConstants.VIDEO_PLAY_ACTION_LIST);
				index = intent.getIntExtra(AppConstants.VIDEO_PLAY_ACTION_INDEX, 0);
				file = new File(videoList.get(index));
				path = file.getAbsolutePath();
			}
		}

		String[] split = path.split("/");
		String title = split[split.length - 1];
		mTitle.setText(title);

		this.mPlayPauseButton.setImageResource(R.drawable.pause);
		this.mPlayPauseButton.setEnabled(true);

		mPlay = true;
		mTouchPressed = false;
		mHold = false;

		int time = dbAdapter.getVideoTime(path);

		mMpegPlayer.setDataSource(path, params, FFmpegPlayer.UNKNOWN_STREAM, mAudioStreamNo, mSubtitleStreamNo);
		if(time > 0)
			mMpegPlayer.seek(String.valueOf(time));

		setSubtitleSource();

		if(mUseSubtitle == true)
			executeSubtitleThread();
		Log.e("filePath : ", path);


		JSONHelper.dramaName = "";
		String finger = dbAdapter.getVideoFingerPrint(path);
		Log.e("fingerPrint", "Test : "+finger);

		if(finger != null && finger.equals(""))
			new AudioFingerPrintHelper(MainActivity.mFFmpegInstallPath, path).fingerTask.execute(this);
		else
			JSONHelper.dramaName = finger;
	}

	public void setSubtitleSource()
	{
		String subtitlePath = path.substring(0, path.lastIndexOf(".")) + ".smi";
		File subtitleFile = new File(subtitlePath);

		if(subtitleFile.isFile() && subtitleFile.canRead())
		{
			mUseSubtitle = true;
			mSubtitleView = (TextView)findViewById(R.id.subtitle_view);
			parsedSubtitleDataList = new ArrayList<SubtitleData>();
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(subtitleFile.toString())), "MS949"));
				String s;
				long time = -1;
				String text = null;
				boolean startSubtitle = false;

				while((s = in.readLine()) != null)
				{
					if(s.contains("<SYNC"))
					{
						startSubtitle = true;
						if(time != -1) {
							parsedSubtitleDataList.add(new SubtitleData(time, text));
						}

						time = Integer.parseInt(s.substring(s.indexOf("=")+1, s.indexOf(">")));
						text = s.substring(s.indexOf(">")+1, s.length());
						text = text.substring(text.indexOf(">")+1, text.length());
					}
					else
					{
						if(startSubtitle == true)
							text = text + s;
					}
				}

				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
			mUseSubtitle = false;
	}

	public void executeSubtitleThread()
	{
		new Thread(new Runnable() {

			@Override
			public void run() {
				while(mUseSubtitle) {
					try {
						Thread.sleep(200);
						subtitleHandler.sendMessage(subtitleHandler.obtainMessage());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				parsedSubtitleDataList.clear();
				indexSubtitle = 0;
			}
		}).start();
	}

	Handler subtitleHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			if(currentTime > 0)
			{
				try {
					indexSubtitle = getIndexSubtitle(currentTime);

					if(indexSubtitle == -1)
						mSubtitleView.setText("");
					else
						mSubtitleView.setText(Html.fromHtml(parsedSubtitleDataList.get(indexSubtitle).getText()));

				} catch(Exception e) {}
			}
		}
	};

	public int getIndexSubtitle(long currentTime)
	{
		int l = 0;
		int m;
		int h = parsedSubtitleDataList.size();

		while(l <= h)
		{
			m = (l + h) / 2;
			if(parsedSubtitleDataList.get(m).getTime() <= currentTime && currentTime < parsedSubtitleDataList.get(m+1).getTime())
				return m;
			if(currentTime > parsedSubtitleDataList.get(m+1).getTime())
				l = m + 1;
			else
				h = m - 1;
		}

		return -1;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		if(holdCheck==true){
			DisplayMetrics screen = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(screen);

			float x_changed = event.getRawX() - mTouchX;
			float y_changed = event.getRawY() - mTouchY;

			float coef = Math.abs (y_changed / x_changed);
			float xgesturesize = ((x_changed / screen.xdpi) * 2.54f);

			if(event.getAction() == MotionEvent.ACTION_MOVE && onPPL == false)
			{
				mMove = true;	
				mControllerHandler.removeMessages(0);//////////////////
				
				if(coef > 3)
				{
					mSeekVariationView.setVisibility(View.GONE);
					if(mTouchX < (getDeviceWidth() / 2))
					{
						doBrightnessTouch(y_changed);
						mControllerHandler = new Handler(){
							@Override
							public void handleMessage(Message msg) {
								mVolumeBrightnessVariationView.setVisibility(View.GONE);
							}
						};
						this.mVolumeBrightnessVariationView.setVisibility(View.VISIBLE);
					}
					if(mTouchX > (getDeviceWidth() / 2))
					{
						doVolumeTouch(y_changed);
						mControllerHandler = new Handler(){
							@Override
							public void handleMessage(Message msg) {
								mVolumeBrightnessVariationView.setVisibility(View.GONE);
							}
						};
						this.mVolumeBrightnessValue.setText(""+mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
						this.mVolumeBrightnessImage.setBackgroundResource(R.drawable.sound);
						this.mVolumeBrightnessVariationView.setVisibility(View.VISIBLE);
					}

					return true;
				}else if(coef < 3 || mSeekVariationView.getVisibility() == View.VISIBLE)
				{
					if(xgesturesize < 0.02 && xgesturesize > -0.02)
					{
						mMove = false;
					}else if(coef < 0.5 && Math.abs(xgesturesize) > 1)
					{
						mVolumeBrightnessVariationView.setVisibility(View.GONE);

						Log.e("SeekBartest", "                                               seekbar");
						mSeekControlHandler = new Handler(){
							@Override
							public void handleMessage(Message msg) {
								mSeekVariationView.setVisibility(View.GONE);
							}
						};
						this.mSeekVariationValue.setText("[ "+((currentTimeS>seekValue)?"-":"+")+parseTime(Math.abs(currentTimeS-seekValue))+" ]");
						this.mSeekCurrentTimeValue.setText(parseTime(currentTimeS));
						this.mSeekVariationView.setVisibility(View.VISIBLE);

						doSeekTouch(coef, xgesturesize, false);        

					}
				}

				return true;
			}
			else if(event.getAction() == MotionEvent.ACTION_DOWN)
			{
				mTouchX = event.getRawX();
				mTouchY = event.getRawY();

				mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

			}
			else if(event.getAction() == MotionEvent.ACTION_UP)
			{
				Log.e("GestureSize Up", ""+xgesturesize);

				if(mSeekVariationView.getVisibility()==View.VISIBLE){
					mSeekControlHandler.sendEmptyMessageDelayed(0, 1000);

				}else if(mVolumeBrightnessVariationView.getVisibility()==View.VISIBLE){
					mControllerHandler.sendEmptyMessageDelayed(0, 1000);
				}

				if(mMove == true)
				{
					mMove = false;
					
					mControllerHandler = new Handler(){
						@Override
						public void handleMessage(Message msg) {
							mTitleBar.setVisibility(View.GONE);
							mControlsView.setVisibility(View.GONE);
							mPPLButton.setVisibility(View.GONE);
							if(mUseSubtitle) {
								RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
								params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
								params.addRule(RelativeLayout.CENTER_HORIZONTAL);
								params.setMargins(20, 20, 20, 20);

								mSubtitleView.setLayoutParams(params);
							}
							mTouchPressed = false;
						}
					};
					this.mTitleBar.setVisibility(View.VISIBLE);
					this.mControlsView.setVisibility(View.VISIBLE);
					this.mPPLButton.setVisibility(View.VISIBLE);
					if(mUseSubtitle) {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
						params.addRule(RelativeLayout.ABOVE, mControlsView.getId());
						params.addRule(RelativeLayout.CENTER_HORIZONTAL);
						params.setMargins(20, 20, 20, 20);

						mSubtitleView.setLayoutParams(params);
					}
					
					mControllerHandler.sendEmptyMessageDelayed(0, 4000);
					
					if(mSeek==true)
					{
						mSeek = false;

						mMpegPlayer.seek(String.valueOf(seekValue));

						return true;
					}

					
					return true;
				}

				if(mHold == false)
				{
					if(mPPLLayout.getVisibility() == View.GONE)///////////////////////////////////////////////////////////수정요망
					{
						if(mTouchPressed == false)
						{
							mTouchPressed = true;

							mControllerHandler = new Handler(){
								@Override
								public void handleMessage(Message msg) {
									mTitleBar.setVisibility(View.GONE);
									mControlsView.setVisibility(View.GONE);
									mPPLButton.setVisibility(View.GONE);
									if(mUseSubtitle) {
										RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
										params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
										params.addRule(RelativeLayout.CENTER_HORIZONTAL);
										params.setMargins(20, 20, 20, 20);

										mSubtitleView.setLayoutParams(params);
									}
									mTouchPressed = false;
								}
							};
							this.mTitleBar.setVisibility(View.VISIBLE);
							this.mControlsView.setVisibility(View.VISIBLE);
							this.mPPLButton.setVisibility(View.VISIBLE);
							if(mUseSubtitle) {
								RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
								params.addRule(RelativeLayout.ABOVE, mControlsView.getId());
								params.addRule(RelativeLayout.CENTER_HORIZONTAL);
								params.setMargins(20, 20, 20, 20);

								mSubtitleView.setLayoutParams(params);
							}
							//mControllerHandler.sendEmptyMessageDelayed(0, 10000);
							//displaySystemMenu(false);

						}
						else
						{
							mTouchPressed = false;
							mControllerHandler.removeMessages(0);
							this.mTitleBar.setVisibility(View.GONE);
							this.mControlsView.setVisibility(View.GONE);
							this.mPPLButton.setVisibility(View.GONE);
							if(mUseSubtitle) {
								RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
								params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
								params.addRule(RelativeLayout.CENTER_HORIZONTAL);
								params.setMargins(20, 20, 20, 20);

								mSubtitleView.setLayoutParams(params);
								//displaySystemMenu(false);
							}
						}
					}
					else
					{
						mPPLLayout.setVisibility(View.GONE);
						if(mPlay) {
							mMpegPlayer.resume();
							mTouchPressed = false;
							getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
							this.mTitleBar.setVisibility(View.GONE);
							this.mControlsView.setVisibility(View.GONE);
							this.mPPLButton.setVisibility(View.GONE);
						}

						//mPPLLayout.setVisibility(View.GONE);
						mSeekBar.setEnabled(true);
						onPPL = false;
					}
				}
				// hold 상태일 때,
				else
				{
					mUnHoldButtonView.setVisibility(View.VISIBLE);
				}

				if(mControlsView.getVisibility() == View.VISIBLE){
					mControllerHandler.sendEmptyMessageDelayed(0, 4000);
				}

				return true;
			}

			return true;
		}else if(holdCheck == false){
			holdVideo();
			return true;
		}
		return true;
	}

	@Override
	public void onClick(View v)
	{
		mControllerHandler.removeMessages(0);
		if(onPPL == false) {
			switch (v.getId())
			{
			case R.id.hold_video:
				if(mPlay)
				{
					holdVideo();	
				}
				break;
			case R.id.unhold_button:
				unholdVideo();
				break;
			case R.id.play_pause:
				resumePause();
				break;
			case R.id.next_video:
				nextVideo();
				break;
			case R.id.prev_video:
				prevVideo();
				break;
				//case R.id.ratio_video:
				//        changeRatio();
				//        break;
			case R.id.ppl_button:
				if(mPlay) 
					mMpegPlayer.pause();
				mPPLViewPager.setAdapter(new PPLPagerViewAdapter(this));

				//					imageButton.setOnClickListener(new OnClickListener() {
				//						
				//						@Override
				//						public void onClick(View v) {
				//							
				//							mPPLDataLayout.setVisibility(View.VISIBLE);
				//							
				//							PPLData ppl1 = JSONParserHelper.pplData.get(v.getId());
				//							URL url;
				//							Bitmap bmp;
				//							try {
				//								url = new URL(ppl1.product_image);
				//								bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
				//								
				//								int layoutSize = mPPLLayout.getHeight()-60;
				//								int bitmapHeight = bmp.getHeight();
				//								int bitmapWidth = bmp.getWidth();
				//								int bitmapReSize = (int)((float)bitmapWidth*((float)layoutSize/bitmapHeight));
				//								bmp = Bitmap.createScaledBitmap(bmp, layoutSize, layoutSize, true);
				//								mPPLDataImage.setImageBitmap(bmp);
				//								mPPLDataImage.setVisibility(View.VISIBLE);
				//							} catch (IOException e) {
				//								// TODO Auto-generated catch block
				//								e.printStackTrace();
				//							}
				//							
				//							mPPLDataText.setText(""+ppl1.product_code);
				//							mPPLDataBrand.setText(ppl1.brand_name);
				//							mPPLDataMall.setText(ppl1.store_link);
				//							mPPLDataName.setText(ppl1.product_name);
				//							mPPLDataPrice.setText(""+ppl1.price);
				//							mPPLDataSite.setText(ppl1.drama_code);
				//							//Toast.makeText(getApplicationContext(), ""+ppl1.product_name, Toast.LENGTH_SHORT).show();
				//							
				//						}
				//					});
				//				}


				this.mTitleBar.setVisibility(View.GONE);
				this.mControlsView.setVisibility(View.GONE);
				this.mPPLButton.setVisibility(View.GONE);
				mPPLLayout.setVisibility(View.VISIBLE);
				onPPL = true;
				mSeekBar.setEnabled(false);
				break;
			default:
				throw new RuntimeException();
			}
			
			
			mControllerHandler = new Handler(){
				@Override
				public void handleMessage(Message msg) {
					mTitleBar.setVisibility(View.GONE);
					mControlsView.setVisibility(View.GONE);
					mPPLButton.setVisibility(View.GONE);
					if(mUseSubtitle) {
						RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
						params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
						params.addRule(RelativeLayout.CENTER_HORIZONTAL);
						params.setMargins(20, 20, 20, 20);

						mSubtitleView.setLayoutParams(params);
					}
					mTouchPressed = false;
				}
			};
			this.mTitleBar.setVisibility(View.VISIBLE);
			this.mControlsView.setVisibility(View.VISIBLE);
			this.mPPLButton.setVisibility(View.VISIBLE);
			if(mUseSubtitle) {
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ABOVE, mControlsView.getId());
				params.addRule(RelativeLayout.CENTER_HORIZONTAL);
				params.setMargins(20, 20, 20, 20);

				mSubtitleView.setLayoutParams(params);
			}
			
			mControllerHandler.sendEmptyMessageDelayed(0, 4000);
			
			
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
	{
		String value = String.valueOf(seekBar.getProgress());
		if (fromUser)
		{
			long timeUs = Long.parseLong(value) * 1000 * 1000;
			int currentTimeS = (int)(timeUs / 1000 / 1000);
			mCurrentTime.setText(parseTime(currentTimeS));
			/////////////////////////////////////////////////////////////////////////////////////////////////SeekBar

		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar)
	{

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar)
	{
		String value = String.valueOf(seekBar.getProgress());
		Log.e("seekbar value : ", value);
		//if (fromUser)
		//{
		//System.out.println(seekBar.getProgress());
		//long timeUs = Long.parseLong(value) * 1000 * 1000;
		//System.out.println("timeUs = " + timeUs);
		mMpegPlayer.seek(value);
		//}
	}

	@Override
	public void onFFDataSourceLoaded(FFmpegError err, FFmpegStreamInfo[] streams)
	{
		if (err != null)
		{
			String format = getResources().getString(
					R.string.main_could_not_open_stream);
			String message = String.format(format, err.getMessage());

			Builder builder = new AlertDialog.Builder(VideoActivity.this);
			builder.setTitle(R.string.app_name)
			.setMessage(message)
			.setOnCancelListener(
					new DialogInterface.OnCancelListener() {

						@Override
						public void onCancel(DialogInterface dialog) {
							VideoActivity.this.finish();
						}
					}).show();
			return;
		}
		mPlayPauseButton.setEnabled(true);
	}

	@Override
	public void onFFUpdateTime(long currentTimeUs, long videoDurationUs, boolean isFinished)
	{        
		currentTimeS = (int)(currentTimeUs / 1000 / 1000);
		int videoDurationS = (int)(videoDurationUs / 1000 / 1000);

		currentTime = currentTimeUs / 1000;

		if(currentTimeS >= 0){
			mSeekBar.setMax(videoDurationS);
			mSeekBar.setProgress(currentTimeS);

			mCurrentTime.setText(parseTime(currentTimeS));
			mTotalTime.setText(parseTime(videoDurationS));

		}

		//Log.e("Seek Motion", "currentTimeS                  "+currentTimeS);

		if (isFinished) {
			isFinish = true;
			nextVideo();
			isFinish = false;
		}
	}

	@Override
	public void onFFResume(NotPlayingException result)
	{
		this.mPlayPauseButton.setImageResource(R.drawable.pause);
		this.mPlayPauseButton.setEnabled(true);

		//displaySystemMenu(false);
	}

	@Override
	public void onFFPause(NotPlayingException err)
	{
		this.mPlayPauseButton.setImageResource(R.drawable.play);
		this.mPlayPauseButton.setEnabled(true);
	}

	@Override
	public void onFFStop()
	{
	}

	@Override
	public void onFFSeeked(NotPlayingException result)
	{
		//if (result != null)
		//        throw new RuntimeException(result);
	}

	public void resumePause()
	{
		this.mPlayPauseButton.setEnabled(false);

		if (mPlay)
		{
			mMpegPlayer.pause();
			//displaySystemMenu(false);
		}
		else
		{
			mMpegPlayer.resume();
			//displaySystemMenu(true);
		}

		mPlay = !mPlay;
	}

	private void stop()
	{
		this.mControlsView.setVisibility(View.GONE);
	}

	/**
	 * 작성자 : 이준영
	 * 메소드 이름 : parseTIme
	 * 매개변수 : 가공되지 않은 시간
	 * 반환값 : 가공된 시간
	 * 메소드 설명 : 시간을 매개변수로 받아 처리해서 반환해준다(TextView에 적절하게 뿌리기 위해)
	 */
	private String parseTime(int time)
	{
		String minS = null;
		String secS = null;
		int total = time;
		int spare;

		int hour = total / (60 * 60);
		spare = total % (60 * 60);
		int min = spare / (60);
		spare = spare % (60);
		int sec = spare;

		if (min < 10 && min > -10)
			minS = "0" + min;
		else
			minS = min + "";
		if (sec < 10 && sec > -10)
			secS = "0" + sec;
		else
			secS = sec + "";

		if(hour > 0){
			String result = hour + " : " + minS + " : " + secS;
			return result;
		}else{
			String result = minS + " : " + secS;
			return result;
		}


	}

	private int getDeviceWidth() {
		if (12 < Build.VERSION.SDK_INT) {
			Point p = new Point();
			getWindowManager().getDefaultDisplay().getSize(p);
			return p.x;
		} else {
			return getWindowManager().getDefaultDisplay().getWidth();
		}
	}

	private int getDeviceHeight() {
		if (12 < Build.VERSION.SDK_INT) {
			Point p = new Point();
			getWindowManager().getDefaultDisplay().getSize(p);
			return p.y;
		} else {
			return getWindowManager().getDefaultDisplay().getHeight();
		}
	}

	// 홀드 처리
	public void holdVideo() {
		mHold = true;
		holdCheck = false;

		mHoldHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				mUnHoldButtonView.setVisibility(View.GONE);
			}
		};
		mUnHoldButtonView.setVisibility(View.VISIBLE);
		mHoldHandler.sendEmptyMessageDelayed(0, 4000);

		this.mTitleBar.setVisibility(View.GONE);
		this.mControlsView.setVisibility(View.GONE);
		this.mPPLButton.setVisibility(View.GONE);
	}

	public void unholdVideo() {
		mHold = false;
		holdCheck = true;
		mUnHoldButtonView.setVisibility(View.GONE);

		this.mTitleBar.setVisibility(View.VISIBLE);
		this.mControlsView.setVisibility(View.VISIBLE);
		this.mPPLButton.setVisibility(View.VISIBLE);
		//displaySystemMenu(true);
	}

	public void nextVideo() {
		if(videoList != null && index < videoList.size() - 1) {
			saveVideoTime();
			file = new File(videoList.get(++index));
			path = file.getAbsolutePath();
			setDataSource();
			mMpegPlayer.resume();
		}
	}

	public void prevVideo() {
		if(index > 0) {
			saveVideoTime();
			file = new File(videoList.get(--index));
			path = file.getAbsolutePath();
			setDataSource();
			mMpegPlayer.resume();
		}
	}

	public void saveVideoTime() {
		int now = (int) (mMpegPlayer.getCurrentTime() / 1000 / 1000);
		if(isFinish)
			dbAdapter.saveVideoTime(path, 1, JSONHelper.dramaName);
		else
			dbAdapter.saveVideoTime(path, now, JSONHelper.dramaName);

	}


	private void doBrightnessTouch(float y_changed)
	{
		float delta = -y_changed / getDeviceHeight() * 0.07f;
		WindowManager.LayoutParams lp = getWindow().getAttributes();

		if(brightnessCheck == false){
			SharedPreferences pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
			lp.screenBrightness = pref.getFloat("brightness", 0.5f);
			lp.screenBrightness = (lp.screenBrightness-1)/14;
			getWindow().setAttributes(lp);
			brightnessCheck = true;
		}

		lp.screenBrightness = Math.min(Math.max(lp.screenBrightness + delta, 0.01f), 1);

		brightnessValue = (lp.screenBrightness*14)+1;
		this.mVolumeBrightnessValue.setText(""+(int)brightnessValue);
		this.mVolumeBrightnessImage.setBackgroundResource(R.drawable.bright);
		getWindow().setAttributes(lp);
	}

	private void doVolumeTouch(float y_changed)
	{
		int delta = -(int) ((y_changed / getDeviceHeight()) * mAudioMax);
		int vol = (int) Math.min(Math.max(mVolume + delta, 0), mAudioMax);

		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		AudioManager mAudioManager = 
				(AudioManager)getSystemService(AUDIO_SERVICE);
		if(holdCheck==true){
			switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP :
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
						AudioManager.ADJUST_RAISE, 
						AudioManager.FLAG_SHOW_UI);
				mControllerHandler = new Handler(){
					@Override
					public void handleMessage(Message msg) {
						mVolumeBrightnessVariationView.setVisibility(View.GONE);
					}
				};
				this.mVolumeBrightnessValue.setText(""+mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
				this.mVolumeBrightnessImage.setBackgroundResource(R.drawable.sound);
				this.mVolumeBrightnessVariationView.setVisibility(View.VISIBLE);
				mControllerHandler.sendEmptyMessageDelayed(0, 4000);
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, 
						AudioManager.ADJUST_LOWER, 
						AudioManager.FLAG_SHOW_UI);
				mControllerHandler = new Handler(){
					@Override
					public void handleMessage(Message msg) {
						mVolumeBrightnessVariationView.setVisibility(View.GONE);
					}
				};
				this.mVolumeBrightnessValue.setText(""+mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
				this.mVolumeBrightnessImage.setBackgroundResource(R.drawable.sound);
				this.mVolumeBrightnessVariationView.setVisibility(View.VISIBLE);
				mControllerHandler.sendEmptyMessageDelayed(0, 4000);
				return true;
			case KeyEvent.KEYCODE_BACK:
				if(mPPLLayout.getVisibility() == View.VISIBLE){
					mPPLLayout.setVisibility(View.GONE);
					if(mPlay) {
						mMpegPlayer.resume();
						mTouchPressed = false;
						getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
						this.mTitleBar.setVisibility(View.GONE);
						this.mControlsView.setVisibility(View.GONE);
						this.mPPLButton.setVisibility(View.GONE);
					}

					mSeekBar.setEnabled(true);
					onPPL = false;
					return true;
				}

				if (onPPL) {
					if(mPlay) {
						mMpegPlayer.resume();
						mTouchPressed = false;
						getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
						this.mTitleBar.setVisibility(View.GONE);
						this.mControlsView.setVisibility(View.GONE);
						this.mPPLButton.setVisibility(View.GONE);
					}
					
					mSeekBar.setEnabled(true);
					onPPL = false;
				} 
				else if (mHold);
				else {
					mUseSubtitle = false;
					saveVideoTime();
					SharedPreferences pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
					SharedPreferences.Editor editor = pref.edit();
					editor.putFloat("brightness", this.brightnessValue);
					editor.commit();
					finish();
				}

				return true;
			}

			return false;
		}else if(holdCheck==false){
			switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_UP :
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return true;
			case KeyEvent.KEYCODE_BACK:
				return true;
			}
			return false;
		}
		return false;
	}
	
	public void onUserLeaveHint(){
		if (onPPL) {
			if(mPlay) {
				mMpegPlayer.resume();
				mTouchPressed = false;
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				this.mTitleBar.setVisibility(View.GONE);
				this.mControlsView.setVisibility(View.GONE);
				this.mPPLButton.setVisibility(View.GONE);
			}

			mPPLLayout.setVisibility(View.GONE);
			mSeekBar.setEnabled(true);
			onPPL = false;
		} 
		else if (mHold);
		else {
			mUseSubtitle = false;
			saveVideoTime();
			SharedPreferences pref = getSharedPreferences("pref", Activity.MODE_PRIVATE);
			SharedPreferences.Editor editor = pref.edit();
			editor.putFloat("brightness", this.brightnessValue);
			editor.commit();
			finish();
		}
	}

	private void doSeekTouch(float coef, float xgesturesize, boolean seek)
	{

		if(coef > 0.5 || Math.abs(xgesturesize) < 1)
			return;

		long mCurrentTimeUs = mMpegPlayer.getCurrentTime();
		long mVideoDurationUs = mMpegPlayer.getVideoDuration();
		long value;

		int jump = (int) (Math.signum(xgesturesize) * ((600000 * Math.pow((xgesturesize), 4)) + 3000));

		if((jump > 0) && ((mCurrentTimeUs + jump) > mVideoDurationUs))
			jump = (int) (mVideoDurationUs - mCurrentTimeUs);
		if((jump < 0) && ((mCurrentTimeUs + jump) < 0))
			jump = (int) -mCurrentTimeUs;

		value = mCurrentTimeUs + jump;

		seekValue = (int)(value / 1000 / 1000);

		mSeek = true;
	}

	@Override
	public void onSystemUiVisibilityChange(int visibility) {
		if(visibility != View.SYSTEM_UI_FLAG_HIDE_NAVIGATION){
			Toast.makeText(getApplicationContext(), "a;slkdjfalksj", Toast.LENGTH_SHORT).show();
			this.mTitleBar.setVisibility(View.GONE);
			this.mControlsView.setVisibility(View.GONE);
			this.mPPLButton.setVisibility(View.GONE);

			if(mUseSubtitle){
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.ABOVE, mControlsView.getId());
				params.addRule(RelativeLayout.CENTER_HORIZONTAL);
				params.setMargins(20, 20, 20, 20);

				mSubtitleView.setLayoutParams(params);
			}
		}

	}
	/*mVideoView.setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {

	@Override
	public void onSystemUiVisibilityChange(int visibility) {
		if(visibility != View.SYSTEM_UI_FLAG_HIDE_NAVIGATION){
			Toast.makeText(getApplicationContext(), "a;slkdjfalksj", Toast.LENGTH_SHORT).show();



		}
	}
});*/

	/*	this.mTitleBar.setVisibility(View.VISIBLE);
		this.mControlsView.setVisibility(View.VISIBLE);
		this.mPPLButton.setVisibility(View.VISIBLE);
		if(mUseSubtitle) {
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.ABOVE, mControlsView.getId());
			params.addRule(RelativeLayout.CENTER_HORIZONTAL);
			params.setMargins(20, 20, 20, 20);

			mSubtitleView.setLayoutParams(params);
	 */
	private class PPLPagerViewAdapter extends PagerAdapter{

		private LayoutInflater mInflater;
		private ArrayList<PPLData> mPPLList;

		public PPLPagerViewAdapter(Context c){
			super();
			mInflater = LayoutInflater.from(c);
			mPPLList = new ArrayList<PPLData>();
			JSONParserHelper.pplData.clear();
			JSONParserHelper.parsingPPL(JSONHelper.dramaName, currentTimeS);
			if(JSONParserHelper.pplData.size()==0){
				PPLData ppl = new PPLData();
				Bitmap bitmap = null;
				try {
					bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.parser);
					bitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
					ppl.setProduct_image(bitmap);
				} catch(Exception e){

				}

				ppl.setPrice(0);
				ppl.setProduct_code(0);
				ppl.setDrama_code("");
				ppl.setBrand_name("현재 시청장면에");
				ppl.setProduct_name("등록된 상품이 없습니다.");

				JSONParserHelper.pplData.add(ppl);
			}
			//imageView.			
			for (int i = 0; i < JSONParserHelper.pplData.size(); i++) {
				try {
					PPLData ppl = JSONParserHelper.pplData.get(i);
					Log.e("Product", ppl.getProduct_name());
					mPPLList.add(ppl);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			initPageMark();
		}

		@Override
		public int getCount() {
			return mPPLList.size();
		}

		@Override
		public Object instantiateItem(View pager, int position) {
			View v = mInflater.inflate(R.layout.vp_ppl, null);
			ImageView iv_ppl = (ImageView)v.findViewById(R.id.iv_ppl_image);
			iv_ppl.setImageBitmap(mPPLList.get(position).getProduct_image());
			TextView tv_ppl_title = (TextView)v.findViewById(R.id.tv_ppl_title);
			tv_ppl_title.setText(mPPLList.get(position).getProduct_name());
			TextView tv_ppl_brand = (TextView)v.findViewById(R.id.tv_ppl_brand);
			tv_ppl_brand.setText(mPPLList.get(position).getBrand_name());
			TextView tv_ppl_price = (TextView)v.findViewById(R.id.tv_ppl_price);
			tv_ppl_price.setText(mPPLList.get(position).getPrice() + "원");
			if(mPPLList.get(position).getPrice()==0){
				tv_ppl_price.setText("");
			}

			((ViewPager)pager).addView(v, 0);

			return v; 
		}

		@Override
		public void destroyItem(View pager, int position, Object view) {    
			((ViewPager)pager).removeView((View)view);
		}

		@Override
		public boolean isViewFromObject(View pager, Object obj) {
			return pager == obj; 
		}


		private void initPageMark(){
			mPageMark.removeAllViews();
			for(int i=0; i<getCount(); i++)
			{
				ImageView iv = new ImageView(getApplicationContext());	//페이지 표시 이미지 뷰 생성
				iv.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

				//첫 페이지 표시 이미지 이면 선택된 이미지로
				if(i==0)
					iv.setBackgroundResource(R.drawable.page_select);
				else	//나머지는 선택안된 이미지로
					iv.setBackgroundResource(R.drawable.page_not);

				//LinearLayout에 추가
				mPageMark.addView(iv);
			}
			mPrevPosition = 0;	//이전 포지션 값 초기화
		}

		@Override public void restoreState(Parcelable arg0, ClassLoader arg1) {}
		@Override public Parcelable saveState() { return null; }
		@Override public void startUpdate(View arg0) {}
		@Override public void finishUpdate(View arg0) {}
	}
}