package com.olinsdepot.od_traction;

//import android.app.Activity;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.olinsdepot.od_traction.VerticalSeekBar;


public class ThrottleFragment extends Fragment {
/** tbd:	
**/
	// Container Activity must implement this interface
	public interface OnThrottleChangeListener{
		/**
		 * Called when UI detects a change in throttle setting.
		 * @param tID
		 * @param speed
		 */
		public void onThrottleChange(int tID, int speed);
	}

	private OnThrottleChangeListener tListener;
	
	/**
	 * Logging flag and string for the class name
	 */
	private static final boolean L = true;
	private final String TAG = getClass().getSimpleName();
	
	/**
	 * Fragment characteristics, an example.
	 */
	private static final String ARG_HAND = "HANDVIEW";

	private int tID = 3;
	private int tSet = 50;
	private int tDir = 0;
	private int speed = 0;


    /**
     * Returns a new instance of the throttle fragment
     */
    public static ThrottleFragment newInstance(int hand) {
        ThrottleFragment fragment = new ThrottleFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_HAND, hand);
        fragment.setArguments(args);
        return fragment;
    }

 
	/**
	 * Life cycle functions
	 */
    
    // On Attach method
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        if (L) Log.i(TAG, "onAttach " + activity.getClass().getSimpleName());
 
        // Open interface to container fragment.
        try {
            tListener = (OnThrottleChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Throttle Change Listener");
        }

   }
	
	// On Create method
	public void onCreate(Bundle fragState) {
		super.onCreate(fragState);

        if (L) Log.i(TAG, "onCreate");
        
		// tbd initialize throttle settings.
	}
	
	// On Create View method
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (L) Log.i(TAG, "onCreateView");

        tID = getArguments().getInt("HANDVIEW");
        
        
        final View tFragView = inflater.inflate(R.layout.fragment_throttle, container, false);
		
		Button tReverseButton = (Button) tFragView.findViewById(R.id.BTNREV);
		tReverseButton.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(final View v) {
					//Pass tFragView through to the handler so that findViewById
					//can be used to get a handle on the fragments own views.
					setDirRev(tFragView);
				}
			}
		);

		Button tStopButton = (Button) tFragView.findViewById(R.id.BTNSTOP);
		tStopButton.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(final View v) {
					//Pass tFragView through to the handler so that findViewById
					//can be used to get a handle on the fragments own views.
					setDirStop(tFragView);
				}
			}
		);

		Button tForwardButton = (Button) tFragView.findViewById(R.id.BTNFWD);
		tForwardButton.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(final View v) {
					//Pass tFragView through to the handler so that findViewById
					//can be used to get a handle on the fragments own views.
					setDirFwd(tFragView);
				}
			}
		);

		VerticalSeekBar vbarThrottle = (VerticalSeekBar)tFragView.findViewById(R.id.Throttle);
		vbarThrottle.setMax(100);
		vbarThrottle.setProgress(tSet);
		vbarThrottle.setOnSeekBarChangeListener(
			new VerticalSeekBar.OnSeekBarChangeListener() {
				@Override
				public void onStopTrackingTouch(VerticalSeekBar seekBar) {
//					sendToServer("T1done");
					speed = tDir*tSet;
					tListener.onThrottleChange(tID, speed);
				}

				@Override
				public void onStartTrackingTouch(VerticalSeekBar seekBar) {
//					sendToServer("T1start");
				}

				@Override
				public void onProgressChanged(VerticalSeekBar seekBar, int progress, boolean fromUser) {
//					sendToServer("T1value="+progress);
					tSet = progress;
		
				}
			}
		);
		
		return tFragView;
	}
	
	// On Activity created
	@Override
	public void onActivityCreated(Bundle saved) {
		super.onActivityCreated(saved);
        if (L) Log.i(TAG, "onPause");
	}
	
	// On Start
	@Override
	public void onStart() {
		super.onStart();
        if (L) Log.i(TAG, "onPause");
	}
	
	// On Resume method
	@Override
	public void onResume()
	{
		super.onResume();
        if (L) Log.i(TAG, "onResume");
	}
	
	// On Pause method
	@Override
	public void onPause()
	{
		super.onPause();
        if (L) Log.i(TAG, "onPause");
	}
	
	// On Stop method
	@Override
	public void onStop() {
		super.onStop();
		if (L) Log.i(TAG, "onStop");
	}
	
	// Save Instance State method
	@Override
	public void onSaveInstanceState(Bundle toSave) {
		super.onSaveInstanceState(toSave);
		if (L) Log.i(TAG, "onSaveInstanceState");
	}
	


	/**
	 *  Button Handlers
	 */
	 private void setDirRev (View myView) {
		//logic to set direction to reverse.
		if (tDir == 0) {
			tDir = -1;
		}
	}

	private void setDirStop (View myView) {
		//logic to set direction to stop.
		if (tDir != 0) {
			tDir = 0;
			tSet = 0;
		}
	}

	private void setDirFwd (View myView) {
		//logic to set direction to forward.
		if (tDir == 0) {
			tDir = +1;
		}
	}

}
