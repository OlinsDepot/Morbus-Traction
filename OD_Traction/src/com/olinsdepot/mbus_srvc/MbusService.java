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
import android.util.SparseArray;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.olinsdepot.mbus_srvc.CommsThread.*;


/**
 *  The MorBus Service translates the generic functions, (speed step, headlight on, etc.)
 *  produced by user action on the GUI into commands to a remote MorBus server. The service
 *  receives events from the main activity GUI and translates then into commands to the EmCAN
 *  layer below which is connected to the remote server via LAN. The service translates responses
 *  received from the EmCAN layer into events passed to the main activity which then updates the
 *  state of the GUI. Work done in the service is performed on separate threads to avoid blocking
 *  the GUI. 
 *  
 * @author mhughes
 *
 */
public class MbusService extends Service {
	private final String TAG = this.getClass().getSimpleName();
	private static final boolean L = true;
	

	/**
	 *  Morbus service commands
	 */
	public static enum MbusSrvcCmd {
		CONNECT,
		DISCONNECT,
		POWER_ON,
		POWER_OFF,
		POWER_IS,
		EMRG_STOP,
		ACQ_DECODER,
		RLS_DECODER,
		THTL_STEP,
		HARD_STOP;
		
		/* Returns the code for this MorBus Service command */
		public int toCode() {
			return this.ordinal();
		}
		
		/* Returns the MorBus Service command for the code passed */
		public static MbusSrvcCmd fromCode(int cmd) {
			return MbusSrvcCmd.values()[cmd];
		}
	}
	
	/**
	 * Morbus service events
	 */
	public static enum MbusSrvcEvt {
		CONNECTED,
		DISCONNECTED,
		PORT_ON;

		/* Return the code for this MorBus Service event. */
		public int toCode() {
			return this.ordinal();
		}
		
		/* Return the MorBus Service event for the code passed. */
		public static MbusSrvcEvt fromCode(int evt) {
			return MbusSrvcEvt.values()[evt];
		}
		
	}
	

	/*
	 * Thread to handle the upward interface to GUI client. It receives commands from the GUI
	 * on the ClientToSrvc messenger queue and sends responses to the SrvcToClient message
	 * handler in the GUI.
	 */
	private HandlerThread mClientToSrvcThread;
	private Looper mClientToSrvcLooper;
	private static Handler mClientToSrvcHandler;
	private static Messenger mSrvcToClientMsgr;
	
	/* 
	 * Thread to handle the downward interface to the EmCAN bus driver. GUI commands are
	 * decomposed into EmCAN transactions and sent to the driver via the SrvcToComms message
	 * queue. Responses from the EmCAN driver are received on the CommsToSrvc message queue.
	 */
	private Socket MbusSrvSocket;
	private CommsThread mCommsThread;
	static Handler mCommsToSrvcHandler = new CommsToSrvcHandler();
	private static Messenger mSrvcToCommsMsgr;

	
	// Keep alive thread
	static final int UPDATE_INTERVAL = 50000;
	private Timer timer = new Timer();
	
	// Registered decoders for throttle commands
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
	 *  MorBus service unexpectedly shutdown.
	 */
	@Override
	public void onDestroy() {
		if (L) Log.i(TAG, "Mbus Service Stopping");
		Message msg = mClientToSrvcHandler.obtainMessage();
		msg.what = MbusSrvcEvt.DISCONNECTED.toCode();
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
			if (L) Log.i(TAG,"MBUS Client Msg Hdlr msg = " + msg.what);
		
			// Dispatch the incoming message based on 'what'.
			switch (MbusSrvcCmd.fromCode(msg.what)) {
			
			// Connect to the server.
			case CONNECT:
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
			case DISCONNECT:
				//Shut down service
				//TODO Close socket and cancel related tasks, then shutdown the service.
				if (L) Log.i(TAG,"Stop comms thread");

				Toast.makeText(getApplicationContext(), "Mbus Server Disconnected", Toast.LENGTH_SHORT).show();
				break;

			//Register a target decoder to a throttle.
			case ACQ_DECODER:
				regDecoders[msg.arg1] = new DCCencoder((Bundle)msg.obj);
				break;
				
			// Dispatch a change in throttle step for the decoder registered to the throttle in arg1.
			case THTL_STEP:
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
			switch (CommsEvt.fromCode(msg.what)) {
			
			case START:
				mSrvcToCommsMsgr = msg.replyTo;
				break;

			// Event = CONNECTED - save servers message handler.
			case CONNECT:
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
				Log.d("CommsToSrvcHandler", "Unknown event type");
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
					Message SrvrMsg = Message.obtain(null,CommsCmd.SND_KEEPALIVE.toCode());
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