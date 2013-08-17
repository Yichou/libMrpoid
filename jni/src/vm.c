#include "vm.h"

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <errno.h>


//int vm_is_running = 0;

static char 		runMrpPath[DSM_MAX_FILE_LEN + 1];
static pmsg_box_t	gs_pVMMsgBox;
static int 			b_native_thread = FALSE;
static pthread_t 	native_therad_id = 0;

//static const int 	BUF_SIZE = 128;
//static msg_block_t block_buf[BUF_SIZE];
//static T_VMMSG_BODY body_buf[BUF_SIZE];
//static int curIndex;


static void mainLoop();

static void delMsg(pmsg_block_t pMsg)
{
	free(pMsg->msg);
	free(pMsg);
}

inline
void vm_sendMsg_ex(E_VMMSG_ID id, int p0, int p1, int p2, void *data)
{
	msg_block_t *block = malloc(sizeof (msg_block_t));
	T_VMMSG_BODY *body = malloc(sizeof (T_VMMSG_BODY));

//	if(curIndex > BUF_SIZE)
//		curIndex = 0;
//
//	msg_block_t *block = &block_buf[curIndex];
//	T_VMMSG_BODY *body = &body_buf[curIndex];

	body->id = id;
	body->p0 = p0;
	body->p1 = p1;
	body->p2 = p2;
	body->data = data;

	block->msg = (void *)body;
	msgbox_post(gs_pVMMsgBox, block);
}

//加载 MRP
jint vm_loadMrp(JNIEnv * env, jobject self, jstring path)
{
	jniEnv = env;

	const char *str = (*env)->GetStringUTFChars(env, path, JNI_FALSE);
	if(str){
//		char buf[DSM_MAX_FILE_LEN + 1] = {0};
//
//		sprintf(buf, "%%%s", str[0]=='/'? str+1 : str);

		LOGD("vm_loadMrp entry:%s", str);
		UTF8ToGBString(str, runMrpPath, sizeof(runMrpPath));

		b_native_thread = FALSE;
		gEmulatorCfg.useLinuxTimer = FALSE; //native 线程
		gEmulatorCfg.b_nativeThread = FALSE;
		if(!gEmulatorCfg.b_tsfInited)
			gEmulatorCfg.androidDrawChar = TRUE;

//		gEmulatorCfg.enableExram = 1;

		showApiLog = 1;
		gApiLogSw.showMrPrintf = 1;

		setJniEnv(env);
//		emu_attachJniEnv();

		dsm_init();

		gEmulatorCfg.b_vm_running = 1;

#ifdef DSM_FULL
		mr_start_dsm(runMrpPath);
#else
		mr_start_dsmC("cfunction.ext", runMrpPath);
#endif

		(*env)->ReleaseStringUTFChars(env, path, str);

		return 1;
	}

	return -1;
}

//----------- native thread -------------------------------
static void native_mrp_exit()
{
	emu_detachJniEnv();

	//如果消息队列 还有消息 是不是就内存泄露了？
	msgbox_clear(gs_pVMMsgBox, delMsg);
	msgbox_delete(gs_pVMMsgBox);

	mr_stop();
	//最后执行
	mr_exit();
}

//退出 mrp 线程
static void thread_exit(int signo)
{
	if (signo == SIGKILL)
	{
		LOGI("thread_exit from SIGKILL");
		native_mrp_exit();
		//退出自己
		pthread_exit(NULL);
	}
}

static void native_mrp_run()
{
	//捕获 SIGKILL 信号，用于强制退出
	signal(SIGKILL, thread_exit);

	//获取 jni 环境
	emu_attachJniEnv();

	//创建消息处理器
	gs_pVMMsgBox = msgbox_new(128);

//	curIndex = 0;
//	memset(block_buf, 0, sizeof(block_buf));
//	memset(body_buf, 0, sizeof(body_buf));

	mr_start_dsm(runMrpPath);

	//启动主循环
	mainLoop();

	//运行到这里说明  MRP 结束了
	native_mrp_exit();
}

/**
 * 强制关闭 native 线程
 */
void vm_exit_foce(JNIEnv * env, jobject self)
{
	if (b_native_thread)
	{
		LOGD("native force exit call");
		int ret = pthread_kill(native_therad_id, SIGKILL);

		if(ret == ESRCH)
			LOGD("the specified thread did not exists or already quit\n");
		else if(ret == EINVAL)
			LOGD("signal is invalid\n");
		else
			LOGD("the specified thread is alive\n");

		pthread_join(native_therad_id, NULL); //等待 native thread 结束释放资源
	}
}

jint vm_loadMrp_thread(JNIEnv * env, jobject self, jstring path)
{
	const char *str;

	jniEnv = env;

	str = (*env)->GetStringUTFChars(env, path, JNI_FALSE);
	if (str)
	{
		char buf[DSM_MAX_FILE_LEN + 1];

		sprintf(buf, "%%%s", str[0] == '/' ? str + 1 : str);
		LOGD("vm_loadMrp_thread path:%s entry:%s", str, buf);
		UTF8ToGBString(buf, runMrpPath, DSM_MAX_FILE_LEN);

		(*env)->ReleaseStringUTFChars(env, path, str);

		b_native_thread = TRUE;
		gEmulatorCfg.useLinuxTimer = 1; //native 线程
		gEmulatorCfg.b_nativeThread = TRUE;

		int ret;
		ret = pthread_create(&native_therad_id, NULL, (void *)native_mrp_run, NULL);
		if(ret != 0){
			LOGE ("Create pthread error!");
			exit (1);
		}

		return 1;
	}

	return -1;
}

