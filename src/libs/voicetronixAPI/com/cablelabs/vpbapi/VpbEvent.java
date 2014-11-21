/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.vpbapi;

public class VpbEvent {

	 VpbEventType type = null;
	 
	 public int channel = 0;
	 public int data = 0;
	 public long data1 = 0;
//	     typedef struct {
//		    int   type;       // event type (see below)
//		    int   handle;           // channel that generated event
//		    int   data;       // optional data
//		    unsigned long data1;
//		 } VPB_EVENT;
//		  
//		  // unsolicited events (maskable)
//		  
//		 #define VPB_RING              0
//		 #define VPB_DIGIT             1
//		 #define     VPB_TONEDETECT          2
//		 #define     VPB_TIMEREXP            3
//		 #define     VPB_VOXON               4
//		 #define     VPB_VOXOFF              5
//		 #define     VPB_PLAY_UNDERFLOW      6
//		 #define     VPB_RECORD_OVERFLOW     7
//		 #define VPB_DTMF        8
//		  
//		  // solicited events (not maskable)
//		  
//		 #define     VPB_PLAYEND             100
//		 #define     VPB_RECORDEND           101
//		 #define     VPB_DIALEND             102
//		 #define     VPB_TONE_DEBUG_END      103
//		 #define     VPB_CALLEND             104
//	 #define     VPB_DIAL          0           // dial tone detected
//	 #define     VPB_RINGBACK      1           // ringback detected
//	 #define     VPB_BUSY          2           // busy tone detected
//	 #define     VPB_GRUNT         3           // grunt detected


	 public VpbEvent() {
		 
	 }
	 
	 public VpbEvent(int type, int handle, int data, long data1) throws IllegalArgumentException {
		 switch (type) {
		 case 0:
			 this.type = VpbEventType.RINGING;
			 this.channel  = handle;
			 this.data = data;
			 this.data1 = data1;
			 break;
		 case 2:
			 switch (data) {
			 case 0:
				 this.type = VpbEventType.DIALTONE;
				 this.channel  = handle;
				 this.data = data;
				 this.data1 = data1;
				 break;
			 case 1:
				 this.type = VpbEventType.RINGBACK;
				 this.channel  = handle;
				 this.data = data;
				 this.data1 = data1;
				 break;
			 case 2:
				 this.type = VpbEventType.BUSY;
				 this.channel  = handle;
				 this.data = data;
				 this.data1 = data1;
				 break;
			 case 4:
				 this.type = VpbEventType.REORDER;
				 this.channel  = handle;
				 this.data = data;
				 this.data1 = data1;
				 break;
			 case 5:
				 this.type = VpbEventType.CALL_WAITING_TONE;
				 this.channel  = handle;
				 this.data = data;
				 this.data1 = data1;
				 break;
			 default:
				 this.type = VpbEventType.UNKNOWN;

			 }

			 break;
		 case 100:
			 this.type = VpbEventType.VOICE_DETECTED;
			 this.channel  = handle;
			 this.data = data;
			 this.data1 = data1;
			 break;
		 
		 case 102:
			 this.type = VpbEventType.DIAL;
			 this.channel  = handle;
			 this.data = data;
			 this.data1 = data1;
			 break;
		 default:
			 this.type = VpbEventType.UNKNOWN;
		 break;
		 }
	 }
}
