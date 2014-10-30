package com.olinsdepot.mbus_srvc;

import android.os.Bundle;

/**
 * The DCC encoder object is created with the characteristics, (address etc.)
 * of a specific decoder. It's methods translate general commands to set speed
 * or functions into DCC command strings specific to this decoder.
 * 
 * @author mhughes
 *
 */
public class DCCencoder {
	
	// Decoder Parameters
	private byte[] dcdrAdr;
	private int spdFmt;
	private byte[] funK;
	
	// Decoder Instructions
	private static enum DccInst {
		DCD_CTL		(0x00),
		ADV_OPS		(0x20),
		RVS_SPD		(0x40),
		FWD_SPD		(0x60),
		F_GRP_1		(0x80),
		F_GRP_2		(0xA0),
		FTR_EXP		(0xC0),
		CV_ACCS		(0xE0);
		
		private final int instruction;		

		DccInst (int theInstruction) {
			this.instruction = theInstruction;
		}
		
		public byte op() {return (byte) this.instruction;}
		
	}
	
	/**
	 * Constructor sets decoder characteristics
	 * @param newDcdr
	 */
	DCCencoder (Bundle newDcdr) {
		// Unpack this decoder's address
		boolean adrTypX = newDcdr.getBoolean("ADR_TYP_X");
		int theAddr = newDcdr.getInt("DCDR_ADR");
		if (adrTypX) {
			this.dcdrAdr = new byte[2];
			this.dcdrAdr[0] = (byte)(0xC0 | ((theAddr >> 8) & 0x3F));
			this.dcdrAdr[1] = (byte)(theAddr & 0xFF);
		} else {
			this.dcdrAdr = new byte[1];
			this.dcdrAdr[0] = (byte)(theAddr & 0x7F);
		}
		
		// Save speed step format
		this.spdFmt = newDcdr.getInt("SPD_FMT");
		
		// Get function key status.
		this.funK = newDcdr.getByteArray("FUNC_KEY");
	}
	
	
	/**
	 * Encode reset command
	 * @return DCC byte string to reset this decoder.
	 */
	public byte[] DCCreset () {
		//TODO implement reset method
		byte[] encoderBuf = null;
		
		return encoderBuf;
		
	}
	
	/**
	 * Encode speed/dir command
	 * @param speed
	 * @return DCC byte string to set speed step this decoder.
	 */
	byte[] DCCspeed (int speed) {
		
		byte[] encoderBuf = null;
		int throttleStep;
		int indx = this.dcdrAdr.length;
		//TODO 14 bit addressing.
		//TODO encode speed of zero as normal Stop.
		switch (this.spdFmt) {

		case 0:
			//TODO implement 14 step format
			//Setup buffer for a 1 byte instruction
			encoderBuf = new byte[indx + 1];
			System.arraycopy(this.dcdrAdr, 0, encoderBuf, 0, indx);
			
			// Set direction and throttle step for 14 step decoder
			break;
			
		case 1:
			//TODO implment 28 step format
			//Setup buffer for a 1 byte instruction
			encoderBuf = new byte[indx + 1];
			System.arraycopy(this.dcdrAdr, 0, encoderBuf, 0, indx);
			
			//Set direction and throttle step for 28 step decoder
			break;
			
		case 2:
			//Setup buffer for a 2 byte instruction
			encoderBuf = new byte[indx + 2];
			System.arraycopy(this.dcdrAdr, 0, encoderBuf, 0, indx);
			
			// Set direction and throttle step for 126 step decoder.
			if (speed < 0) throttleStep = (-speed & 0x7F);
			else if (speed > 0) throttleStep = (0x80 | (speed & 0x7F));
			else throttleStep = 0;
//			encoderBuf[0] = (byte) 0x07;
//			encoderBuf[1] = (byte) 0x04;
//			encoderBuf[2] = (byte) 0x00;
//			encoderBuf[3] = (byte) 0x19;
//			encoderBuf[4] = (byte) 0x00;
//			encoderBuf[5] = (byte) 0x00;
//			encoderBuf[6] = (byte) 0x80;
//			encoderBuf[7] = this.dcdrAdr;
//			encoderBuf[0] = (byte) 0x3F;  //extended command + speedStep
			encoderBuf[indx] = (byte)(DccInst.FTR_EXP.op() | 0x0F); //extended command + speedstep sub command
			encoderBuf[indx + 1] = (byte)throttleStep;
			break;
		}
		
		return encoderBuf;
		
	}
	
	/**
	 * Encode eStop command
	 * @return DCC byte string for Emergency Stop for this decoder.
	 */
	byte[] DCCestop () {
		byte[] encoderBuf = null;
		
		switch (this.spdFmt) {
		
		case 0:
			//TODO implement estop for 14 step format
			break;
			
		case 1:
			//TODO implement estop for 28 step format
			break;
			
		case 2:
			//TODO implement estop for 126 step format
			break;
		}
		//TODO encode emergency stop command
		return encoderBuf;
	}
		
	/**
	 * Encode Function command
	 * @param funcKey
	 * @return Byte string for function command for this decoder.
	 */
	byte[] DCCfunc (int funcKey) {
		//TODO implement functions
		byte[] encoderBuf = null;
		
		if (funcKey <= 4) {
			//FL and F1-F4
		} else if (funcKey <= 12) {
			//F5 through F12
		} else if (funcKey <= 20){
			//F13 through F20
		} else if (funcKey <= 28) {
			//F21 through F28
		} else {
			//throw an invalid format error.
		}
		
		return encoderBuf;
	}
}

