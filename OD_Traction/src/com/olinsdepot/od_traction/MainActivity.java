package com.olinsdepot.od_traction;

import android.app.Activity;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

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
        ThrottleFragment.OnThrottleChangeListener {
	
	
    /**
     * Nav Drawer - Fragment managing the behaviors, interactions and presentation.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Nav Drawer - String stores the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

	/**
	 * Logging - String for the class name, Logging on/off flag.
	 */
	private final String TAG = getClass().getSimpleName();
	private static final boolean L = true;



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
	public void onRestart()
	{
		super.onRestart();
		if (L) Log.i(TAG, "onRestart");
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if (L) Log.i(TAG, "onStart");
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (L) Log.i(TAG, "onResume");
//		new CreateCommThreadTask().execute();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		if (L) Log.i(TAG, "onPause");
//		new CloseSocketTask().execute();
	}

	@Override
	public void onStop()
	{
		super.onStop();
		if (L) Log.i(TAG, "onStop");
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if (L) Log.i(TAG, "onDestroy");
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		if (L) Log.i(TAG, "onSaveInstanceState");
	}

	@Override
	public void onRestoreInstanceState(Bundle savedState)
	{
		super.onRestoreInstanceState(savedState);
		if (L) Log.i(TAG, "onRestoreInstanceState");
	}


	
	//
	// Navigation Interface
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
	            SetMainView.replace(R.id.main_container, PlaceholderFragment.newInstance(2))
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
    // Page Interface Methods
    //
    
    /**
     * Server page listener.
     */
    @Override
    public void onServerChange(String srvrAddr, int srvrPort) {
		 Intent mbus = new Intent(this, MbusService.class);
		 
		 if (srvrAddr != null) {
			 startService(mbus);
		 }
		 else {
			 stopService(mbus);
		 }

    }
    

	/**
	 * Throttle change listener
	 */
	@Override
	public void onThrottleChange(int tID, int speed) {
		// TODO Auto-generated method stub
        Toast.makeText(getApplicationContext(), "ID="+tID+" Speed="+speed, Toast.LENGTH_SHORT).show();

	}

}
