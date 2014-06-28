package com.olinsdepot.od_traction;

import java.io.IOException;
import java.net.UnknownHostException;

import com.olinsdepot.od_traction.ThrottleFragment.OnThrottleChangeListener;

//import com.olinsdepot.od_traction.MainActivity.WriteToServerTask;

import android.app.Activity;
import android.app.Fragment;

import android.content.Context;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * The Network fragment gets network address of the Morbus server from user.
 */
public class NetFragment extends Fragment {
	
	/**
	 * Logging - String for the class name, Logging on/off flag.
	 */
	private final String TAG = getClass().getSimpleName();
	private static final boolean L = true;

	// Container Activity must implement this interface
	public interface OnServerChangeListener{
		/**
		 * Called when User hits the connect button with a valid IP
		 */
		public void onServerChange(String mSrvr, int mPort);
	}
	private OnServerChangeListener netListener;

	/**
	 * Link back to Main activity, set when fragment is attached.
	 */
	MainActivity mActivity;
	
	 
	 /**
	  * Fragment characteristic, an example
	  */
	 private static final String ARG_IPADR = "IP_ADR";
	 
	 /**
	  * Null constructor for this fragment
	  */
	 public NetFragment() { }
	 
	 /**
	  * Returns a new instance of the NET view fragment
	  */
	 public static NetFragment newInstance() {
		 NetFragment thisfrag = new NetFragment();
		 Bundle args = new Bundle();
		 args.putString(ARG_IPADR, "192.168.1.0");
		 thisfrag.setArguments(args);
		 return thisfrag;
	 }

	 /**
	  * onClickListener for Connect Button
	  * @param view
	 public void onClickConnect(View view) {
		 String mSrvrAddr = "192.168.1.0";
		 int mSrvrPort = 500;
		 
	     if (L) Log.i(TAG, "onClick Connect");
		 
		 ConnectivityManager connMgr = (ConnectivityManager) this.mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
		 NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		 if (networkInfo == null) {
			 //no network connection
		 }
	  */

/*
			// Send message to the server
			private class WriteToServerTask extends AsyncTask
			<byte[], Void, Void> {

				@Override
				protected Void doInBackground(byte[]... data)
				{
					mbusSrvr.write(data[0]);
					return null;
				}
			}
			
			// Close connection to the server
			private class CloseSocketTask extends AsyncTask
			<Void, Void, Void> {

				@Override
				protected Void doInBackground(Void... params) {
					try {
						mbusSrvr.cancel();
					}
					catch (IOException e)
					{
						Log.d("Traction", e.getLocalizedMessage());
					}
					return null;
				}
			}
			
			// Method to send a string to the server
			private void sendToServer(String message)
			{
				byte[] theByteArray = message.getBytes();
				new WriteToServerTask().execute(theByteArray);
			}


			// Handler to process a morbus message from the server.
			static Handler mbusMsgIn = new Handler()
			{
				@Override
				public void handleMessage(Message msg)
				{
					int type =msg.what;
					if (type == 0) {
						int numOfBytesReceived = msg.arg1;
						byte[] buffer = (byte[]) msg.obj;
						
						// convert the entire byte array to string
						String strReceived = new String(buffer);
						
						// extract only the actual string received
						strReceived = strReceived.substring(0, numOfBytesReceived);
						
						// display the text received on the TextView
			
					} else if (type == 1) {
						int tracID = msg.arg1;
						int speed = msg.arg2;
			//			sendToServer("ID="+tracID+" Speed="+speed);
					}
				}
			};

*/
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
            netListener = (OnServerChangeListener) activity;
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

        View netFragView = inflater.inflate(R.layout.fragment_net, container, false);
        
        final EditText hostName = (EditText) netFragView.findViewById(R.id.hostName);

		Button netCnctBtn = (Button) netFragView.findViewById(R.id.cnctBtn);
		netCnctBtn.setOnClickListener(
			new OnClickListener() {
				@Override
				public void onClick(final View thisView) {
					//Pass View through to the handler so that findViewById
					//can be used to get a handle on the fragments own views.
				     if (L) Log.i(TAG, "onClick Connect");
				     netListener.onServerChange(hostName.getText().toString(), 2000);
				}
			}
		);
                
        return netFragView;
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