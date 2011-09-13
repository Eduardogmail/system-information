package simple.home.jtbuaa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class simpleHome extends Activity {

	WebView serverWeb;
	myListView favoAppList;
	ListView sysAppList, userAppList;
	AlertDialog m_altDialog;
	String version, myPackageName;
	FrameLayout mainlayout;
	ArrayList mFavoApps, mSysApps, mUserApps;
	private Button btnFavo, btnSys, btnUser, btnWeb;
	int currentTab;
	static int grayColor = 0xFFEEEEEE;
	static int whiteColor = 0xFFFFFFFF;
	Context mContact;
	PackageManager pm;
	favoAppAdapter favoAdapter;
	ApplicationsAdapter sysAdapter, userAdapter;
	ResolveInfo ri;
	ProgressDialog mProgressDialog;
	private static final int MAX_PROGRESS = 100;

	@Override
	protected Dialog onCreateDialog(int id) {
        switch (id) {
        case 0:
        {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMax(MAX_PROGRESS);
            return mProgressDialog;
    	}
        case 1: {
        	return new AlertDialog.Builder(this).
        	setMessage(getString(R.string.app_name) + " " + version + "\n\n" 
        			+ getString(R.string.about_dialog_notes) + "\n" + getString(R.string.about_dialog_text2)). 
        	setPositiveButton("Ok",
	          new DialogInterface.OnClickListener() {
	        	  public void onClick(DialogInterface dialog, int which) {}
	          }).create();
        }
        }
        return null;
	}

	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	menu.add(0, 0, 0, getString(R.string.changeback)).setVisible(false);
    	menu.add(0, 1, 0, getString(R.string.help));
    	menu.add(0, 2, 0, getString(R.string.about));
    	return true;
    }
	
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()) {
		case 0:
			break;
		case 1:
			break;
		case 2:
			showDialog(1);
			break;
		}
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		ri = (ResolveInfo) v.getTag();
		if (currentTab == 0)
			menu.add(0, 0, 0, getString(R.string.removeFromFavo));
		else {
			menu.add(0, 0, 0, getString(R.string.addtoFavo));
			menu.add(0, 1, 0, getString(R.string.backup)).setEnabled(false);
		}
	}
	
	public boolean onContextItemSelected(MenuItem item){
		super.onContextItemSelected(item);
		switch (item.getItemId()) {
		case 0:
			if (currentTab ==0) {
				favoAdapter.remove(ri);
			}
			else {
				if (favoAdapter.getPosition(ri) < 0) {
					favoAdapter.add(ri);
					favoAdapter.sort(new ResolveInfo.DisplayNameComparator(pm));
					}
			}
			try {
				FileOutputStream fo = this.openFileOutput("favo", 0);
				ObjectOutputStream oos = new ObjectOutputStream(fo);
				for (int i = 0; i < favoAdapter.getCount(); i++) {
					oos.writeObject(((ResolveInfo)favoAdapter.localApplist.get(i)).activityInfo.name);
				}
				oos.flush();
				oos.close();
				fo.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
			}
			break;
		case 1://not implement now
			break;
		}
		return false;
	}
	
	/*@Override
	public void onConfigurationChanged(Configuration newConfig) {
	  super.onConfigurationChanged(newConfig); //not restart activity each time screen orientation changes
	}*/

	class myListView extends ListView {

		public myListView(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		protected final void onSizeChanged(final int w, final int h,  
	            final int oldw, final int oldh) {
			setBackground();
	    }  
		
		public void setBackground() {
			int w = getWidth();
			int h = getHeight();
			if ((w <= 0) || (h <= 0)) return;
	        Log.d("================", "w: " + w + " h: " + h);
			
			int x = 0, y = 0;
	        Bitmap oldbmp = ((BitmapDrawable) getWallpaper()).getBitmap();
	        Log.d("================", "dw: " + oldbmp.getWidth() + " dh: " + oldbmp.getHeight());
	        Matrix matrix = new Matrix();   
	        float scalew = ((float)w) / oldbmp.getWidth();
	        float scaleh = ((float)h) / oldbmp.getHeight();
	        Log.d("================", "scalew: " + scalew + " scaleh: " + scaleh);
	        if (scalew > scaleh) {
	        	scalew = scaleh;
	        	if (scalew < 1) y = (oldbmp.getHeight() - h) / 2;
	        }
	        else if ((scalew < scaleh) && (scalew < 1)) x = (oldbmp.getWidth() - w) / 2;//centerize the pic.
	        matrix.postScale(scalew, scalew);
	        //Bitmap newbmp = Bitmap.createBitmap(oldbmp, x, y, w, h, matrix, true);//wrong for some pic?
	        Bitmap newbmp = Bitmap.createBitmap(oldbmp, 0, 0, w, h, matrix, true);
	        BitmapDrawable bd = new BitmapDrawable(newbmp);
	        
			setBackgroundDrawable(bd);
		}
	};
	
	BroadcastReceiver wallpaperReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			favoAppList.setBackground();
		}
	};
	
	OnBtnClickListener mBtnCL = new OnBtnClickListener();
	class OnBtnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			int newTab = 0;

			String text = (String) ((Button) v).getText();
			if (text.equals(getString(R.string.favoriteapps))) newTab = 0;
			else if (text.equals(getString(R.string.systemapps))) newTab = 1;
			else if (text.equals(getString(R.string.userapps))) newTab = 2;
			else if (text.equals(getString(R.string.online))) newTab = 3;
			
			if (currentTab != newTab) {
				switch(currentTab) {
				case 0:
					btnFavo.setBackgroundResource(R.drawable.button_layout_unselected);
					favoAppList.setVisibility(View.INVISIBLE);
					break;
				case 1:
					btnSys.setBackgroundResource(R.drawable.button_layout_unselected);
					sysAppList.setVisibility(View.INVISIBLE);
					break;
				case 2:
					btnUser.setBackgroundResource(R.drawable.button_layout_unselected);
					userAppList.setVisibility(View.INVISIBLE);
					break;
				case 3:
					btnWeb.setBackgroundResource(R.drawable.button_layout_unselected);
					serverWeb.setVisibility(View.INVISIBLE);
					break;
				}
				
				switch(newTab) {
				case 0:
					btnFavo.setBackgroundResource(R.drawable.button_layout_selected);
					favoAppList.setVisibility(View.VISIBLE);
					break;
				case 1:
					btnSys.setBackgroundResource(R.drawable.button_layout_selected);
					sysAppList.setVisibility(View.VISIBLE);
					break;
				case 2:
					btnUser.setBackgroundResource(R.drawable.button_layout_selected);
					userAppList.setVisibility(View.VISIBLE);
					break;
				case 3:
					btnWeb.setBackgroundResource(R.drawable.button_layout_selected);
					serverWeb.setVisibility(View.VISIBLE);
					break;
				}
				
				currentTab = newTab;
			}
		}
	}
	
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mContact = this.getBaseContext();
        
        myPackageName = this.getApplicationInfo().packageName;

    	pm = getPackageManager();
    	
    	//requestWindowFeature(Window.FEATURE_NO_TITLE); // hide titlebar of application, must be before setting the layout
    	getWindow().requestFeature(Window.FEATURE_PROGRESS);
    	setContentView(R.layout.ads);
    	
        mainlayout = (FrameLayout)findViewById(R.id.mainFrame);
        
    	Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
    	mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
    	List<ResolveInfo> mAllApps = pm.queryIntentActivities(mainIntent, 0);
    	//mainIntent.removeCategory(Intent.CATEGORY_LAUNCHER);
    	//mainIntent.addCategory(Intent.CATEGORY_HOME);
    	//mAllApps.addAll(pm.queryIntentActivities(mainIntent, 0));//may add some strange activity.
    	Collections.sort(mAllApps, new ResolveInfo.DisplayNameComparator(pm));//sort by name

    	mSysApps = new ArrayList();
    	mUserApps = new ArrayList();
    	for (int i = 0; i < mAllApps.size(); i++) {
    		ResolveInfo ri = mAllApps.get(i);
    		if ((ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) 
    			mSysApps.add(ri);
    		else mUserApps.add(ri);
    		
    		//if (ri.filter.hasAction(Intent.ACTION_DIAL)) //phone, message, contact, ... should add to favorite
    			//mFavoApps.add(ri);
    	}
    	
    	/*ArrayList packages = (ArrayList) pm.getInstalledPackages(0);
    	for (int i = 0; i < packages.size(); i++) {
    		PackageInfo pi = (PackageInfo) packages.get(i);
    		Intent intent = pm.getLaunchIntentForPackage(pi.packageName);
    		if (intent == null) {//no Launcher activity
    		}
    	}*/

		FileInputStream fi;
		mFavoApps = new ArrayList();
		try {//read favorite data
			fi = this.openFileInput("favo");
			ObjectInputStream ois = new ObjectInputStream(fi);
			String activityName;
			while ((activityName = (String) ois.readObject()) != null) {
				for (int i = 0; i < mAllApps.size(); i++)
					if (mAllApps.get(i).activityInfo.name.equals(activityName)) {
						mFavoApps.add(mAllApps.get(i));
						break;
					}
			}
			ois.close();
			fi.close();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
		}

    	//favorite app tab
    	favoAppList = new myListView(this);
    	favoAppList.setDividerHeight(0);
    	favoAppList.setVerticalScrollBarEnabled(false);
    	favoAppList.inflate(this, R.layout.app_list, null);
    	favoAppList.setFadingEdgeLength(0);//no shadow when scroll
    	favoAppList.setScrollingCacheEnabled(false);
    	favoAdapter = new favoAppAdapter(this, mFavoApps);
    	favoAppList.setAdapter(favoAdapter);
        
    	//system app tab
    	sysAppList = new ListView(this);
    	sysAppList.inflate(this, R.layout.app_list, null);
    	sysAppList.setFadingEdgeLength(0);//no shadow when scroll
    	sysAppList.setScrollingCacheEnabled(false);
    	sysAdapter = new ApplicationsAdapter(this, mSysApps);
    	sysAppList.setAdapter(sysAdapter);
        
    	//user app tab
        userAppList = new ListView(this);
        userAppList.inflate(this, R.layout.app_list, null);
        userAppList.setFadingEdgeLength(0);//no shadow when scroll
        userAppList.setScrollingCacheEnabled(false);
        userAdapter = new ApplicationsAdapter(this, mUserApps);
        userAppList.setAdapter(userAdapter);
        
        //online tab
        final Activity activity = this;
        serverWeb = new WebView(this);
        WebSettings webSettings = serverWeb.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSaveFormData(true);
        webSettings.setTextSize(WebSettings.TextSize.SMALLER);
        serverWeb.setScrollBarStyle(0);
        serverWeb.setWebChromeClient(new WebChromeClient() {
		});
		serverWeb.setWebViewClient(new WebViewClient() {

			@Override
			public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error){
		        handler.proceed();//接受证书
			}
			
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				showDialog(0);
				super.onPageStarted(view, url, favicon);
			}
			 
			@Override
			public void onPageFinished(WebView view, String url) {
				if(mProgressDialog.isShowing()){
					mProgressDialog.dismiss();
	            }
			}         
			
			public void onProgressChanged(WebView view, int progress) {
				mProgressDialog.setProgress(progress);
				if (progress >= MAX_PROGRESS) {
					mProgressDialog.dismiss();
				}
			}
			
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if (url.substring(url.length()-4).equals(".apk")){
					String ss[] = url.split("/");
					String apkName = ss[ss.length-1]; //得到音乐文件的全名(包括后缀)
					Intent intent = new Intent(getBaseContext(), DownloadService.class);
					intent.putExtra("url", url);
					intent.putExtra("apk", apkName);
					getBaseContext().startService(intent);
					return true;
				}
				return false;
			}
		});
		
        try {
        	PackageInfo pi = pm.getPackageInfo(myPackageName, 0);
        	version = "v" + pi.versionName;
    	} catch (NameNotFoundException e) {
    		e.printStackTrace();
    	}    

        currentTab = 0;
    	sysAppList.setVisibility(View.INVISIBLE);
        userAppList.setVisibility(View.INVISIBLE);
		serverWeb.setVisibility(View.INVISIBLE);
		mainlayout.addView(serverWeb);
        mainlayout.addView(userAppList);
        mainlayout.addView(sysAppList);
        mainlayout.addView(favoAppList);
        
        btnFavo = (Button) findViewById(R.id.btnFavoriteApp);
        btnFavo.setOnClickListener(mBtnCL);
        
        btnSys = (Button) findViewById(R.id.btnSystemApp);
        btnSys.setOnClickListener(mBtnCL);
        
        btnUser = (Button) findViewById(R.id.btnUserApp);
        btnUser.setOnClickListener(mBtnCL);
        
        btnWeb = (Button) findViewById(R.id.btnOnline);
		btnWeb.setOnClickListener(mBtnCL);

		//for package add/remove
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addDataScheme("package");
		registerReceiver(packageReceiver, filter);
		
		//for wall paper changed
		filter = new IntentFilter();
		filter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
		registerReceiver(wallpaperReceiver, filter);
		
		//for download apk completed
		filter = new IntentFilter();
		filter.addAction("simple.home.downloadcompleted");
		registerReceiver(downloadedReceiver, filter);

		PageTask task = new PageTask();
        task.execute("");
    }
    
	BroadcastReceiver downloadedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// TODO Auto-generated method stub
			String apkname = arg1.getExtras().getString("apk");
			Log.d("=====================apk: ", apkname);
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(new File(apkname)), "application/vnd.android.package-archive"); 
			startActivity(intent); 
		}
	
	};
	
	BroadcastReceiver packageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
            String action = intent.getAction();
            String packageName = intent.getDataString().split(":")[1];
            if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
            	if ((intent.getFlags() & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
            		for (int i = 0; i < mSysApps.size(); i++) {
            			ResolveInfo info = (ResolveInfo) mSysApps.get(i);
            			if (info.activityInfo.packageName.equals(packageName)) {
            				sysAdapter.remove(info);
            				break;
            			}
            		}
            	}
            	else {
            		for (int i = 0; i < mUserApps.size(); i++) {
            			ResolveInfo info = (ResolveInfo) mUserApps.get(i);
            			if (info.activityInfo.packageName.equals(packageName)) {
            				userAdapter.remove(info);
            				break;
            			}
            		}
            	}
        		for (int i = 0; i < mFavoApps.size(); i++) {
        			ResolveInfo info = (ResolveInfo) mFavoApps.get(i);
        			if (info.activityInfo.packageName.equals(packageName)) {
        				favoAdapter.remove(info);
        				break;
        			}
        		}
            }
            else if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
            	Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            	mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            	mainIntent.setPackage(packageName);
            	List<ResolveInfo> targetApps = pm.queryIntentActivities(mainIntent, 0);

            	if ((intent.getFlags() & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
    				sysAdapter.add(targetApps.get(0));
    		    	Collections.sort(sysAdapter.localApplist, new ResolveInfo.DisplayNameComparator(pm));//sort by name
            	}
            	else {
    				userAdapter.add(targetApps.get(0));
    		    	Collections.sort(userAdapter.localApplist, new ResolveInfo.DisplayNameComparator(pm));//sort by name
            	}
            }
		}
		
	};

    private class ApplicationsAdapter extends ArrayAdapter<ResolveInfo> {
    	ArrayList localApplist;
        public ApplicationsAdapter(Context context, List<ResolveInfo> apps) {
            super(context, 0, apps);
            localApplist = (ArrayList) apps;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ResolveInfo info = (ResolveInfo) localApplist.get(position);

            if (convertView == null) {
                final LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.app_list, parent, false);
            }

            if (position % 2 == 1)
            	convertView.setBackgroundColor(whiteColor);
            else
            	convertView.setBackgroundColor(grayColor);
        	//convertView.setBackgroundColor(0x00000000);
            
            final ImageButton btnIcon = (ImageButton) convertView.findViewById(R.id.appicon);
            final TextView textView1 = (TextView) convertView.findViewById(R.id.appname);
			final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            
            btnIcon.setImageDrawable(info.loadIcon(pm));
            btnIcon.setEnabled(false);
    		btnIcon.setOnClickListener(new OnClickListener() {//kill app
				@Override
				public void onClick(View arg0) {
					am.restartPackage(info.activityInfo.packageName);
		        	textView1.setTextColor(0xFF000000);//set color back to black after kill it. 
		        	btnIcon.setEnabled(false);
				}
    		});
    		

            LinearLayout lapp = (LinearLayout) convertView.findViewById(R.id.app);
            lapp.setOnClickListener(new OnClickListener() {//start app
				@Override
				public void onClick(View arg0) {
					if (info.activityInfo.applicationInfo.packageName.equals(myPackageName)) return;//not start system info again.
					
					Intent i = new Intent(Intent.ACTION_MAIN);
					i.setComponent(new ComponentName(
							info.activityInfo.applicationInfo.packageName,
							info.activityInfo.name));
					i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);//not start a new activity but bring it to front if it already launched.
					try {
						startActivity(i);
	            		btnIcon.setEnabled(true);
	                	textView1.setTextColor(0xFFFF7777);//red for running apk
					} catch(Exception e) {
						Toast.makeText(getBaseContext(), e.toString(), 3500).show();
					}
				}
            });
            lapp.setTag(info);
            registerForContextMenu(lapp);
            
            textView1.setText(info.loadLabel(pm));
            textView1.setTextColor(0xFF000000);
        	List appList = am.getRunningAppProcesses();
        	for (int i = 0; i < appList.size(); i++) {
        		RunningAppProcessInfo as = (RunningAppProcessInfo) appList.get(i);
            	if ((info.activityInfo.processName.equals(as.processName)) && (!as.processName.equals(myPackageName))) {
            		btnIcon.setEnabled(true);
                	textView1.setTextColor(0xFFFF7777);//red for running apk
        			break;
        		}
        	}
            
            final Button btnVersion = (Button) convertView.findViewById(R.id.appversion);
            btnVersion.setVisibility(View.VISIBLE);
            try {
            	btnVersion.setText(pm.getPackageInfo(info.activityInfo.packageName, 0).versionName);
			} catch (NameNotFoundException e) {
				btnVersion.setText("unknown");
			}
			btnVersion.setEnabled((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0);//disable for system app now.
			btnVersion.setOnClickListener(new OnClickListener() {//delete app
				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub
					Uri uri = Uri.fromParts("package", info.activityInfo.packageName, null);
					Intent intent = new Intent(Intent.ACTION_DELETE, uri);
					startActivity(intent);
				}
			});
            
            final TextView textView3 = (TextView) convertView.findViewById(R.id.appsource);
            String source = "";
            int textColor = 0xFF000000;
            if((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE) {
            	source = info.activityInfo.applicationInfo.sourceDir + " (debugable) " + info.activityInfo.packageName;
            	textColor = 0xFFEECC77;//brown for debuggable apk
            }
            else if((info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
            	source = info.activityInfo.applicationInfo.sourceDir;//we can use source dir to remove it.
            }
            else {
            	source = info.activityInfo.packageName;//we can use package name to uninstall it.
            }
        	textView3.setText(source);
        	textView3.setTextColor(textColor);//must set color here, otherwise it will be wrong for some item.
			
            return convertView;
        }
    }

    private class favoAppAdapter extends ArrayAdapter<ResolveInfo> {
    	ArrayList localApplist;
        public favoAppAdapter(Context context, List<ResolveInfo> apps) {
            super(context, 0, apps);
            localApplist = (ArrayList) apps;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ResolveInfo info = (ResolveInfo) localApplist.get(position);

            if (convertView == null) {
                final LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.favo_list, parent, false);
            }

            convertView.setBackgroundColor(0);
            
            final ImageButton btnIcon = (ImageButton) convertView.findViewById(R.id.favoappicon);
            final TextView textView1 = (TextView) convertView.findViewById(R.id.favoappname);
			final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            
            btnIcon.setImageDrawable(info.loadIcon(pm));
            btnIcon.setEnabled(false);
    		btnIcon.setOnClickListener(new OnClickListener() {//kill app
				@Override
				public void onClick(View arg0) {
					am.restartPackage(info.activityInfo.packageName);
		        	textView1.setTextColor(0xFF000000);//set color back to black after kill it. 
		        	btnIcon.setEnabled(false);
				}
    		});
    		

            textView1.setOnClickListener(new OnClickListener() {//start app
				@Override
				public void onClick(View arg0) {
					if (info.activityInfo.applicationInfo.packageName.equals(myPackageName)) return;//not start system info again.
					
					Intent i = new Intent(Intent.ACTION_MAIN);
					i.setComponent(new ComponentName(
							info.activityInfo.applicationInfo.packageName,
							info.activityInfo.name));
					i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);//not start a new activity but bring it to front if it already launched.
					try {
						startActivity(i);
	            		btnIcon.setEnabled(true);
	                	textView1.setTextColor(0xFFFF7777);//red for running apk
					} catch(Exception e) {
						Toast.makeText(getBaseContext(), e.toString(), 3500).show();
					}
				}
            });
            textView1.setTag(info);
            registerForContextMenu(textView1);
            
            textView1.setText(info.loadLabel(pm));
            textView1.setTextColor(0xFF000000);
        	List appList = am.getRunningAppProcesses();
        	for (int i = 0; i < appList.size(); i++) {
        		RunningAppProcessInfo as = (RunningAppProcessInfo) appList.get(i);
            	if ((info.activityInfo.processName.equals(as.processName)) && (!as.processName.equals(myPackageName))) {
            		btnIcon.setEnabled(true);
                	textView1.setTextColor(0xFFFF7777);//red for running apk
        			break;
        		}
        	}
            
            return convertView;
        }
    }
    
	class PageTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... params) {
			try {serverWeb.loadUrl("file:///android_asset/online.html");}
			catch (Exception e) {}
			return null;
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if (currentTab == 3)
				serverWeb.goBack();
			return true;
		}
		else return false;
	}
}
