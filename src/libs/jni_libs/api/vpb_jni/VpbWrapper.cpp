#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "com_cablelabs_vpbapi_VpbAPI.h"
#include "vpbapi.h"

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    open_card
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_open_1card
(JNIEnv *env, jobject obj, jint port) {
	vpb_seterrormode(VPB_EXCEPTION);
	
	int channel = vpb_open(com_cablelabs_vpbapi_VpbAPI_BOARD, port);

	return channel;
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    close_card
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_cablelabs_vpbapi_VpbAPI_close_1card
(JNIEnv *env, jobject obj, jint channel) {

   vpb_close((int)channel);

}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    onhook_channel
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_onhook_1channel
(JNIEnv *env, jobject obj, jint channel) {
	return vpb_sethook_sync(channel, VPB_ONHOOK);
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    offhook_channel
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_offhook_1channel
(JNIEnv *env, jobject obj, jint channel) {
	return vpb_sethook_sync(channel, VPB_OFFHOOK);
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    dial_channel
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_dial_1channel
(JNIEnv *env, jobject obj, jint channel, jstring digits) {
	jint result = 0;
	const char *str = env->GetStringUTFChars(digits, NULL);
	if (str == NULL)
		return -1;

	result = vpb_dial_async(channel, (char *)str);

	env->ReleaseStringUTFChars(digits, str);
	
	return result;
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    hookflash_channel
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_hookflash_1channel
(JNIEnv *env, jobject obj, jint channel) {
		
	return vpb_dial_async(channel, "&");
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    check_dialtone
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_com_cablelabs_vpbapi_VpbAPI_check_1dialtone
(JNIEnv *env, jobject obj, jint channel, jint timeout) {
	VPB_EVENT event;
	char dialStr[64];
	char str[256];
	jboolean dialtone = false;
	int pause = 500;
	int timeRemaining = timeout;

	sprintf(dialStr, "[0%i] Tone Detect: Dial\n", channel);
	// The loop needs to continue through the entire time to
	// flush the queue of any events.
	// NOTE VPB_OK is defined as the value 0
	while (timeRemaining > 0) {
		int status = vpb_get_event_ch_async(channel, &event);
	    if (status == 0)  {
            vpb_translate_event(&event, str);
		    fprintf(stdout, "Event str = %s\n", str);
			if (strcmp(str, dialStr) == 0) {
		      	dialtone = true;
			}
		}
		else {
			fprintf(stdout, "vpb_get_event_ch_async returned %d\n", status);
		}
		timeRemaining -= pause;
		vpb_sleep(pause);
	}
	return dialtone;
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    getEvent
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_cablelabs_vpbapi_VpbAPI_getEvent
(JNIEnv *env, jobject obj, jint timeout) {
	VPB_EVENT event;
	char str[256];

	int result = vpb_get_event_sync(&event, (int)timeout);
	if (result == VPB_TIME_OUT)
		return;
	else {
		vpb_translate_event(&event, str);
		jclass cls = env->GetObjectClass(obj); 
		jmethodID mid = 
        env->GetMethodID(cls, "createEvent", "(IIIJLjava/lang/String;)V"); 
		if (mid == NULL) {
			return; /* method not found */ 
		} 
   
		jstring eventStr = env->NewStringUTF(str);
		env->CallVoidMethod(obj, mid, (jint)event.type, 
			(jint)event.handle, (jint)event.data, (jlong)event.data1,
			(jstring)eventStr); 
	}
	return;
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    getEvent
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_cablelabs_vpbapi_VpbAPI_detectTone
(JNIEnv *env, jobject obj, jint channel, jint id) {
	VPB_DETECT tone;
	//char str[256];

	int result = vpb_gettonedet(channel, id, &tone);
	if (result != 0) {
		fprintf(stdout, "vpb_gettonedet returned %d\n", result);
		return;
	}
	else {
		//vpb_translate_event(&event, str);
		jclass cls = env->GetObjectClass(obj); 
		jmethodID mid = 
        env->GetMethodID(cls, "recvTone", "(IIIIIIISSSSI)V"); 
		if (mid == NULL) {
			return; /* method not found */ 
		} 
   
		//jstring eventStr = env->NewStringUTF(str);
		env->CallVoidMethod(obj, mid, (jint)tone.nstates, 
			(jint)tone.tone_id, (jint)tone.ntones, 
			(jint)tone.freq1, (jint)tone.bandwidth1,
			(jint)tone.freq2, (jint)tone.bandwidth2,
			(jshort)tone.minlevel1, (jshort)tone.minlevel2,
			(jshort)tone.twist, (jshort)tone.snr,
			(jint)tone.glitch); 
	}
	return;
}


/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    setRingback
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_setRingback
(JNIEnv *env, jobject obj, jint channel) {
	int result;
	VPB_DETECT ringback = { 
		2, // number of cadence states
		VPB_RINGBACK, // tone_id
		2, // number of tones
		440, // freq1
		200, // bandwidth1
		485, // freq2
		200, // bandwidth2
		-40, // level1
		-40, // level2
		20, // twist
		10, // SNR
		40, // glitchs
		VPB_RISING, // state 0
		0,
		0,
		0,
		VPB_FALLING, // state 1
		0,
		1800, // ton min
		2100, // ton max
	};
	

	result = vpb_settonedet(channel, &ringback);
	
	return result;
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    setReorder
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_setReorder
(JNIEnv *env, jobject obj, jint channel) {
	int result;
	VPB_DETECT reorder = { 
		3, // number of cadence states
	    4, // VPB_REORDER, // tone_id
		2, // number of tones
		480, // freq1
		100, // bandwidth1
		620, // freq2
		100, // bandwidth2
		-20, // level1
		-20, // level2
		10, // twist
		10, // SNR
		40, // glitchs
		VPB_RISING, // state 0
		0,
		0,
		0,
		VPB_FALLING, // state 1
		0,
		200, // ton min
		300, // ton max
		VPB_RISING, // state 0
		0,
		200,
		300,
	};
	

	result = vpb_settonedet(channel, &reorder);
	
	return result;
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    setCallWaiting
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_setCallWaiting
(JNIEnv *env, jobject obj, jint channel) {
	int result;
	VPB_DETECT callwaiting = { 
		2, // number of cadence states
		5, // VPB_CWTONE, // tone_id
		1, // number of tones
		440, // freq1
		100, // bandwidth1
		0, // freq2
		0, // bandwidth2
		-20, // level1
	    0, // level2
		10, // twist
		10, // SNR
		40, // glitchs
		VPB_RISING, // state 0
		0,
		0,
		0,
		VPB_FALLING, // state 1
		0,
		250, // ton min
		350, // ton max
	};
	

	result = vpb_settonedet(channel, &callwaiting);
	
	return result;
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    setDialTone
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_setDialTone
(JNIEnv *env, jobject obj, jint channel) {
	int result;
	VPB_DETECT dialtone = { 
		2, // number of cadence states
		VPB_DIAL, // tone_id
		1, // number of tones
		440, // freq1
		100, // bandwidth1
		0, // freq2
		0, // bandwidth2
		-3, // level1
	    0, // level2
		10, // twist
		10, // SNR
		40, // glitchs
		VPB_RISING, // state 0
		0,
		0,
		0,
		VPB_TIMER, // state 1
		2000,
		0, // ton min
		0, // ton max
	};
	

	result = vpb_settonedet(channel, &dialtone);
	
	return result;
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    setBusyTone
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_setBusyTone
(JNIEnv *env, jobject obj, jint channel) {
	int result;
	VPB_DETECT busytone = { 
		2, // number of cadence states
		VPB_BUSY, // tone_id
		1, // number of tones
		440, // freq1
		100, // bandwidth1
		0, // freq2
		0, // bandwidth2
		-3, // level1
	    0, // level2
		10, // twist
		10, // SNR
		40, // glitchs
		VPB_RISING, // state 0
		0,
		0,
		0,
		VPB_TIMER, // state 1
		2000,
		0, // ton min
		0, // ton max
	};
	

//	result = vpb_settonedet(channel, &dialtone);
	
//	return result;
	return false;
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    setFlash
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_setFlash
(JNIEnv *env, jobject obj, jint timems) {
	int result = 0;
	result = vpb_set_flash((int)timems);
	return result;
}

/*
 * Class:     com_cablelabs_vpbapi_VpbAPI
 * Method:    playFile
 * Signature: (ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_cablelabs_vpbapi_VpbAPI_playFile
(JNIEnv *env, jobject obj, jint channel, jstring filename) {
	jint result = 0;
	const char *str = env->GetStringUTFChars(filename, NULL);
	if (str == NULL)
		return -1;

	result = vpb_play_file_async(channel, (char *)str, VPB_PLAYEND);

	env->ReleaseStringUTFChars(filename, str);
	
	return result;
}