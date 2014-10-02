package com.olinsdepot.mbus_srvc;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;


import android.widget.Toast;

/**
 * MorBus Service
 * @author mhughes
 *
 */
public class MbusService extends Service {
	private final String TAG = this.getClass().getSimpleName();
	private static final boolean L = true;
	
	// Thread to start service threads, connect to server and stop service.
	private HandlerThread mSrvcMgrThread;
	private Looper mSrvcMgrLooper;
	private static MbusSrvcMgr mSrvcMgrHandler;
	
	// Thread to make network connection to server.
	private Socket MbusSrvSocket;
	private CommsThread commsThread;
	static MsgFromSrvr mMsgFromSrvr = new MsgFromSrvr();
	private static Messenger mMsgToSrvr;

	private final Messenger mMsgFromClient = new Messenger(new MsgFromClient());
	private static Messenger mMsgToClient;
	
	// Morbus service commands
	public static final int CMD_REGISTER	= 1;	// Register the client receive handler
	public static final int CMD_POWER_ON	= 2;	// Switch on power to the layout
	public static final int CMD_POWER_OFF	= 3;	// Switch off power to the layout
	public static final int CMD_POWER_IS	= 4;	// Request current power status
	public static final int CMD_EMRG_STOP	= 5;	// Send MoRBus reset
	public static final int CMD_ACQ_DECODER	= 6;	// Register a DCC decoder to a throttle
	public static final int CMD_RLS_DECODER	= 7;	// Release a DCC decoder from a throttle
	public static final int CMD_THTL_STEP	= 8;	// Set new throttle step on specified decoder
	public static final int CMD_HARD_STOP	= 9;	// Send hard stop to specified decoder
	public static final int CMD_KEEP_ALIVE	= 10;	// Send a keep alive message to server
	
	// Morbus service events
	public static final int EVT_CONNECT		= 1;	// Service has connected to remote server
	public static final int EVT_PORT_ON		= 2;	// Place holder for now
	

	// Keep alive thread
	static final int UPDATE_INTERVAL = 50000;
	private Timer timer = new Timer();
	
	// Registered decoders for throttle commands
	private static DCCencoder regDecoders[];


	//
	// Service life cycle call backs
	//
	
	/**
	 *  Create new MorBus service.
	 */
	@Override
	public void onCreate() {
		if (L) Log.i(TAG, "Create MBus Service");
		// Create thread to manage service.
		mSrvcMgrThread = new HandlerThread("MBusSrvcMgr", Process.THREAD_PRIORITY_BACKGROUND);
		mSrvcMgrThread.start();
		mSrvcMgrLooper = mSrvcMgrThread.getLooper();
		mSrvcMgrHandler = new MbusSrvcMgr(mSrvcMgrLooper);
		
	}
	
	
	/**
	 * MorBus service binder
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// Bind with messenger
		if (L) Log.i(TAG, "Start Mbus Service");
		
		// Get the IP info to connect to the server
		Bundle extras = intent.getExtras();
		
		// pass IP info to the service
		Message msg = mSrvcMgrHandler.obtainMessage();
		msg.what = 1;
		msg.obj = extras.getString("IP_ADR");
		msg.arg1 = Integer.parseInt(extras.getString("IP_PORT"));
		mSrvcMgrHandler.sendMessage(msg);
		
		return mMsgFromClient.getBinder();
	}
	
	
	/**
	 *  MorBus service shutdown.
	 */
	@Override
	public void onDestroy() {
		if (L) Log.i(TAG, "Stop Mbus Service");
		Message msg = mSrvcMgrHandler.obtainMessage();
		msg.what = 2;
		mSrvcMgrHandler.sendMessage(msg);

		super.onDestroy();
	}

	
	//
	// Handlers
	//
	
	/**
	 *  Mbus Service Manager: Coordinates startup and shutdown of Service components
	 *  by request from the originating thread
	 *  
	 *  @param msg - Message containing request type and ip address of server.
	 */
	private final class MbusSrvcMgr extends Handler {
	
