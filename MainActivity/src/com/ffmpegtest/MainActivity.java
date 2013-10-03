package com.ffmpegtest;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.ffmpegtest.adapter.VideoListAdapter;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends Activity implements OnItemClickListener {

	private ActionBar mActionBar;
	private HashMap<String, ArrayList<String>> video;
	private ArrayList<String> path;
	private ArrayList<String> name;
	private ListView listView;
	private String currentPath;
	private String root;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		listView = (ListView)findViewById(R.id.list);
		listView.setOnItemClickListener(this);

		currentPath = Environment.getExternalStorageDirectory().getPath();
		root = Environment.getExternalStorageDirectory().getPath();
		path = new ArrayList<String>();
		name = new ArrayList<String>();
		video = new HashMap<String, ArrayList<String>>();

		getVideoFileList();
	}

	/** 
	* 작성자 : 임창민
	* 메소드 이름 : getVideoFileList()
	* 매개변수 : 없음
	* 반환값 : 없음
	* 메소드 설명 : HashMap에 비디오 파일Path와 리스트를 초기화한다.
	*/ 
	public void getVideoFileList() {

		String[] videoProjection = { MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.SIZE };
		Cursor videoCursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videoProjection, null, null, null);

		videoCursor.moveToFirst();

		if(videoCursor != null) {
			do {
				int videoPath = videoCursor.getColumnIndex(MediaStore.Video.Media.DATA);

				String[] videoFiles = videoCursor.getString(videoPath).split("/");
				String videoFilePath = "";
				for(int i=0; i<videoFiles.length - 1; i++) {
					videoFilePath += videoFiles[i] + "/";
				}
				
				File f = new File(videoFilePath);
				if(f.canRead()) {
					if(!video.containsKey(f.getAbsolutePath())) {
						video.put(f.getAbsolutePath(), new ArrayList<String>());
						Log.e("newKey", f.getAbsolutePath());
					}
					
					String videoName = videoFiles[(videoFiles.length - 1)];
					video.get(f.getAbsolutePath()).add(videoName);
					Log.e("addValue : " + f.getAbsolutePath(), videoName);
				}

			} while(videoCursor.moveToNext());
		}

		path = new ArrayList<String>(video.keySet());
		for(int i=0; i<path.size(); i++) {
			String[] pathList = path.get(i).split("/");
			name.add(pathList[pathList.length - 1]);
		}
		listView.setAdapter(new VideoListAdapter(this, video, currentPath)); 
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);

		mActionBar = getActionBar();
		mActionBar.setDisplayShowHomeEnabled(false);
		mActionBar.setTitle("폴더");

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case R.id.refresh:
			break;
		case R.id.search:
			break;
		}

		return true;
	}
	
	@Override
	public void onItemClick(AdapterView<?> listView, View view, int position, long id)
	{
		File f = null;
		if(currentPath.equals(root)) {
			f = new File(path.get(position));
			currentPath = f.getAbsolutePath();
		}
		
		else {
			Log.e("currentPath : ", currentPath);
			Log.e("name : ", video.get(currentPath).get(position));
			f = new File(currentPath + '/' + video.get(currentPath).get(position));
		}

		if(f.isDirectory()) {
			this.listView.setAdapter(new VideoListAdapter(this, video, currentPath));
			mActionBar.setTitle(name.get(position));
		} else {
			Intent i = new Intent(AppConstants.VIDEO_PLAY_ACTION);
			i.putStringArrayListExtra(AppConstants.VIDEO_PLAY_ACTION_LIST, video.get(currentPath));
			i.putExtra(AppConstants.VIDEO_PLAY_ACTION_PATH, currentPath);
			i.putExtra(AppConstants.VIDEO_PLAY_ACTION_INDEX, position);
			
			startActivity(i);
		}
	}
	
	/** 
	* 작성자 : 임창민
	* 메소드 이름 : onBackPressed()
	* 매개변수 : 없음
	* 반환값 : 없음
	* 메소드 설명 : Android Back Key 리스너로 폴더에 있을때에는 루트경로로 오고 루트경로일때에는 앱을 종료시킨다.
	*/ 
	@Override
	public void onBackPressed() {
		if(!currentPath.equals(root)) {
			currentPath = root;
			listView.setAdapter(new VideoListAdapter(this, video, currentPath));
			mActionBar.setTitle("폴더");
		} else {
			finish();
		}
	}
}