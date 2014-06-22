package com.olinsdepot.od_traction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;


public class MorbusStack extends Thread {
	
	/**
	 * String for logging the class name
	 */
	private final String TAG = getClass().getSimpleName();
	
	/**
	 * Net - IP address of Morbus Server
	 */
	private final Socket mbusSocket;
	private final InputStream mbusInput;
	private final OutputStream mbusOutput;
	
	/**
	 * Constructor for Morbus Stack Object
	 * @param mServerAdr - Address of server to connect to.
	 * @param mServerPort - Port to connect on.
	 */
	public MorbusStack(Socket argSocket) {

		mbusSocket = argSocket;
		InputStream tmpIn = null;
		OutputStream tmpOut = null;

		// create input and output stream objects to read and write socket.
		try {
			tmpIn = mbusSocket.getInputStream();
			tmpOut = mbusSocket.getOutputStream();
		}
		catch (IOException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
		mbusInput = tmpIn;
		mbusOutput = tmpOut;
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
			//Make a blocking call to read input stream and notify MAIN on return.
			try {
				bytes = mbusInput.read(buffer);				
//				MainActivity.mbusMsgIn.obtainMessage(0, bytes, -1, buffer).sendToTarget();
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
			mbusOutput.write(bytes);
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
			mbusSocket.close();
		}
		catch (IOException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
	}
	
}
