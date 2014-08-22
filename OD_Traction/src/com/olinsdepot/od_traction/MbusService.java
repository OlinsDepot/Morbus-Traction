package com.olinsdepot.od_traction;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.util.Log;

//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.Socket;
//import java.net.UnknownHostException;

import android.widget.Toast;

/**
 * MorBus Service
 * @author mhughes
 *
 */
public class MbusService extends Service {
	private final String TAG = this.getClass().getSimpleName();
	private static final boolean L = true;
	
	private HandlerThread mSrvcThread;
	private Looper mSrvcLooper;
	private ServiceHandler mSrvcHandler;
	
	//private Socket MbusSrvSocket;
	//private CommsThread commsThread;

	//private HandlerThread mClientThread;
	//private Looper mClientLooper;
	private ClientHandler mClientHandler = new ClientHandler();
	final Messenger mMessenger = new Messenger(mClientHandler);
	
	// MorBus Service events
	static final int THROTTLE_CHANGE = 1;




	//
	// Service Lifecycle Methods
	//
	
	/**
	 *  New MorBus service  created.
	 */
	@Override
	public void onCreate() {
		if (L) Log.i(TAG, "Create MBus Service");
		// Create thread to manage service and setup handler.
		mSrvcThread = new HandlerThread("MorBusServiceStartup", Process.THREAD_PRIORITY_BACKGROUND);
		mSrvcThread.start();
		mSrvcLooper = mSrvcThread.getLooper();
		mSrvcHandler = new ServiceHandler(mSrvcLooper);
		
	}
	
	/**
	 *  MorBus service startup.
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (L) Log.i(TAG, "Starting MBUS " + startId);
		Toast.makeText(this,  "MORBUS service starting",  Toast.LENGTH_SHORT).show();
		
		//For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mSrvcHandler.obtainMessage();
		msg.arg1 = startId;
		mSrvcHandler.sendMessage(msg);
		
		// If we get killed, after returning from here, restart
		return START_NOT_STICKY;
	}
	 */
	
	/**
	 * MorBus service binder
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// Bind with messenger
		Log.d(TAG, "Binding");
//		Toast.makeText(getApplicationContext(),  "binding", Toast.LENGTH_SHORT).show();
		Message msg = mSrvcHandler.obtainMessage();
		msg.arg1 = 1;
		mSrvcHandler.sendMessage(msg);
		
		return mMessenger.getBinder();
	}
	
	
	/**
	 *  MorBus service shutdown.
	 */
	@Override
	public void onDestroy() {
		if (L) Log.i(TAG, "Stopping MBUS");
		Toast.makeText(this,  "MORBUS service done",  Toast.LENGTH_SHORT).show();
//		commsThread.cancel();          // Close the socket
		mSrvcThread.quitSafely();      // Shut down the start message queue
		super.onDestroy();
	}
	
	//
	// Message handler threads
	//
	
	/**
	 *  Handler for startup message from the originating thread
	 */
	private final class ServiceHandler extends Handler {
	
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG,"Start comms thread");
			/**
			try {
				// create a socket.
				// TODO Pass server address and port from main in msg.
				MbusSrvSocket = new Socket(InetAddress.getByName("192.168.0.43"), 2005);
				commsThread = new CommsThread(MbusSrvSocket);
				commsThread.start();				
			}
			catch (UnknownHostException e) {
				Log.d(TAG, e.getLocalizedMessage());
			}
			catch (IOException e) {
				Log.d(TAG, e.getLocalizedMessage());
			}
			**/ //skip this for the moment while we get client/service interface working.
		}
	}

	
	// used for updating the UI on the main activity
	static Handler rcvMsgHndlr = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			int numOfBytesREceived = msg.arg1;
			byte[] buffer = (byte[]) msg.obj;
			
			// convert the entire byte array to string
			String strReceived = new String(buffer);
			
			// extract only the actual string received
			strReceived = strReceived.substring(0, numOfBytesREceived);
			
			// display the text received
			Log.i("rcvMsgHndlr", strReceived);
		}
	};
	
	
	//
	// MorBus service, Client side interface
	//
	
	
	/**
	 * Handler for incoming messages from clients
	 */
	static class ClientHandler extends Handler {
		
		@Override
		public void handleMessage(Message msg) {
			Log.d("ClientHandler", "Message Received" + msg.what);
			
			switch (msg.what) {
				case THROTTLE_CHANGE:
					Log.d("ClientHandler", "tID=" + msg.arg1 + "speed=" +msg.arg2);
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}
	
	

}