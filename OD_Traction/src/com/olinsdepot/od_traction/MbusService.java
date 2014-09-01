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
	static SrvrMsgRcv srvrMsgRcv = new SrvrMsgRcv();
	
	private HandlerThread mSrvrThread;
	private Looper mSrvrLooper;
	private static SrvrMsgSnd mSrvrHandler;

	private final Messenger mClientMsgRcv = new Messenger(new ClientMsgRcv());
	
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
		Message msg = mSrvcHandler.obtainMessage();
		msg.what = 1;
		mSrvcHandler.sendMessage(msg);
		
		return mClientMsgRcv.getBinder();
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
			
			switch (msg.what) {
			case 1:
				//Start up service
				if (L) Log.i(TAG,"Start comms thread");
				
				// Start a thread to receive from the server
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

				// Start a thread to send to the server
				mSrvrThread = new HandlerThread("SrvrMsgSnd", Process.THREAD_PRIORITY_BACKGROUND);
				mSrvrThread.start();
				mSrvrLooper = mSrvrThread.getLooper();
				mSrvrHandler = new SrvrMsgSnd(mSrvrLooper);
				
				//Start a thread to send keep alive commands every 50 seconds
				KeepAliveThread();
				
				//Create array of to hold DCC encoders registered to each throttle. (6 max);
				regDecoders = new DCCencoder[6];


				Toast.makeText(getApplicationContext(), "Mbus Server Connected", Toast.LENGTH_SHORT).show();

				break;
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
	 * Server message receiver: Parses server response and generates an event
	 * to be sent to the client.
	 * 
	 * @param msg - Message containing byte count and byte data sent from server.
	 */
	static class SrvrMsgRcv extends Handler {
		@Override
		public void handleMessage(Message msg)
		{
			int numOfBytesReceived = msg.arg1;
			byte[] buffer = (byte[]) msg.obj;
			
			// convert the entire byte array to string
			//String strReceived = new String(buffer);
			
			// extract only the actual string received
			//strReceived = strReceived.substring(0, numOfBytesREceived);
			byte[] packet = java.util.Arrays.copyOfRange(buffer, 0, numOfBytesReceived);
			
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
	 *  Server message sender: Parses event from client and generates a
	 *  command packet to send to the server.
	 *  
	 *  @param msg - Message from Main containing an MBus event.
	 */
	private final class SrvrMsgSnd extends Handler {
		
		private byte serverBuf[];
		
		public SrvrMsgSnd(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			
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
	 * Client message receiver. Bound to main thread. Passes client events from
	 * Main thread to MBus service to be parsed into MBus commands and sent.
	 * 
	 * @param msg - Message containing an event for Mbus service to process.
	 */
	static class ClientMsgRcv extends Handler {
		
		@Override
		public void handleMessage(Message msg) {
			if (L) Log.i("ClientHandler", "Message Received = " + msg.what);
			Message mMsg = mSrvrHandler.obtainMessage(msg.what,msg.arg1,msg.arg2);
			mSrvrHandler.sendMessage(mMsg);
			//TODO if mSrvrHandler not running just do a call to super.handleMessage(msg)
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
				Message mMsg = mSrvrHandler.obtainMessage(KEEP_ALIVE);
				mSrvrHandler.sendMessage(mMsg);
				
			}
		}, 0, UPDATE_INTERVAL);
	}
	
	/**
	 * DCC command encoder
	 * @param command
	 */
	
	private class DCCencoder {
		private byte dcdrAdr;
		private byte encoderBuf[];
		
		//Constructor sets decoder address
		DCCencoder (int newDcdr) {
			this.dcdrAdr = (byte) (newDcdr & (0xff));
		}
		
		//Method: encode speed/dir command
		byte [] DCCspeed (int speed) {
			
			byte throttleStep = (byte)((speed >>>3) & (0x1F));

			encoderBuf = new byte[9];
			encoderBuf[0] = (byte) 0x07;
			encoderBuf[1] = (byte) 0x03;
			encoderBuf[2] = (byte) 0x00;
			encoderBuf[3] = (byte) 0x19;
			encoderBuf[4] = (byte) 0x00;
			encoderBuf[5] = (byte) 0x00;
			encoderBuf[6] = (byte) 0x80;
			encoderBuf[7] = this.dcdrAdr;
			encoderBuf[8] = (byte) ((byte) 0x60 | throttleStep);
			
			return encoderBuf;
			
		}
	}
}