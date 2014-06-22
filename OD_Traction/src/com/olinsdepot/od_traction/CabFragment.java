package com.olinsdepot.od_traction;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;


/**
 * The "Cab" fragment containing throttle and I/O views.
 */
public class CabFragment extends Fragment {
    /* tbd:
    * t.b.d. Customize view for current device orientation
    * t.b.d. Cab will be associated with an engine or consist
    * t.b.d. After association, Cab will send message when inputs change
    */  

	/**
	 * String for logging the class name
	 */
	private final String TAG = getClass().getSimpleName();
	
	/**
	 * String for log on or off flag
	 */
	private static final boolean L = true;
	
	/**
	 * Fragment characteristics, an example.
	 */
	private static final String ARG_ORIENTATION = "ORIENTATION";

	/**
	 * Null constructor for this fragment
	 */
    public CabFragment() {
    }
    
    /**
     * Returns a new instance of the CAB view fragment
     */
    public static CabFragment newInstance() {
        CabFragment fragment = new CabFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ORIENTATION, 1);
        fragment.setArguments(args);
        return fragment;
    }

    //
    //Life cycle methods for the CAB fragment
    //
    
    /**
     * Notification that the fragment is associated with an Activity
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (L) Log.i(TAG, "onAttach " + activity.getClass().getSimpleName());
        
        /**
         * Give main the section number so it can update the Action Bar title.
         */
        ((MainActivity) activity).onSectionAttached(4);

    }

    /**
     * Create notification
     */
    @Override
    public void onCreate(Bundle saved) {
    	super.onCreate(saved);
    	if (L) Log.i(TAG, "onCreate");
    }
   

    /**
     * CreateView notification
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (L) Log.i(TAG, "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_cab, container, false);

        getFragmentManager().beginTransaction().replace(R.id.LEFT_THROTTLE_FRAME, ThrottleFragment.newInstance(0)).commit();
        getFragmentManager().beginTransaction().replace(R.id.RIGHT_THROTTLE_FRAME, ThrottleFragment.newInstance(1)).commit();
                
        return rootView;
    }
    
    /**
     * ActivityCreated notification
     */
    @Override
    public void onActivityCreated(Bundle saved) {
    	super.onActivityCreated(saved);
    	if (L) Log.i(TAG, "onActivityCreated");
    }
    
    /**
     * Start notification
     */
    @Override
    public void onStart() {
    	super.onStart();
    	if (L) Log.i(TAG, "onStart");
    }
    
    /**
     * Resume notification
     */
    @Override
    public void onResume() {
    	super.onResume();
    	if (L) Log.i(TAG, "onResume");
    }
    
    /**
     * Pause notification
     */
    @Override
    public void onPause() {
    	super.onPause();
    	if (L) Log.i(TAG, "onPause");
    }
    
    /**
     * Stop notification
     */
    @Override
    public void onStop() {
    	super.onStop();
    	if (L) Log.i(TAG, "onStop");
    }
    
    /**
     * Notification to save instance state
     */
    @Override
    public void onSaveInstanceState(Bundle toSave) {
    	super.onSaveInstanceState(toSave);
    	if (L) Log.i(TAG, "onSaveInstanceState");
    }
}