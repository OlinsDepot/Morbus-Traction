package com.olinsdepot.od_traction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import android.util.Log;

public class CommsThread extends Thread {
	private final String TAG = getClass().getSimpleName();

	private final Socket mSocket;
	private final InputStream inputStream;
	private final OutputStream outputStream;
	
	/**
	 * Constructor for TCP Stream Object
	 * @param sock - socket of target server.
	 */
	public CommsThread(Socket sock) {

		mSocket = sock;
		InputStream tmpIn = null;
		OutputStream tmpOut = null;

		this.setName("CommsThread");
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
	}
	

	/**
	 * Main task loop
	 * Listen on socket for incoming messages
	 */
	public void run() {
		
		byte[]	buffer = new byte[1024];		// buffer store for the stream
		int bytes;		                        // number of bytes returned from read()
			
		while(true)
		{
			//Make a blocking call to read input stream and notify MORBUS stack on return.
			try {
				bytes = inputStream.read(buffer);				
				MbusService.srvrMsgRcv.obtainMessage(0, bytes, -1, buffer).sendToTarget();
			}
			catch (IOException e) {
				Log.d(TAG, e.getLocalizedMessage());
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
	
}
