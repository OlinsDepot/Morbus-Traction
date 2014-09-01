package com.olinsdepot.od_traction;

import android.app.Activity;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;
import android.support.v4.widget.DrawerLayout;


/**
 * Main Activity Olins Depot Throttle Application
 * @author mhughes
 *
 */
public class MainActivity extends Activity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks,
		NetFragment.OnServerChangeListener,
		RosterFragment.OnRosterChangeListener,
        ThrottleFragment.OnThrottleChangeListener {
	
	/**
	 * Logging - String for the class name, Logging on/off flag.
	 */
	private final String TAG = getClass().getSimpleName();
	private static final boolean L = true;
	
    /**
     * Nav Drawer - Fragment managing the behaviors, interactions and presentation.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private CharSequence mTitle;
    
    /**
     * MorBus Service
     */
    private Messenger mService = null;
    private boolean mBound = false;


	//
	// MAIN Life Cycle Call Backs
	//
	
	/**
	 * OnCreate method
	 */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (L) Log.i(TAG, "onCreate" + (null == savedInstanceState ? " Restored state" : " No saved state"));

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
        
		//Check if network is up -
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo == null) {
			Toast.makeText(getApplicationContext(), "No Network Connection", Toast.LENGTH_SHORT).show();
		}
    }

	@Override
	public void onRestart() {
		super.onRestart();
		if (L) Log.i(TAG, "onRestart");
	}

	@Override
	public void onStart() {
		super.onStart();
		if (L) Log.i(TAG, "onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
		if (L) Log.i(TAG, "onResume");
//		new CreateCommThreadTask().execute();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (L) Log.i(TAG, "onPause");
//		new CloseSocketTask().execute();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (L) Log.i(TAG, "onStop");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (L) Log.i(TAG, "onDestroy");
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (L) Log.i(TAG, "onSaveInstanceState");
	}

	@Override
	public void onRestoreInstanceState(Bundle savedState) {
		super.onRestoreInstanceState(savedState);
		if (L) Log.i(TAG, "onRestoreInstanceState");
	}


	
	//
	// Page navigation Interface
	//
	
	/**
	 * Nav - Notification that an item has been selected from the nav drawer.
	 */
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentTransaction SetMainView = getFragmentManager().beginTransaction();
    	switch (position) {
	    	case 0:
	            SetMainView.replace(R.id.main_container, NetFragment.newInstance())
	            .commit();
	    		break;
	    	case 1:
	            SetMainView.replace(R.id.main_container, RosterFragment.newInstance())
	            .commit();
	    		break;
	    	case 2:
	            SetMainView.replace(R.id.main_container, PlaceholderFragment.newInstance(3))
	            .commit();
	    		break;
	    	case 3:
	            SetMainView.replace(R.id.main_container, CabFragment.newInstance())
	            .commit();
	    		break;
    	}
    }

    /**
     * Nav - Set page title. Called when new page is attached.
     * @param number
     */
    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
            case 4:
                mTitle = getString(R.string.title_section4);
                break;
        }
    }


    //
    // Action bar interface.
    //

    /**
     * onCreateOptionsMenu notification
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (L) Log.i(TAG, "onCreateOptionsMenu");

        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Restore ActionBar after page change with updated title
     */
    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    /**
     * on OptionsItemSelected notification
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    
    //
    // Application page interfaces
    //
    
    /**
     * Server page listener.
     */
    @Override
    public void onServerChange(String srvrAddr, int srvrPort) {
    	Log.d(TAG,"onServerChange");
    	
    	// Start up MorBus service on server with this IP
    	// If the IP address is null, shutdown the service.
    	Intent mbusIntent = new Intent(this, MbusService.class);
		 
		if (!mBound) {
			bindService(mbusIntent, mConnection, Context.BIND_AUTO_CREATE);
		 }
		 else {
	            unbindService(mConnection);
	            mBound = false;
		 }

    }

	/**
	 * Roster change listener
	 */
	public void onRosterChange(int tID, int dcdrAdr) {
        Toast.makeText(getApplicationContext(), "ID="+tID+" Decoder="+dcdrAdr, Toast.LENGTH_SHORT).show();

        // If no Morbus service connected, do nothing.
        if (!mBound) return;
        
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, MbusService.REG_DECODER, tID, dcdrAdr);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
		
	}

    /**
	 * Throttle change listener
	 */
	@Override
	public void onThrottleChange(int tID, int speed) {
        Toast.makeText(getApplicationContext(), "ID="+tID+" Speed="+speed, Toast.LENGTH_SHORT).show();

        // If no Morbus service connected, do nothing.
        if (!mBound) return;
        
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, MbusService.THROTTLE_CHANGE, tID, speed);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

	}
    
    //
    // MorBus service interface.
    //
    
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
    	
    	public void onServiceConnected(ComponentName className, IBinder service) {
    		Log.d(TAG, "ServiceConnected - " + className);
    		mService = new Messenger(service);
    		mBound = true;
    	}
    	
    	public void onServiceDisconnected(ComponentName className) {
    		Log.d(TAG, "onServiceDisconnected - " + className);
    		mService = null;
    		mBound = false;
    	}
    };
    

}
