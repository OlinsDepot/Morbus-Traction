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
	
	private HandlerThread mSrvcThread;
	private Looper mSrvcLooper;
	private static MbusSrvcMgr mSrvcHandler;
	
	private Socket MbusSrvSocket;
	private CommsThread commsThread;
	static MsgFromSrvr srvrMsgRcv = new MsgFromSrvr();
	
	private HandlerThread mSrvrThread;
	private Looper mSrvrLooper;
	private static MsgToSrvr mMsgToSrvr;

	private final Messenger mMsgFromClient = new Messenger(new MsgFromClient());
	private static Messenger mMsgToClient;
	
	public static final int REGISTER = 7;
	public static final int POWER_ON = 1;
	public static final int POWER_OFF = 2;
	public static final int EMRG_STOP = 3;
	public static final int THROTTLE_CHANGE = 4;
	public static final int KEEP_ALIVE = 5;
	public static final int REG_DECODER = 6;
	
	static final int UPDATE_INTERVAL = 50000;
	private Timer timer = new Timer();
	
	private DCCencoder regDecoders[];


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
		mSrvcThread = new HandlerThread("MBusSrvcMgr", Process.THREAD_PRIORITY_BACKGROUND);
		mSrvcThread.start();
		mSrvcLooper = mSrvcThread.getLooper();
		mSrvcHandler = new MbusSrvcMgr(mSrvcLooper);
		
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
		Message msg = mSrvcHandler.obtainMessage();
		msg.what = 1;
		msg.obj = extras.getString("IP_ADR");
		msg.arg1 = Integer.parseInt(extras.getString("IP_PORT"));
		mSrvcHandler.sendMessage(msg);
		
		return mMsgFromClient.getBinder();
	}
	
	
	/**
	 *  MorBus service shutdown.
	 */
	@Override
	public void onDestroy() {
		if (L) Log.i(TAG, "Stop Mbus Service");
		Message msg = mSrvcHandler.obtainMessage();
		msg.what = 2;
		mSrvcHandler.sendMessage(msg);

		super.onDestroy();
	}

	
	//
	// Handler threads
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
			
			// Start up the service and connect to the server.
			case 1:
				// Start a thread to receive from the server.
				try {
					// create a socket.
					MbusSrvSocket = new Socket(InetAddress.getByName((String) msg.obj), msg.arg1);
//					MbusSrvSocket = new Socket(InetAddress.getByName("192.168.0.43"), 2005);
					commsThread = new CommsThread(MbusSrvSocket);
					commsThread.start();				
				}
				catch (UnknownHostException e) {
					Log.d(TAG, e.getLocalizedMessage());
				}
				catch (IOException e) {
					Log.d(TAG, e.getLocalizedMessage());
				}

				// Start a thread to send to the server
				mSrvrThread = new HandlerThread("SrvrMsgSnd", Process.THREAD_PRIORITY_BACKGROUND);
				mSrvrThread.start();
				mSrvrLooper = mSrvrThread.getLooper();
				mMsgToSrvr = new MsgToSrvr(mSrvrLooper);
				
				//Start a thread to send keep alive commands every 50 seconds
				KeepAliveThread();
				
				//Create array of to hold DCC encoders registered to each throttle. (6 max);
				regDecoders = new DCCencoder[6];


				Toast.makeText(getApplicationContext(), "Mbus Server Connected", Toast.LENGTH_SHORT).show();

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
			int numOfBytesReceived = msg.arg1;
			byte[] buffer = (byte[]) msg.obj;
			
			// extract only the actual bytes received
			byte[] packet = java.util.Arrays.copyOfRange(buffer, 0, numOfBytesReceived);
			
			// On ID response, send connected message.
			if (packet[0] ==2) {
	    		Message msgClient = Message.obtain(null, 2);
	    		try {
	    			mMsgToClient.send(msgClient);
	    		} catch (RemoteException e) {
	    			e.printStackTrace();
	    		}
			}
			// display the text received
			if (L) Log.i("SrvrMsgRcv", bytesToHex(packet));
		}
	};
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	/**
	 *  Message to Server: Parses event from client and generates a
	 *  command packet to send to the server.
	 *  
	 *  @param msg - Message from Main containing an MBus event.
	 */
	private final class MsgToSrvr extends Handler {
		
		private byte serverBuf[];
		
		public MsgToSrvr(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			
			Log.i(TAG, "Message type " + msg.what);
			
			switch (msg.what) {
			
			case REG_DECODER:
				//Register a target decoder to a throttle.
				regDecoders[msg.arg1] = new DCCencoder(msg.arg2);
				break;
			
			case THROTTLE_CHANGE:
				if (L) Log.i("SrvrMsgSnd", "tID=" + msg.arg1 + "speed=" +msg.arg2);
				
				serverBuf = regDecoders[msg.arg1].DCCspeed(msg.arg2);
				
				commsThread.write(serverBuf);
				
				break;
				
			case KEEP_ALIVE:
				if (L) Log.i("SrvrMsgSnd", "Sending Keep_alive");
				
				serverBuf = new byte[1];
				serverBuf[0] = 11;
				commsThread.write(serverBuf);
				break;
				
			default:
				Log.d("SrvrMsgSnd", "Unknown MBus event type " + msg.what);
				super.handleMessage(msg);
		}
			
		}
	}

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

			if (msg.what == REGISTER) {
				mMsgToClient = msg.replyTo;
				

			}
			else {
				Message mMsg = mMsgToSrvr.obtainMessage(msg.what,msg.arg1,msg.arg2);
				mMsgToSrvr.sendMessage(mMsg);
				//TODO if mSrvrHandler not running just do a call to super.handleMessage(msg)
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
				//TODO send keepalive command here.
				Message mMsg = mMsgToSrvr.obtainMessage(KEEP_ALIVE);
				mMsgToSrvr.sendMessage(mMsg);
				
			}
		}, 0, UPDATE_INTERVAL);
	}
	
}