//暂停MRP
void vm_pause(JNIEnv * env, jobject self)
{
	if(gApiLogSw.showFW) LOGI("mr_pauseApp");

	if(b_native_thread)
		vm_sendMsg_ex(VMMSG_ID_PAUSE, 0,0,0,NULL);
	else
		mr_pauseApp();
}

//恢复MRP
void vm_resume(JNIEnv * env, jobject self)
{
	if(gApiLogSw.showFW) LOGI("mr_resumeApp");

	if(b_native_thread)
		vm_sendMsg_ex(VMMSG_ID_RESUME, 0,0,0,NULL);
	else
		mr_resumeApp();
}

int32 mr_exit(void)
{
	LOGD("native mr_exit() call, CALL:emu_finish()");

	dsm_reset();
	emu_finish();

	gEmulatorCfg.b_vm_running = 0;

	return MR_SUCCESS;
}

//退出MRP
void vm_exit(JNIEnv * env, jobject self)
{
	if(gApiLogSw.showFW) LOGI("mr_stop");

	LOGD("native exitMrp() call, CALL:mr_stop()");

	if(b_native_thread){
		vm_sendMsg_ex(VMMSG_ID_STOP, 0,0,0,NULL);
		pthread_join(native_therad_id, NULL); //等待 native thread 结束释放资源
	}else {
		//仅仅是通知调用 mrc_exit()
		mr_stop();

		//最后执行
		mr_exit();

		emu_detachJniEnv();
	}
}

//后台运行MRP
void vm_backrun(JNIEnv * env, jobject self)
{
}

void vm_timeOut(JNIEnv * env, jobject self)
{
	if(gApiLogSw.showTimerLog) LOGI("timeOut");

	if(b_native_thread)
		vm_sendMsg_ex(VMMSG_ID_MRTIMER, 0,0,0,NULL);
	else
		mr_timer();
}

void vm_event(JNIEnv * env, jobject self, jint code, jint p0, jint p1)
{
	if(gApiLogSw.showFW) LOGI("mr_event(%d, %d, %d)", code, p0, p1);

	if(code == MR_SMS_GET_SC){ //获取短信中心
		p0 = (jint)dsmSmsCenter; //如果 java 层实现了，应该从java层读取信息
		p1 = 0;
	}

	if(b_native_thread)
		vm_sendMsg_ex(VMMSG_ID_EVENT, code, p0, p1, NULL);
	else
		mr_event(code, p0, p1);
}

/**
 * 短信到达通知
 *
 * 2013-3-26 14:51:56
 */
jint vm_smsIndiaction(JNIEnv * env, jobject self, jstring content, jstring number)
{
	int32 ret = MR_IGNORE;
	const char *numStr, *contentStr;

	if(showApiLog) LOGD("vm_smsIndiaction");

	numStr = (*env)->GetStringUTFChars(env, number, JNI_FALSE);
	if (numStr) {
		uint8 buf[64];

		UTF8ToGBString((uint8 *)numStr, buf, sizeof(buf));

		contentStr = (*env)->GetStringUTFChars(env, content, JNI_FALSE);
		if(contentStr){
			uint8 buf2[1024];

			UTF8ToGBString((uint8 *)contentStr, buf2, sizeof(buf2));

			ret = mr_smsIndiaction(buf2, strlen(buf2), buf, MR_ENCODE_ASCII);

			(*env)->ReleaseStringUTFChars(env, content, contentStr);
		}

		(*env)->ReleaseStringUTFChars(env, number, numStr);
	}

	return ret;
}

jint vm_newSIMInd(JNIEnv * env, jobject self,
		jint type, jbyteArray old_IMSI)
{

	return MR_SUCCESS;
}

jint vm_registerAPP(JNIEnv * env, jobject self,
		jbyteArray jba, jint len, jint index)
{
	if(!jba || len <= 0)
		return MR_FAILED;

	jbyte* buf = malloc(len);
	(*jniEnv)->GetByteArrayRegion(jniEnv, jba, 0, len, buf);

	return mr_registerAPP((uint8 *)buf, (int32)len, (int32)index);
}

void mainLoop()
{
	LOGI("start mainLoop...");

	while(1)
	{
		pmsg_block_t msg = NULL;
		PT_VMMSG_BODY body = NULL;

		msgbox_fetch(gs_pVMMsgBox, &msg);
		body = (PT_VMMSG_BODY)msg->msg;

		switch(body->id)
		{
		case VMMSG_ID_START:
			mr_start_dsm(runMrpPath);
			break;

		case VMMSG_ID_MRTIMER:
			mr_timer();
			break;

		case VMMSG_ID_LOAD_MRP:
			mr_start_dsm(runMrpPath);
			break;

		case VMMSG_ID_PAUSE:
			mr_pauseApp();
			break;

		case VMMSG_ID_RESUME:
			mr_resumeApp();
			break;

		case VMMSG_ID_EVENT:
			mr_event(body->p0, body->p1, body->p2);
			break;

		case VMMSG_ID_GETHOST:
			LOGI("getHost callback ip:%#p", body->p0);
			((MR_GET_HOST_CB)mr_soc.callBack)(body->p0);
			break;

		case VMMSG_ID_STOP:
		{
			free(body);
			free(msg);

			goto end;
		}

		}

		free(msg);
		free(body);
	}

end:
	LOGI("exit mainLoop...");
}