		public MbusSrvcMgr(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (L) Log.i(TAG,"ServiceManager msg = " + msg.what);
			
			switch (msg.what) {
			
			// Start up the service.
			case 1:
				// Connect to the server.
				try {
					// create a socket.
					MbusSrvSocket = new Socket(InetAddress.getByName((String) msg.obj), msg.arg1);
					
					// Start the Comms thread on the socket.
					commsThread = new CommsThread(MbusSrvSocket);
					commsThread.start();				
				}
				catch (UnknownHostException e) {
					Log.d(TAG, e.getLocalizedMessage());
				}
				catch (IOException e) {
					Log.d(TAG, e.getLocalizedMessage());
				}

				//Start a thread to send keep alive commands every 50 seconds
				KeepAliveThread();
				
				//Create array of to hold DCC encoders registered to each throttle. (4 max);
				regDecoders = new DCCencoder[4];

				// Announce service startup
				Toast.makeText(getApplicationContext(), "Mbus Service Started", Toast.LENGTH_SHORT).show();
				break;
			
			// Disconnect the server and shut down the service.
			case 2:
				//Shut down service
				//TODO Close socket and cancel related tasks, then shutdown the service.
				if (L) Log.i(TAG,"Stop comms thread");

				Toast.makeText(getApplicationContext(), "Mbus Server Disconnected", Toast.LENGTH_SHORT).show();

				break;
			default:
				//Unknown command in message
				Log.d(TAG, "MbusSrvcMgr unknown request type");
			}
		}
	}

	
	/**
	 * Message from Server: Parses server message and generates events
	 * to be sent to the client.
	 * 
	 * @param msg - Message containing byte count and byte data sent from server.
	 */
	static class MsgFromSrvr extends Handler {
		@Override
		public void handleMessage(Message msg)
		{
			// Dispatch event
			switch (msg.what) {

			// Event = CONNECTED - save servers message handler.
			case EVT_CONNECT:
				mMsgToSrvr = msg.replyTo;
	    		Message msgClient = Message.obtain(null, 2);
	    		try {
	    			mMsgToClient.send(msgClient);
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}

				break;
				
			// All other events
    		default:
				//Unknown event in message
				Log.d("MsgFromSrvr", "Unknown event type");
    			break;
			}
		}
	};
	
	
	
	/**
	 * Handle messages from client UI. Bound to main thread. Passes client events from
	 * Main thread to MBus service to be parsed into MBus commands and sent.
	 * 
	 * @param msg - Message containing an event for Mbus service to process.
	 */
	static class MsgFromClient extends Handler {
		
		@Override
		public void handleMessage(Message msg) {
			if (L) Log.i("ClientHandler", "Message Received = " + msg.what);

			switch (msg.what) {
			
			// Register the client receiver
			case CMD_REGISTER:
				mMsgToClient = msg.replyTo;
				break;

			case CMD_ACQ_DECODER:
				//Register a target decoder to a throttle.
				regDecoders[msg.arg1] = new DCCencoder(msg.arg2);
				break;
				
			case CMD_THTL_STEP:
				// Dispatch a change in throttle step for the decoder registered to the throttle in arg1.
				Message SrvrMsg = Message.obtain(null, msg.what,msg.arg1,msg.arg2);
				SrvrMsg.obj = regDecoders[msg.arg1];
				try {
					mMsgToSrvr.send(SrvrMsg);
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}
				
			default:
				//Unknown command in message
				Log.d("MsgFromClient", "Unknown command type");
			}
		}
	}
	
	/**
	 * Keep Alive Thread. Posts a keep-alive message to the send queue every 50 seconds.
	 *
	 */
	private void KeepAliveThread () {
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				Log.d(TAG, "KeepAliveTask");
				
				if (mMsgToSrvr != null) {
					//Send server a keep alive message.
					Message SrvrMsg = Message.obtain(null,CMD_KEEP_ALIVE);
					try {
						mMsgToSrvr.send(SrvrMsg);
		    		} catch (RemoteException e) {
		    			e.printStackTrace();
		    		}
				}
			}
		}, 0, UPDATE_INTERVAL);
	}
	
}