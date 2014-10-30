package com.olinsdepot.mbus_srvc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;

import android.util.Log;

public class CommsThread extends Thread {
	private final String TAG = getClass().getSimpleName();
	private static final boolean L = true;

	// Network connection
	private final Socket mSocket;
	private final InputStream inputStream;
	private final OutputStream outputStream;
	
	// Thread to send to server
	private static HandlerThread mCommsSendThread;
	private static Looper mCommsSendLooper;
	private static Handler mCommsSendHandler;

	// Server state shared with the send and receive threads
	private enum SrvStates {
		INIT,	// Not connected to server yet
		READY,	// Connected, accepting commands
		BUSY	// Connected, waiting for a command to complete
	}

	private  class SrvrState {
		
		private SrvStates myState;
		
		SrvStates get() {
			return myState;
		}
		
		SrvStates set(SrvStates state) {
			myState = state;
			return myState;
		}
	}
	SrvrState mSrvrState = new SrvrState();
	
	// Commands into Comms Thread
	
	// Events out of Comms Thread
	public static final int EVT_START 		= 1;
	public static final int EVT_STOP 		= 2;
	public static final int EVT_CONNECT		= 3;
	
	
	// EMCAN Stream Op Codes
	private static enum EmCAN {
		NOP 		(0),
		PING 		(1),
		ID 			(2),
		FWINFO		(3),
		CMDS		(4),
		RESET		(5),
		SENDS		(6),
		SENDE		(7),
		SENDSR		(8),
		SENDER		(9),
		ENUM		(10),
		KEEPALIVE	(11),
		STROUT		(12);
		
		private final int opcode;		
		EmCAN(int op) {
			this.opcode = op;
		}
		
		public byte op() {return (byte) opcode;}
	}
	
	/**
	 * Constructor: Opens stream and starts transmit thread
	 * 
	 * @param sock - socket of target server.
	 */
	public CommsThread(Socket sock) {
		
		// Init server state
		mSrvrState.set(SrvStates.INIT);

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
		mCommsSendThread = new HandlerThread("SrvrMsgSnd", Process.THREAD_PRIORITY_BACKGROUND);
		mCommsSendThread.start();
		mCommsSendLooper = mCommsSendThread.getLooper();
		mCommsSendHandler = new CommsSendHandler(mCommsSendLooper);

		// Send "START" event to Morbus service with the Comm thread's message handler.
		Message msg = MbusService.mCommsToSrvcHandler.obtainMessage(EVT_START, 0, 0);
		msg.replyTo  = new Messenger(mCommsSendHandler);
        MbusService.mCommsToSrvcHandler.sendMessage(msg);
	}
	

