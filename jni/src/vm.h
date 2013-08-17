#ifndef _VM_H
#define _VM_H

#include "emulator.h"

#define VM_NATIVE_THREAD 0

//extern int vm_is_running;


typedef enum {
	VMMSG_ID_MRTIMER = 1001,
	VMMSG_ID_LOAD_MRP,
	VMMSG_ID_PAUSE,
	VMMSG_ID_RESUME,
	VMMSG_ID_EVENT,
	VMMSG_ID_STOP,
	VMMSG_ID_START,
	VMMSG_ID_GETHOST,//网络异步回调

	VMMSG_ID_MAX
}E_VMMSG_ID;

typedef struct {
	E_VMMSG_ID id;
	int p0, p1, p2;
	void* data;
}T_VMMSG_BODY, *PT_VMMSG_BODY;


//---------------------------------------


//---------------------------------------
#define VM_SEND_MSG(_msg_) \
	msgbox_post(gs_pVMMsgBox, _msg_)

#define VM_NEWMSG_ADN_SEND(_id_, _p0_, _p1_, _p2_, _data_) \
	do { \
		PT_VMMSG_BODY msg = malloc(sizeof(T_VMMSG_BODY)); \
		msg->id = _id_; \
		msg->p0 = _p0_; \
		msg->p1 = _p1_; \
		msg->p2 = _p2_; \
		msg->data = _data_; \
		msgbox_post(gs_pVMMsgBox, msg); \
	}while(0)


inline void vm_sendMsg_ex(E_VMMSG_ID id, int p0, int p1, int p2, void *data);

//---------------------------------------
jint vm_loadMrp(JNIEnv * env, jobject self, jstring path);
jint vm_loadMrp_thread(JNIEnv * env, jobject self, jstring path);
void vm_pause(JNIEnv * env, jobject self);
void vm_resume(JNIEnv * env, jobject self);
void vm_exit(JNIEnv * env, jobject self);
void vm_backrun(JNIEnv * env, jobject self);
void vm_timeOut(JNIEnv * env, jobject self);
void vm_event(JNIEnv * env, jobject self, jint code, jint p0, jint p1);
int vm_smsIndiaction(JNIEnv * env, jobject self, jstring pContent, jstring pNum);

jint vm_newSIMInd(JNIEnv * env, jobject self,
		jint type, jbyteArray old_IMSI);

jint vm_registerAPP(JNIEnv * env, jobject self,
		jbyteArray p, jint len, jint index);

/*
 * 强制退出 native 启动的 thread
 */
void vm_exit_foce(JNIEnv * env, jobject self);

#endif
