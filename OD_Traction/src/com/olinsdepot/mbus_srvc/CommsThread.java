package com.olinsdepot.mbus_srvc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;

import android.util.Log;

public class CommsThread extends Thread {
	private final String TAG = getClass().getSimpleName();
	private static final boolean L = true;

	// TCP stream
	private final Socket mSocket;
	private final InputStream inputStream;
	private final OutputStream outputStream;
	
	// Thread to send to server
	private static HandlerThread mSrvrSndThread;
	private static Looper mSrvrSndLooper;
	private static SndToSrvr mSndToSrvrHandler;

	private enum srvrState {
		INIT,
		READY,
		BUSY
	}
	
	// EMCAN Stream Op Codes
	private static final byte OP_NOP		= 0x00;		//
	private static final byte OP_PING		= 0x01;		//
	private static final byte OP_ID			= 0x02;		//
	private static final byte OP_FWINFO		= 0x03;		//
	private static final byte OP_CMDS		= 0x04;		//
	private static final byte OP_RESET		= 0x05;		//
	private static final byte OP_SENDS		= 0x06;		//
	private static final byte OP_SENDE		= 0X07;		//
	private static final byte OP_SENDSR		= 0x08;		//
	private static final byte OP_SENDER		= 0x09;		//
	private static final byte OP_ENUM		= 0X0A;		//
	private static final byte OP_KEEPALIVE	= 0x0B;		//
	private static final byte OP_STROUT		= 0x0C;		//
	
	
	// EMCAN Stream Response Codes
	private static final byte RS_NOP		= 0x00;		//
	private static final byte RS_PING		= 0x01;		//
	private static final byte RS_ID			= 0x02;		//
	private static final byte RS_FWINFO		= 0x03;		//
	private static final byte RS_CMDS		= 0x04;		//
	private static final byte RS_RESET		= 0x05;		//
	private static final byte RS_SENDS		= 0x06;		//
	private static final byte RS_SENDE		= 0X07;		//
	private static final byte RS_SENDSR		= 0x08;		//
	private static final byte RS_SENDER		= 0x09;		//
	private static final byte RS_ENUM		= 0X0A;		//
	private static final byte RS_KEEPALIVE	= 0x0B;		//
	private static final byte RS_STROUT		= 0x0C;		//

	
	/**
	 * Constructor: Opens stream and starts transmit thread
	 * 
	 * @param sock - socket of target server.
	 */
	public CommsThread(Socket sock) {

		mSocket = sock;
		InputStream tmpIn = null;
		OutputStream tmpOut = null;

		this.setName("SrvrMsgRcv");
		
		// create input and output stream objects to read and write socket.
		try {
			tmpIn = mSocket.getInputStream();
			tmpOut = mSocket.getOutputStream();
		}
		catch (IOException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
		inputStream = tmpIn;
		outputStream = tmpOut;
		
		// Start the thread to send to the server
		mSrvrSndThread = new HandlerThread("SrvrMsgSnd", Process.THREAD_PRIORITY_BACKGROUND);
		mSrvrSndThread.start();
		mSrvrSndLooper = mSrvrSndThread.getLooper();
		mSndToSrvrHandler = new SndToSrvr(mSrvrSndLooper);

	}
	

	/**
	 * Server Receive Thread: Parses packet from server and generates an
	 * event to send to the client.
	 */
	public void run() {
		
		byte[]	rcvBuf = new byte[1024];		// buffer store for the stream
		int bytes = 0;		                    // number of bytes returned from read()
//		byte[] packet;
			
		while(true) {
			//Make a blocking call to read input stream.
			try {
				bytes = inputStream.read(rcvBuf);				
			}
			catch (IOException e) {
				Log.d(TAG, e.getLocalizedMessage());
			}

			
			// extract only the actual bytes received
//			packet = java.util.Arrays.copyOfRange(rcvBuf, 0, bytes);
			

			// If logging is enabled, display the text received
//			if (L) Log.i("SrvrMsgRcv", bytesToHex(packet));
			if (L) Log.i("SrvrMsgRcv", String.valueOf(rcvBuf[0]));
			
			//TODO Make state machine to process buffer.
			
			// Dispatch response received
//			switch (packet[0]) {
			switch (rcvBuf[0]) {
			
			// Process ID response.
			case RS_ID:
				// TODO On connect event, (state = connect, rspns = ID) -
				//      send message handler to service, change server state to ready.

				// Send "Connect" event to Morbus service. Attach the server's message handler.
				Message msg = MbusService.mMsgFromSrvr.obtainMessage(MbusService.EVT_CONNECT, 0, 0);
	    		msg.replyTo  = new Messenger(mSndToSrvrHandler);
                MbusService.mMsgFromSrvr.sendMessage(msg);
				break;
				
			// Process remaining responses.
			default:
				break;
				
			}	// end switch	
		}	// end while
	}

	/**
	 *  Server Send Thread: Parses command from client and generates a
	 *  packet to send to the server.
	 *  
	 *  @param msg - Message from Main containing an MBus event.
	 */
	private final class SndToSrvr extends Handler {
		
		private byte sndBuf[];
		
		// Constructor
		public SndToSrvr(Looper looper) {
			super(looper);
		}
		
		// Message handler
		@Override
		public void handleMessage(Message msg) {
			if (L) Log.i("SrvrMsgSnd", "Message type " + msg.what);

			// Dispatch this service command
			switch (msg.what) {
				case MbusService.CMD_POWER_IS:
					break;
				
				case MbusService.CMD_THTL_STEP:
					if (L) Log.i("SrvrMsgSnd", "tID=" + msg.arg1 + "speed=" +msg.arg2);
					
					DCCencoder Unit = (DCCencoder) msg.obj;
					
					sndBuf = Unit.DCCspeed(msg.arg2);
					
					write(sndBuf);
					
					break;
					
				case MbusService.CMD_KEEP_ALIVE:
					if (L) Log.i("SrvrMsgSnd", "Sending Keep_alive");
					
					sndBuf = new byte[1];
					sndBuf[0] = OP_KEEPALIVE;
					write(sndBuf);
					break;
					
				default:
					Log.d("SrvrMsgSnd", "Unknown MBus event type " + msg.what);
					super.handleMessage(msg);
			}
			
		}
	}

	/**
	 * CommsThread Write Method
	 * Write a string of bytes to the output stream.
	 * @param bytes
	 */
	public void write(byte[] bytes)
	{
		try {
			outputStream.write(bytes);
		}
		catch (IOException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
	}
	

	/**
	 * CommsThread Cancel Method
	 * Close the socket
	 */
	public void cancel()
	{
		try {
			mSocket.close();
		}
		catch (IOException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
	}

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

}