	/**
	 * Comms Receive Thread: Parses packet from server and generates an
	 * event to send to the client.
	 */
	public void run() {
		String TAG = "CommsRcv";
		
		// EMCAN Stream Response Codes
		final int  EOF		= -1;		//
		final byte NOP		= 0;		//
		final byte PONG		= 1;		//
		final byte ID		= 2;		//
		final byte FWINFO	= 3;		//
		final byte CMDS		= 4;		//
		final byte CANFR	= 5;		//
		final byte RESET	= 6;		//
		final byte ADR		= 7;		//
		final byte UNADR	= 8;		//
		final byte STROUT	= 9;		//
		final byte STRINRES	= 10;		//
		final byte STRIN	= 11;		//

		final byte[] PROT_NAME = {'E','m','C','a','n',':','M','o','r','B','u','s'};
		int protVersion = 0;

		byte rspCode = EOF;					// read expected response code.
		byte[] rcvBuf = new byte[1024];		// buffer store for the stream
		int bytes = 0;		                // number of bytes returned from read
		
		while(true) {
			//Make a blocking call to read response code from input stream.
			try {
				rspCode = (byte)inputStream.read();				
			}
			catch (IOException e) {
				Log.d(TAG, e.getLocalizedMessage());
				//TODO Send message to service that read on socket failed.
			}			

			// Dispatch received response code
			switch (rspCode) {
			
			case EOF:
				if (L) Log.i(TAG, "Reached EOF");
				break;
			
			case NOP:
				if (L) Log.i(TAG, "Received NOP");
				break;
			
			case PONG:
				if (L) Log.i(TAG, "Received PONG");
				// If server was BUSY, receiving PONG indicates it's now READY
				if (mSrvrState.get() == SrvStates.BUSY) {
					mSrvrState.set(SrvStates.READY);
				}
				break;
			
			case ID:
				if (L) Log.i(TAG, "Received ID");
				// Process this response if the server is not already connected, else - ignore it.
				if (mSrvrState.get() == SrvStates.INIT) {
					
					//Read rest of the ID response...
					for (bytes = 0; bytes < 1024; bytes++) {
						try {
							rcvBuf[bytes] = (byte)inputStream.read();
						} catch (IOException e) {
							Log.d(TAG, e.getLocalizedMessage());
						}
						if (rcvBuf[bytes] == 0) break;
					}
					
					//  ...and verify isequal EmCan:Morbus
					byte[] rcvdId = Arrays.copyOf(rcvBuf,bytes);
					if (Arrays.equals(rcvdId, PROT_NAME)) {
							
						//Read the protocol version.
						try {
							protVersion = inputStream.read();
						} catch (IOException e) {
							Log.d(TAG, e.getLocalizedMessage());
						}
	
						// Send "Connect" event to Morbus service. Attach the server's message handler.
						Message msg = MbusService.mCommsToSrvcHandler.obtainMessage(EVT_CONNECT);
						msg.arg1 = 0; //Event is success
						msg.arg2 = protVersion; // Report protocol version.
		                MbusService.mCommsToSrvcHandler.sendMessage(msg);
		                
		                // Update server state to "Connected, ready for commands"
			                mSrvrState.set(SrvStates.READY);
					}
				}
				break;
			
			case FWINFO:
				if (L) Log.i(TAG, "Received FWINFO");
				break;
			
			case CMDS:
				if (L) Log.i(TAG, "Received CMDS");
				break;
			
			case CANFR:
				if (L) Log.i(TAG, "Received CANFR");
				break;
			
			case RESET:
				if (L) Log.i(TAG, "Received RESET");
				break;
			
			case ADR:
				if (L) Log.i(TAG, "Received ADR");
				break;
			
			case UNADR:
				if (L) Log.i(TAG, "Received UNADR");
				break;
			
			case STROUT:
				if (L) Log.i(TAG, "Received STROUT");
				break;
			
			case STRINRES:
				if (L) Log.i(TAG, "Received STRINRES");
				break;

			case STRIN:
				if (L) Log.i(TAG, "Received STRIN");
				break;

			default:
				Log.d(TAG,"Unknown EMCan response");
				break;
				
			}	// end switch	
		}	// end while
	}

	/**
	 *  Comms Send Thread: Parses command from service and generates a
	 *  packet to send to the server.
	 *  
	 *  @param msg - Message from Main containing an MBus event.
	 */
	private final class CommsSendHandler extends Handler {
		
		private byte sndBuf[];
		
		// Constructor
		public CommsSendHandler(Looper looper) {
			super(looper);
		}
		
		// Message handler
		@Override
		public void handleMessage(Message msg) {
			if (L) Log.i("SrvrMsgSnd", "Message type " + msg.what);
			
			/*Spin waiting for server state to be READY.
			while (mSrvrState.get() != SrvStates.READY) {
				try {
					mSrvrState.wait();
				} catch (InterruptedException e) {
					Log.d(TAG, e.getLocalizedMessage());
				}			

			}
			*/
			
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
					sndBuf[0] = EmCAN.KEEPALIVE.op();
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
	 * 
	 * @param bytes - byte array to send
	 */
	private void write(byte[] bytes)
	{
		try {
			outputStream.write(bytes);
		}
		catch (IOException e) {
			Log.d(TAG, e.getLocalizedMessage());
			//TODO Send message to service that write on socket failed.
		}
	}


	/**
	 * CommsThread Cancel Method
	 * Close the socket
	 */
	private void cancel()
	{
		try {
			mSocket.close();
		}
		catch (IOException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
	}

	
	/**
	 * Bytes to Hex String: Utility function
	 * @param Byte array
	 */
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
