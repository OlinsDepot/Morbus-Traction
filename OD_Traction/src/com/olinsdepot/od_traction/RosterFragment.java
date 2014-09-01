package com.olinsdepot.od_traction;

import android.app.Activity;
import android.app.Fragment;
//import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

/**
 * The Network fragment gets network address of the Morbus server from user.
 */
public class RosterFragment extends Fragment {
	private final String TAG = getClass().getSimpleName();
	private static final boolean L = true;

	// Container Activity must implement this interface
	public interface OnRosterChangeListener{
		/**
		 * Called when User hits the connect button with a valid IP
		 */
		public void onRosterChange(int tID, int dcdrAdr);
	}
	private OnRosterChangeListener rosterListener;

	/**
	 * Link back to Main activity, set when fragment is attached.
	 */
	MainActivity mActivity;
	
	 
	 /**
	  * Fragment characteristic, an example
	  */
//	 private static final int dcdrAdr = 0;
	 
	 /**
	  * Null constructor for this fragment
	  */
	 public RosterFragment() { }
	 
	 /**
	  * Returns a new instance of the ROSTER view fragment
	  */
	 public static RosterFragment newInstance() {
		 RosterFragment thisfrag = new RosterFragment();
		 Bundle dcdr = new Bundle();
		 dcdr.putInt("Throttle_ID", 0);
		 dcdr.putInt("DCDR_ADDRESS", 0);
		 thisfrag.setArguments(dcdr);
		 return thisfrag;
	 }


	/**
     * Life cycle methods for the NET fragment
     */
    
	// On Attach method
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (L) Log.i(TAG, "onAttach " + activity.getClass().getSimpleName());
        
        // Open interface to container fragment.
        try {
            rosterListener = (OnRosterChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Server Change Listener");
        }

        // Give main the section number so it can update the Action Bar title.
        this.mActivity = (MainActivity) activity;
        this.mActivity.onSectionAttached(1);

    }

    // On Create method
    @Override
    public void onCreate(Bundle saved) {
    	super.onCreate(saved);
    	if (L) Log.i(TAG, "onCreate");
    }


	// On CreateView method
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (L) Log.i(TAG, "onCreateView");

        View rosterFragView = inflater.inflate(R.layout.fragment_roster, container, false);
        
        final EditText dcdrAdr1 = (EditText) rosterFragView.findViewById(R.id.dcdrAdr1);

		ToggleButton dcdrSel1Btn = (ToggleButton) rosterFragView.findViewById(R.id.dcdrSel1);
		dcdrSel1Btn.setOnCheckedChangeListener(
			new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				     if (L) Log.i(TAG, "onClick Connect");
				     
				     if (isChecked) {
				    	 rosterListener.onRosterChange(0, Integer.valueOf(dcdrAdr1.getText().toString()));
				     }
				     else {
				    	 rosterListener.onRosterChange(0, 0);
				     }
				}
			}
		);

		final EditText dcdrAdr2 = (EditText) rosterFragView.findViewById(R.id.dcdrAdr2);

		ToggleButton dcdrSel2Btn = (ToggleButton) rosterFragView.findViewById(R.id.dcdrSel2);
		dcdrSel2Btn.setOnCheckedChangeListener(
			new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				     if (L) Log.i(TAG, "onClick Connect");
				     
				     if (isChecked) {
				    	 rosterListener.onRosterChange(1, Integer.valueOf(dcdrAdr2.getText().toString()));
				     }
				     else {
				    	 rosterListener.onRosterChange(1, 0);
				     }
				}
			}
		);
                
        return rosterFragView;
    }

    
    // On ActivityCreated method
    @Override
    public void onActivityCreated(Bundle saved) {
    	super.onActivityCreated(saved);
    	if (L) Log.i(TAG, "onActivityCreated");
    }
    

    // On Start method
    @Override
    public void onStart() {
    	super.onStart();
    	if (L) Log.i(TAG, "onStart");
    }

    
    // On Resume method
    @Override
    public void onResume() {
    	super.onResume();
    	if (L) Log.i(TAG, "onResume");
    }
    

    // On Pause method
    @Override
    public void onPause() {
    	super.onPause();
    	if (L) Log.i(TAG, "onPause");
    }
    

    // On Stop method
    @Override
    public void onStop() {
    	super.onStop();
    	if (L) Log.i(TAG, "onStop");
    }
    

    // On SaveInstanceState method
    @Override
    public void onSaveInstanceState(Bundle toSave) {
    	super.onSaveInstanceState(toSave);
    	if (L) Log.i(TAG, "onSaveInstanceState");
    }
    
}