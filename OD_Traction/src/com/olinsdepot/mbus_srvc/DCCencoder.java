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

