package com.olinsdepot.mbus_srvc;

public class DCCencoder {
	private byte dcdrAdr;
	private byte encoderBuf[];
	
	//Constructor sets decoder address
	DCCencoder (int newDcdr) {
		this.dcdrAdr = (byte) (newDcdr & (0xff));
	}
	
	//Method: encode speed/dir command
	byte [] DCCspeed (int speed) {
		
		//TODO implement encoders for 16 and 28 speed step decoders
		
		// Set direction and throttle step.
		byte throttleStep;
		if (speed < 0) throttleStep = (byte)(-speed & 0x7F);
		else if (speed > 0) throttleStep = (byte)(0x80 | (speed & 0x7F));
		else throttleStep = 0;
		
		encoderBuf = new byte[10];
		encoderBuf[0] = (byte) 0x07;
		encoderBuf[1] = (byte) 0x04;
		encoderBuf[2] = (byte) 0x00;
		encoderBuf[3] = (byte) 0x19;
		encoderBuf[4] = (byte) 0x00;
		encoderBuf[5] = (byte) 0x00;
		encoderBuf[6] = (byte) 0x80;
		encoderBuf[7] = this.dcdrAdr;
		encoderBuf[8] = (byte) 0x3F;  //extended command + speedStep
		encoderBuf[9] = throttleStep;
		
		return encoderBuf;
		
	}
}

