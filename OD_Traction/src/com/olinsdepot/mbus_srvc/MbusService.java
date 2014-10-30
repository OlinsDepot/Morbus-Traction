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
	private HandlerThread mClientToSrvcThread;
	private Looper mClientToSrvcLooper;
	private static Handler mClientToSrvcHandler;
	private static Messenger mSrvcToClientMsgr;
	
	// Thread to make network connection to server.
	private Socket MbusSrvSocket;
	private CommsThread mCommsThread;
	static Handler mCommsToSrvcHandler = new CommsToSrvcHandler();
	private static Messenger mSrvcToCommsMsgr;

	
	// Morbus service commands
	public static final int CMD_CONNECT		= 1;	// Connect to server in passed arg.
	public static final int CMD_DISCONNECT	= 2;	// Disconnect server & shutdown service
	public static final int CMD_POWER_ON	= 4;	// Switch on power to the layout
	public static final int CMD_POWER_OFF	= 5;	// Switch off power to the layout
	public static final int CMD_POWER_IS	= 6;	// Request current power status
	public static final int CMD_EMRG_STOP	= 7;	// Send MoRBus reset
	public static final int CMD_ACQ_DECODER	= 8;	// Register a DCC decoder to a throttle
	public static final int CMD_RLS_DECODER	= 9;	// Release a DCC decoder from a throttle
	public static final int CMD_THTL_STEP	= 10;	// Set new throttle step on specified decoder
	public static final int CMD_HARD_STOP	= 11;	// Send hard stop to specified decoder
	public static final int CMD_KEEP_ALIVE	= 12;	// Send a keep alive message to server
	
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
		// Create thread to dispatch incoming messages from Client side interface.
		mClientToSrvcThread = new HandlerThread("MBusSrvcMgr", Process.THREAD_PRIORITY_BACKGROUND);
		mClientToSrvcThread.start();
		mClientToSrvcLooper = mClientToSrvcThread.getLooper();
		mClientToSrvcHandler = new ClientToSrvcHandler(mClientToSrvcLooper);
		
	}
	
	
	/**
	 * MorBus service binder
	 */
	@Override
	public IBinder onBind(Intent intent) {
		if (L) Log.i(TAG, "Start Mbus Service");
		
		// Pass the service's Client message handler back.
		return new Messenger(mClientToSrvcHandler).getBinder();
	}
	
	
	/**
	 *  MorBus service shutdown.
	 */
	@Override
	public void onDestroy() {
		if (L) Log.i(TAG, "Mbus Service Stopping");
		Message msg = mClientToSrvcHandler.obtainMessage();
		msg.what = CMD_DISCONNECT;
		mClientToSrvcHandler.sendMessage(msg);

		super.onDestroy();
	}

	
	//
	// Handlers
	//
	
	/**
	 *  Mbus Client Message Handler: Coordinates startup and shutdown of Service components.
	 *  Dispatches messages from the client side.
	 *  by request from the originating thread
	 *  
	 *  @param msg - Message containing request type and ip address of server.
	 */
	private final class ClientToSrvcHandler extends Handler {
	
		public ClientToSrvcHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (L) Log.i(TAG,"ServiceManager msg = " + msg.what);
		
			// Dispatch the incoming message based on 'what'.
			switch (msg.what) {
			
			// Connect to the server.
			case CMD_CONNECT:
				// Get IP information for the target server
				Bundle mSrvrIP = (Bundle)msg.obj;
				
				// Register the Client's handler for messages from the service.
				mSrvcToClientMsgr = msg.replyTo;
	
				// Extract IP info and create a socket. Start Comms Thread on new Socket.
				String mAddr = mSrvrIP.getString("IP_ADR");
				int mPort = Integer.parseInt(mSrvrIP.getString("IP_PORT"));
				
				try {
					MbusSrvSocket = new Socket(InetAddress.getByName(mAddr), mPort);
					
					// Start the Comms thread on the socket.
					mCommsThread = new CommsThread(MbusSrvSocket);
					mCommsThread.start();				
				}
				catch (UnknownHostException e) {
					Log.d(TAG, e.getLocalizedMessage());
				}
				catch (IOException e) {
					Log.d(TAG, e.getLocalizedMessage());
				}

				//Start a thread to send keep alive commands every 50 seconds
				KeepAliveThread();
				
				//Create array to hold DCC encoders registered to each throttle. (4 max);
				regDecoders = new DCCencoder[4];

				// Announce service startup
				Toast.makeText(getApplicationContext(), "Mbus Service Started", Toast.LENGTH_SHORT).show();
				break;
			
			// Disconnect the server and shut down the service.
			case CMD_DISCONNECT:
				//Shut down service
				//TODO Close socket and cancel related tasks, then shutdown the service.
				if (L) Log.i(TAG,"Stop comms thread");

				Toast.makeText(getApplicationContext(), "Mbus Server Disconnected", Toast.LENGTH_SHORT).show();
				break;

			//Register a target decoder to a throttle.
			case CMD_ACQ_DECODER:
				regDecoders[msg.arg1] = new DCCencoder(msg.arg2);
				break;
				
			// Dispatch a change in throttle step for the decoder registered to the throttle in arg1.
			case CMD_THTL_STEP:
				Message SrvrMsg = Message.obtain(null, msg.what,msg.arg1,msg.arg2);
				SrvrMsg.obj = regDecoders[msg.arg1];
				try {
					mSrvcToCommsMsgr.send(SrvrMsg);
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}
				
			//Unknown command in message
			default:
				Log.d("MsgFromClient", "Unknown command type");
				}
		}
	}

	
	/**
	 * Message from Server: Parses server message and generates events
	 * to be sent to the client.
	 * 
	 * @param msg - Message containing byte count and byte data sent from server.
	 */
	static class CommsToSrvcHandler extends Handler {
		@Override
		public void handleMessage(Message msg)
		{
			// Dispatch event
			switch (msg.what) {
			
			case CommsThread.EVT_START:
				mSrvcToCommsMsgr = msg.replyTo;
				break;
				

			// Event = CONNECTED - save servers message handler.
			case CommsThread.EVT_CONNECT:
	    		Message msgClient = Message.obtain(null, 2);
	    		try {
	    			mSrvcToClientMsgr.send(msgClient);
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
	 * Keep Alive Thread. Posts a keep-alive message to the send queue every 50 seconds.
	 *
	 */
	private void KeepAliveThread () {
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				Log.d(TAG, "KeepAliveTask");
				
				if (mSrvcToCommsMsgr != null) {
					//Send server a keep alive message.
					Message SrvrMsg = Message.obtain(null,CMD_KEEP_ALIVE);
					try {
						mSrvcToCommsMsgr.send(SrvrMsg);
		    		} catch (RemoteException e) {
		    			e.printStackTrace();
		    		}
				}
			}
		}, 0, UPDATE_INTERVAL);
	}
	
}