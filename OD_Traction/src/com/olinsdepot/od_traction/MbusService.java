package com.olinsdepot.od_traction;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.widget.Toast;


public class MbusService extends Service {
	private final String TAG = this.getClass().getSimpleName();
	private static final boolean L = true;
	
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private HandlerThread mServiceThread;
	
	private Socket MbusSrvSocket;
	CommsThread commsThread;
	


	// Handler for startup message from the originating thread
	private final class ServiceHandler extends Handler {
	
		//Constructor
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			// Start comms thread and connect to server
			try
			{
				// create a socket.
				// TODO Pass server address and port from main in msg.
				MbusSrvSocket = new Socket(InetAddress.getByName("192.168.0.43"), 2005);
				commsThread = new CommsThread(MbusSrvSocket);
				commsThread.start();				
			}
			catch (UnknownHostException e)
			{
				Log.d(TAG, e.getLocalizedMessage());
			}
			catch (IOException e)
			{
				Log.d(TAG, e.getLocalizedMessage());
			}
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

	
	// New MorBus service is being created.
	@Override
	public void onCreate() {
		if (L) Log.i(TAG, "Create MBus Thread");
		// Start up the thread running the service. Note that we create a 
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		mServiceThread = new HandlerThread("MorBusServiceStartup", Process.THREAD_PRIORITY_BACKGROUND);
		mServiceThread.start();
		
		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = mServiceThread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}
	
	// MorBus service is starting.
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (L) Log.i(TAG, "Starting MBUS");
		Toast.makeText(this,  "MORBUS service starting",  Toast.LENGTH_SHORT).show();
		
		//For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);
		
		// If we get killed, after returning from here, restart
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}
	
	
	// MorBus service is shutting down.
	@Override
	public void onDestroy() {
		if (L) Log.i(TAG, "Stopping MBUS");
		Toast.makeText(this,  "MORBUS service done",  Toast.LENGTH_SHORT).show();
		commsThread.cancel();          // Close the socket
		mServiceThread.quitSafely();   // Shut down the start message queue
		super.onDestroy();
	}

}