#include "emulator.h"

#include <jni.h>

#include <android/log.h>
#include <malloc.h>
#include <string.h>
#include <fcntl.h>
#include <asm-generic/fcntl.h>
#include <stdio.h>

#define USE_JBITMAP

#ifdef USE_JBITMAP
#include <android/bitmap.h>
#endif

//---------------------------------
T_EMULATOR_PARAMS 	gEmulatorParams;
T_EMULATOR_CFG		gEmulatorCfg; 	//保存模拟器配置
T_APILOG_SW			gApiLogSw; //API LOG 控制

uint16				*cacheScreenBuffer;	//缓冲屏幕地址
JavaVM				*gs_JavaVM;
JNIEnv				*jniEnv;
pmsg_box_t			g_pEmuMsgBox;
int 				SCNW = 240;
int 				SCNH = 320;
int					showApiLog = TRUE;


//---------------------------------
static int   g_bAttatedT = 0;
static		m_bSuspendDraw;

static jfieldID		fid_charW, fid_charH, fid_editInputContent;
static jfieldID		fid_memLen, fid_memTop, fid_memLeft;

static jmethodID	id_flush; //刷新画布函数 ID
static jmethodID	id_finish; //结束activity 
static jmethodID	id_timerStart, id_timerStop; 
static jmethodID	id_showEdit, id_showDlg;
static jmethodID	id_getIntSysinfo;
static jmethodID	id_getStringSysinfo;
static jmethodID	id_requestCallback;
static jmethodID	id_sendSms;
static jmethodID	id_getHostByName;
static jmethodID	id_setStringOptions;
static jmethodID	id_playSound, id_stopSound, id_musicLoadFile, id_musicCMD, id_startShake, id_stopShake;
static jmethodID 	id_drawChar, id_measureChar;
static jmethodID 	id_readTsfFont;
static jmethodID 	id_callVoidMethod;


//static jbyteArray 	jba_screenBuf;
static jobject		obj_emulator;
static jobject		obj_mrpScreen;
static jobject		obj_emuAudio;
static jobject		obj_realBitmap;

static uint16		*realScreenBuffer;


inline JNIEnv *getJniEnv()
{
	return jniEnv;
}

void setJniEnv(JNIEnv *p)
{
	jniEnv = p;
}

JNIEnv *emu_attachJniEnv()
{
	int status;

	jniEnv = NULL;
	status = (*gs_JavaVM)->GetEnv(gs_JavaVM, (void **)&jniEnv, JNI_VERSION_1_4);
	if(status < 0)
	{
		status = (*gs_JavaVM)->AttachCurrentThread(gs_JavaVM, &jniEnv, NULL);
		if(status < 0)
		{
			return NULL;
		}
		g_bAttatedT = TRUE;
	}

	return jniEnv;
}

void emu_detachJniEnv()
{
	if(g_bAttatedT)
	{
		(*gs_JavaVM)->DetachCurrentThread(gs_JavaVM);
		g_bAttatedT = FALSE;
	}
}

static void initJniId(JNIEnv * env, jobject self)
{
	//----------- Emulator.java -------------------------------------
	jclass cls = (*env)->GetObjectClass(env, obj_emulator);

	fid_charW = (*env)->GetFieldID(env, cls, "N2J_charW", "I");
	fid_charH = (*env)->GetFieldID(env, cls, "N2J_charH", "I");
	fid_editInputContent = (*env)->GetFieldID(env, cls, "N2J_editInputContent", "Ljava/lang/String;");
	fid_memTop = (*env)->GetFieldID(env, cls, "N2J_memTop", "I");
	fid_memLen = (*env)->GetFieldID(env, cls, "N2J_memLen", "I");
	fid_memLeft = (*env)->GetFieldID(env, cls, "N2J_memLeft", "I");

	id_flush = (*env)->GetMethodID(env, cls, "N2J_flush", "(IIII)V");
	id_requestCallback = (*env)->GetMethodID(env, cls, "N2J_requestCallback", "(II)V");
	id_timerStart = (*env)->GetMethodID(env, cls, "N2J_timerStart", "(S)V");
	id_timerStop = (*env)->GetMethodID(env, cls, "N2J_timerStop", "()V");
	id_showEdit = (*env)->GetMethodID(env, cls, "N2J_showEdit", "(Ljava/lang/String;Ljava/lang/String;II)V");
	id_showDlg = (*env)->GetMethodID(env, cls, "N2J_showDlg", "(Ljava/lang/String;)V");
	id_finish = (*env)->GetMethodID(env, cls, "N2J_finish", "()V");
	id_sendSms = (*env)->GetMethodID(env, cls, "N2J_sendSms", "(Ljava/lang/String;Ljava/lang/String;ZZ)I");
	id_getHostByName = (*env)->GetMethodID(env, cls, "N2J_getHostByName", "(Ljava/lang/String;)V");
	id_setStringOptions = (*env)->GetMethodID(env, cls, "N2J_setOptions", "(Ljava/lang/String;Ljava/lang/String;)V");

	id_getIntSysinfo = (*env)->GetMethodID(env, cls, "N2J_getIntSysinfo", "(Ljava/lang/String;)I");
	id_getStringSysinfo = (*env)->GetMethodID(env, cls, "N2J_getStringSysinfo", "(Ljava/lang/String;)Ljava/lang/String;");
	id_readTsfFont = (*env)->GetMethodID(env, cls, "N2J_readTsfFont", "()[B");

	id_callVoidMethod = (*env)->GetMethodID(env, cls, "N2J_callVoidMethod", "([Ljava/lang/String;)V");

	(*env)->DeleteLocalRef(env, cls);

	//----------- EmuAudio.java -------------------------------------
	cls = (*env)->GetObjectClass(env, obj_emuAudio);

	id_playSound = (*env)->GetMethodID(env, cls, "N2J_playSound", "(Ljava/lang/String;I)V");
	id_stopSound = (*env)->GetMethodID(env, cls, "N2J_stopSound", "()V");
	id_musicLoadFile = (*env)->GetMethodID(env, cls, "N2J_musicLoadFile", "(Ljava/lang/String;)V");
	id_musicCMD = (*env)->GetMethodID(env, cls, "N2J_musicCMD", "(III)I");
	id_startShake = (*env)->GetMethodID(env, cls, "N2J_startShake", "(I)V");
	id_stopShake = (*env)->GetMethodID(env, cls, "N2J_stopShake", "()V");

	(*env)->DeleteLocalRef(env, cls);

	//----------- MrpScreen.java -------------------------------------
	cls = (*env)->GetObjectClass(env, obj_mrpScreen);

	id_drawChar = (*env)->GetMethodID(env, cls, "N2J_drawChar", "(SIII)V");
	id_measureChar = (*env)->GetMethodID(env, cls, "N2J_measureChar", "(S)V");

	(*env)->DeleteLocalRef(env, cls);
}

static void freeJniId()
{

}

//------------------------
void sevg_handler(int signo)
{
	char *argv[2] = {
			"crash",
			"不小心又崩溃了%>_<%"
	};

	N2J_callVoidMethodString(2, argv);

	sleep(2);

	exit(0);
}

//初始化模拟器  唯一实例
void native_create(JNIEnv *env, jobject self, jobject mrpScreen, jobject emuAudio)
{
	LOGI("native_create");

	signal(SIGSEGV, sevg_handler);

	jniEnv = env;

	//直接保存 obj 到 DLL 中的全局变量是不行的,应该调用以下函数:
	obj_emulator = (*env)->NewGlobalRef(env, self);
	obj_emuAudio = (*env)->NewGlobalRef(env, emuAudio);
	obj_mrpScreen = (*env)->NewGlobalRef(env, mrpScreen);

	initJniId(env, self);

	gApiLogSw.showFile = FALSE;
	gApiLogSw.showInputLog = FALSE;
	gApiLogSw.showTimerLog = FALSE;
	gApiLogSw.showNet = FALSE;
	gApiLogSw.showMrPlat = FALSE;
	gApiLogSw.showFW = FALSE;

	gEmulatorCfg.androidDrawChar = FALSE;
	gEmulatorCfg.useSysScreenbuf = FALSE;
	gEmulatorCfg.useLinuxTimer = FALSE;
	gEmulatorCfg.memSize = 4; //默认4M

	tsf_init();

	//初始化 DSM启动时间
	gettimeofday(&gEmulatorParams.dsmStartTime, NULL);
}

void native_pause(JNIEnv *env, jobject self)
{
	m_bSuspendDraw = 1;
	LOGD("native pause!");
}

void native_resume(JNIEnv *env, jobject self)
{
	LOGD("native resume!");
	m_bSuspendDraw = 0;

	if(gEmulatorCfg.b_vm_running)
		emu_bitmapToscreen(cacheScreenBuffer, 0, 0, screenW, screenH);
}

void native_destroy(JNIEnv * env, jobject self)
{
	(*env)->DeleteGlobalRef(env, obj_emulator);
	(*env)->DeleteGlobalRef(env, obj_emuAudio);
	(*env)->DeleteGlobalRef(env, obj_mrpScreen);
	(*env)->DeleteGlobalRef(env, obj_realBitmap);

//	(*env)->ReleaseByteArrayElements(env, jba_screenBuf, jScreenBuf, 0);

	gs_JavaVM = NULL;
	obj_emulator = NULL;
	tsf_dispose();

	LOGI("native destroy");
}

/**
 * native 与 java 的回调中介
 */
void native_callback(JNIEnv * env, jobject self, int what, int param)
{
	if(!gEmulatorCfg.b_vm_running){
		LOGW("native_callback but vm has exited!");
		return;
	}

	LOGD("native_callback(%d, %d)", what, param);

	switch(what)
	{
	case CALLBACK_PLAYMUSIC:
		if(dsmMediaPlay.cb)
			dsmMediaPlay.cb(param);
		break;

	case CALLBACK_GETHOSTBYNAME:
		LOGI("getHost callback ip:%#p", param);
		((MR_GET_HOST_CB)mr_soc.callBack)(param);
		break;

	case CALLBACK_TIMER_OUT:
		mr_timer();
		break;
	}
}

void native_mrpScreenRest(JNIEnv * env, jobject self,
		jobject cacheBitmap, jobject realBitmap, jint width, jint height)
{
	if (!cacheBitmap || !realBitmap || width <= 0 || height <= 0) {
		LOGE("native_initScreen error params!");
		return;
	}

	obj_realBitmap = (*env)->NewGlobalRef(env, realBitmap);

	SCNW = width;
	SCNH = height;

#ifdef USE_JBITMAP
	AndroidBitmapInfo info;
	void* pixels;
	int ret;

	if ((ret = AndroidBitmap_lockPixels(env, cacheBitmap, &pixels)) >= 0) {
		cacheScreenBuffer = (uint16 *)pixels;
		AndroidBitmap_unlockPixels(env, cacheBitmap);
	}else{
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return;
	}

	if ((ret = AndroidBitmap_lockPixels(env, realBitmap, &pixels)) >= 0) {
		realScreenBuffer = (uint16 *)pixels;
		AndroidBitmap_unlockPixels(env, realBitmap);
	}else{
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return;
	}
#endif
}

jstring native_getStringOptions(JNIEnv * env, jobject self, jstring key)
{
	const char *str, *str2;
	jstring ret = NULL;


	str = (*env)->GetStringUTFChars(env, key, JNI_FALSE);
	if(str)
	{
		if (strcasecmp(str, "memSize") == 0)
		{
			char buf[24];
			sprintf(buf, "%d", gEmulatorCfg.memSize);
			ret = (*env)->NewStringUTF(env, buf);
		}

		(*env)->ReleaseStringUTFChars(env, key, str);
	}

	return ret;
}

void native_setStringOptions(JNIEnv * env, jobject self, jstring key, jstring value)
{
	const char *str, *str2;
	char buf[128];

	str = (*env)->GetStringUTFChars(env, key, JNI_FALSE);
	str2 = (*env)->GetStringUTFChars(env, value, JNI_FALSE);
	if(str && str2)
	{
		if (strcasecmp(str, "sdpath") == 0)
		{
			SetDsmSDPath(str2);
		}
		else if (strcasecmp(str, "mythroadPath") == 0) //mythroad 路径
		{
			SetDsmPath(str2);
		}
		else if (strcasecmp(str, "manufactory") == 0)
		{

		}
		else if (strcasecmp(str, "type") == 0)
		{

		}
		else if (strcasecmp(str, "smsCenter") == 0)
		{
			strncpy(dsmSmsCenter, str2, sizeof(dsmSmsCenter));
		}
		/*else if (strcasecmp(str, "platscreenbuf") == 0)
		{

		}*/
	}
	if(str) (*env)->ReleaseStringUTFChars(env, key, str);
	if(str2) (*env)->ReleaseStringUTFChars(env, value, str2);
}

void native_setIntOptions(JNIEnv * env, jobject self, jstring key, jint value)
{
	const char *str;

	str = (*env)->GetStringUTFChars(env, key, JNI_FALSE);
	if(str)
	{
//		if(showApiLog)
//			LOGI("setOperation(%s, %d)", str, value);
		if(strcasecmp(str, "platdrawchar") == 0){
			gEmulatorCfg.androidDrawChar = (value);
		}else if(strcasecmp(str, "enableApilog") == 0){
			showApiLog = value;
			if(!showApiLog){
				memset(&gApiLogSw, 0, sizeof(gApiLogSw));
			}
		}else if(strcasecmp(str, "enable_log_input") == 0){
			gApiLogSw.showInputLog = showApiLog && value;
		}else if(strcasecmp(str, "enable_log_file") == 0){
			gApiLogSw.showFile = showApiLog && value;
		}else if(strcasecmp(str, "enable_log_net") == 0){
			gApiLogSw.showNet = showApiLog && value;
		}else if(strcasecmp(str, "enable_log_mrplat") == 0){
			gApiLogSw.showMrPlat = showApiLog && value;
		}else if(strcasecmp(str, "enable_log_timer") == 0){
			gApiLogSw.showTimerLog = showApiLog && value;
		}else if(strcasecmp(str, "enable_log_fw") == 0){
			gApiLogSw.showFW = showApiLog && value;
		}else if(strcasecmp(str, "enable_log_mrprintf") == 0){
			gApiLogSw.showMrPrintf = showApiLog && value;
		}else if(strcasecmp(str, "uselinuxTimer") == 0){
			gEmulatorCfg.useLinuxTimer = value;
		}else if(strcasecmp(str, "enableExram") == 0){
			gEmulatorCfg.enableExram = value;
		}else if(strcasecmp(str, "platform") == 0){
			gEmulatorCfg.platform = value;
		}else if(strcasecmp(str, "memSize") == 0){
			gEmulatorCfg.memSize = value;
		}

		(*env)->ReleaseStringUTFChars(env, key, str);
	}
}

void native_getMemoryInfo(JNIEnv * env, jobject self)
{
	uint32 len, left, top;
	mr_getMemoryInfo(&len, &left, &top);

	(*env)->SetIntField(env, self, fid_memTop, (jint)top);
	(*env)->SetIntField(env, self, fid_memLen, (jint)len);
	(*env)->SetIntField(env, self, fid_memLeft, (jint)left);
}

////////////////////////////////////////
jstring native_getAppName(JNIEnv * env, jobject self, jstring path)
{
	const char *str;

	str = (*env)->GetStringUTFChars(env, path, JNI_FALSE);
	if(str){
		int fd;
		uint8 buf[128]={0};
		uint8 utf8[256]={0};
		jbyteArray ba;

		fd = open(str, O_RDONLY);
		(*env)->ReleaseStringUTFChars(env, path, str);
		if(fd >= 0){
			lseek(fd, 28, 0);
			read(fd, buf, 24);
			close(fd);
		}

		GBToUTF8String(buf, utf8, sizeof(utf8));
		return (*env)->NewStringUTF(env, utf8);
	}

	return NULL;
}

void native_flushBitmap(JNIEnv * env, jobject self,
		jobject bitmap, jint x, jint y, jint w, jint h)
{
//#ifdef USE_JBITMAP
//	AndroidBitmapInfo info;
//	void* pixels;
//	int ret;
//
//	if ((ret = AndroidBitmap_getInfo(jniEnv, bitmap, &info)) < 0) {
//		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
//		return;
//	}
//	if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
//		LOGE("Bitmap format is not RGB_565 !");
//		return;
//	}
//	if ((ret = AndroidBitmap_lockPixels(jniEnv, bitmap, &pixels)) < 0) {
//		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
//	}
//
//	//终究是有问题的
//
//	//刷新到屏幕
//	uint16 *p = (uint16 *) pixels;
//	int i;
//	for (i = y; i <= y1; i++)
//		memcpy((p + (sw * i) + x), (data + (sw * i) + x), w * 2);
//
//	AndroidBitmap_unlockPixels(jniEnv, bitmap);
//#endif
}

///////////// N->J /////////////////////////////////////////////
static void fix_x0y0x1y1(int *x0, int *y0, int *x1, int *y1)
{
	if(*x0 > *x1){ //交换
		int tmp = *x0;
		*x0 = *x1;
		*x1 = tmp;
	}

	if(*y0 > *y1){ //交换
		int tmp = *y0;
		*y0 = *y1;
		*y1 = tmp;
	}
}

static int clip_rect(int *x0, int *y0, int *x1, int *y1, int r, int b)
{
	fix_x0y0x1y1(x0, y0, x1, y1);

	//超出的情况
	if (*x0>r || *y0>b || *x1<0 || *y1<0)
		return 1;

	//根据Clip修正后的 x y r b 
	*x0 = MAX(*x0, 0);
	*y0 = MAX(*y0, 0);
	*x1 = MIN(*x1, r);
	*y1 = MIN(*y1, b);

	return 0;
}

void native_mrpScreen_lockBitmap(JNIEnv * env, jobject self)
{
	(*env)->MonitorEnter(env, obj_realBitmap);
}

void native_mrpScreen_unLockBitmap(JNIEnv * env, jobject self)
{
	(*env)->MonitorExit(env, obj_realBitmap);
}

void emu_bitmapToscreen(uint16* data, int x, int y, int w, int h)
{
	if(m_bSuspendDraw)
		return;

	int ret;
	int				x1, y1, r, b;
	int32			sw=SCNW, sh=SCNH;
	
	if(x>=sw || y>=sh || w<=0 || h<=0)
		return;

	r = sw-1, b = sh-1;
	x1 = x+w-1, y1 = y+h-1;
	clip_rect(&x, &y, &x1, &y1, r, b);
	w = x1-x+1, h = y1-y+1;
	
	getJniEnv();

	(*jniEnv)->MonitorEnter(jniEnv, obj_realBitmap);

	//刷新到屏幕
	uint16 *p = (uint16 *) realScreenBuffer;
	int i;
	for (i = y; i <= y1; i++)
		memcpy((p + (sw * i) + x), (data + (sw * i) + x), w * 2);

	(*jniEnv)->MonitorExit(jniEnv, obj_realBitmap);

	(*jniEnv)->CallVoidMethod(jniEnv, obj_emulator, id_flush, x, y, w, h);
}

int32 emu_timerStart(uint16 t)
{
	(*getJniEnv())->CallVoidMethod(jniEnv, obj_emulator, id_timerStart, t);

	return MR_SUCCESS;
}

int32 emu_timerStop()
{
	(*getJniEnv())->CallVoidMethod(jniEnv, obj_emulator, id_timerStop);

	return MR_SUCCESS;
}

void N2J_callVoidMethodString(int argc, const char *argv[])
{
	jclass clsString = (*jniEnv)->FindClass(jniEnv, "java/lang/String");
	jobjectArray arr = (*jniEnv)->NewObjectArray(jniEnv, argc, clsString, NULL);
	if(arr != NULL){
		int i;
		for(i=0; i<argc; ++i){
			jstring str = (*jniEnv)->NewStringUTF(jniEnv, argv[i]);
			(*jniEnv)->SetObjectArrayElement(jniEnv, arr, i, str);
		}

		(*jniEnv)->CallVoidMethod(jniEnv, obj_emulator, id_callVoidMethod, arr);
	}
}

void emu_showDlg(const char *msg)
{
	if(!msg) return;

	jstring jstr = NULL;

	static char buf[256];
	GBToUTF8String((uint8 *)msg, (uint8 *)buf, sizeof(buf));
	jstr = (*jniEnv)->NewStringUTF(jniEnv, buf);
	(*jniEnv)->CallVoidMethod(jniEnv, obj_emulator, id_showDlg, jstr);
}


typedef struct {
	char *buf_title;
	char *buf_text;
	char *buf_content;
}T_MR_EDIT, *PT_MR_EDIT;

int32 emu_showEdit(const char * title, const char * text, int type, int max_size)
{
	jstring stitle = NULL, stext = NULL;
	char *buf;
	int l;
	T_MR_EDIT *edit = malloc(sizeof(T_MR_EDIT));

	if(showApiLog) LOGI("emu_showEdit");

	getJniEnv();

	memset(edit, 0, sizeof(T_MR_EDIT));
	if(title){
		l = UCS2_strlen(title);
		buf = malloc(l+2);
		memset(buf, 0, l+2);
		memcpy(buf, title, l);
		UCS2ByteRev(buf); //unicode大端转小端
		stitle = (*jniEnv)->NewString(jniEnv, (const jchar*)buf, l/2);

		edit->buf_title = buf;
	}else{
		stitle = (*jniEnv)->NewStringUTF(jniEnv, "\x0\x0");
	}
	
	if(text){
		l = UCS2_strlen(text);
		buf = malloc(l+2);
		memset(buf, 0, l+2);
		memcpy(buf, text, l);
		UCS2ByteRev(buf);
		stext = (*jniEnv)->NewString(jniEnv, (const jchar*)buf, l/2);

		edit->buf_text = buf;
	}else{
		stext = (*jniEnv)->NewStringUTF(jniEnv, "\x0\x0");
	}

	(*jniEnv)->CallVoidMethod(jniEnv, obj_emulator, id_showEdit, stitle, stext, type, max_size);

	return (int32)edit;
}

/**
 * 返回编辑框内容的起始地址(编辑框的内容指针，大端unicode编码)，如果失败返回NULL.
 */
const char* emu_getEditInputContent(int32 editHd)
{
	if(!editHd) return NULL;

	jstring text = (jstring)(*getJniEnv())->GetObjectField(jniEnv, obj_emulator, fid_editInputContent);
	T_MR_EDIT *edit = (PT_MR_EDIT)editHd;

	if(text != NULL){
		int len = (*jniEnv)->GetStringLength(jniEnv, text); //返回 unicode 个数（字节数/2）
		if(len > 0){
			const jchar * str = (*jniEnv)->GetStringChars(jniEnv, text, JNI_FALSE); //返回 unicode 小端
			if(str != NULL){
				len *= 2;

				char *content = malloc(len + 2);
				memset(content, 0, len+2);
				memcpy(content, str, len);
				UCS2ByteRev(content); //转为大端

				edit->buf_content = content;

				(*jniEnv)->ReleaseStringChars(jniEnv, text, str);

				return content;
			}
		}
	} 
	
	//貌似直接返回空指针会挂
	edit->buf_content = malloc(8);
	memset(edit->buf_content, 0, 8);

	return edit->buf_content;
}

void emu_releaseEdit(int32 editHd)
{
	if(editHd){
		T_MR_EDIT *edit = (PT_MR_EDIT)editHd;

		FREE_SET_NULL(edit->buf_content);
		FREE_SET_NULL(edit->buf_text);
		FREE_SET_NULL(edit->buf_title);

		free(edit);
	}
}

void emu_finish()
{
	(*getJniEnv())->CallVoidMethod(jniEnv, obj_emulator, id_finish);
}

int emu_getIntSysinfo(const char * name)
{
	int i = (*getJniEnv())->CallIntMethod(jniEnv, obj_emulator,
			id_getIntSysinfo,
			(*jniEnv)->NewStringUTF(jniEnv, name));
	return i;
}

void emu_getHostByName(const char * name)
{
	if(!name) return;

	(*getJniEnv())->CallVoidMethod(jniEnv, obj_emulator, id_getHostByName,
			(*jniEnv)->NewStringUTF(jniEnv, name));
}

void emu_setStringOptions(const char *key, const char *value)
{
	if(!key || !value) return;

	(*getJniEnv())->CallVoidMethod(jniEnv, obj_emulator, id_setStringOptions,
			(*jniEnv)->NewStringUTF(jniEnv, key),
			(*jniEnv)->NewStringUTF(jniEnv, value)
			);
}

const char *emu_getStringSysinfo(const char * name)
{
	jstring text = (jstring)(*getJniEnv())->CallObjectMethod(jniEnv, obj_emulator,
		id_getStringSysinfo,
		(*jniEnv)->NewStringUTF(jniEnv, name));

	if(text != NULL){
		const char *str = (*jniEnv)->GetStringUTFChars(jniEnv, text, JNI_FALSE);
		if(str != NULL){
			int l = strlen(str) + 1;
			char *content = malloc(l);
			memcpy(content, str, l);

			(*jniEnv)->ReleaseStringUTFChars(jniEnv, text, str);

			return content;
		}
	}

	LOGE("emu_getStringSysinfo ret null");

	return NULL;
}

void emu_requestCallback(int what, int param)
{
	LOGI("emu_requestCallback(%d, %d)", what, param);

	if(!gEmulatorCfg.b_vm_running){
		LOGW(" error: vm has exited!");
		return;
	}

	int status;
	JNIEnv *env;

	status = (*gs_JavaVM)->GetEnv(gs_JavaVM, (void **) &env, JNI_VERSION_1_4);
	if (status < 0)
	{
		status = (*gs_JavaVM)->AttachCurrentThread(gs_JavaVM, &env, NULL);
		if (status < 0)
		{
			LOGE(" error: failed to attach current thread!");
		}else{
			(*env)->CallVoidMethod(env, obj_emulator, id_requestCallback, what, param);
			(*gs_JavaVM)->DetachCurrentThread(gs_JavaVM);

			LOGI(" suc.", what, param);
		}
	}
}

/**
 * 发送短信接口
 *
 * 2013-3-26 14:51:08
 */
int emu_sendSms(char *pNumber, char *pContent, int32 flags)
{
	if(!pNumber || !pContent)
		return;

	LOGD("emu_sendSms, msg addr: %s", pNumber);

	jstring num, msg;

	num = (*jniEnv)->NewStringUTF(jniEnv, pNumber);

	if((flags&0x07) == MR_ENCODE_UNICODE){
		int l = UCS2_strlen(pContent);
		char *tmp = malloc(l + 2);

		memcpy(tmp, pContent, l);
		tmp[l] = 0;
		tmp[l+1] = 0;
		UCS2ByteRev(tmp);

		msg = (*jniEnv)->NewString(jniEnv, (const jchar*)tmp, l/2);

		free(tmp);
	}else{
		int l = strlen(pContent);
		int l2 = (l+1)*2;
		uint8 *buf = malloc(l2);

		GBToUTF8String(pContent, buf, l2);
		LOGD("  (gb)msg body: %s", buf);

		msg = (*jniEnv)->NewStringUTF(jniEnv, buf);
		free(buf);
	}

	return (*jniEnv)->CallIntMethod(jniEnv, obj_emulator, id_sendSms,
			num, msg, flags&0x08, flags&0x10);
}

/**
 * 读取字体
 *
 * 2013-4-19 9:24:35
 */
void N2J_readTsfFont(uint8 **outbuf, int32 *outlen)
{
	jbyte *buf = NULL;
	jint len = 0;
	jbyteArray jba = (*jniEnv)->CallObjectMethod(jniEnv, obj_emulator, id_readTsfFont);

	if(jba){
		len = (*jniEnv)->GetArrayLength(jniEnv, jba);
		if(len > 0){
			buf = malloc(len);
			(*jniEnv)->GetByteArrayRegion(jniEnv, jba, 0, len, buf);

			LOGD("read tsf from assets SUC!", buf, len);
		}

		(*jniEnv)->DeleteLocalRef(jniEnv, jba);
	}

	*outbuf = (uint8 *)buf;
	*outlen = len;
}

//-------------- Begin MrpScreen.java ------------------------------------------------
static int rgb565_to_rgb888(int p)
{
	int result;
	int c;

	/*
	 * a higher quality version to avoid the faint green in stead of speed.
	 * and also recover the perfect white.
	 *
	 * rrrrrggg gggbbbbb => rrrrrRRRggggggGGbbbbbBBB
	 * RRR = R5[2:0]
	 *  GG = G6[1:0]
	 * BBB = B5[2:0]
	 */
	c = (p << 27) >> 27;
	result = (c << 3) | (c & 0x7);
	c = (p << 21) >> 26;
	result += ((c << 2) | (c & 0x3)) << 8;
	c = p >> 11;
	result += ((c << 3) | (c & 0x7)) << 16;

	return result|0xff000000;
}

void emu_drawChar(uint16 ch, int x, int y, uint16 color)
{
	(*getJniEnv())->CallVoidMethod(jniEnv, obj_mrpScreen, id_drawChar, ch, x, y, rgb565_to_rgb888((int)color));
}

void emu_measureChar(uint16 ch, int *w, int *h)
{
	jint ret;

	(*getJniEnv())->CallVoidMethod(jniEnv, obj_mrpScreen, id_measureChar, ch);
	ret = (*jniEnv)->GetIntField(jniEnv, obj_emulator, fid_charW);
	if(w != NULL)
		*w = ret;
	ret = (*jniEnv)->GetIntField(jniEnv, obj_emulator, fid_charH);
	if(h != NULL)
		*h = ret;
}
//-------------- End MrpScreen.java ------------------------------------------------


//-------------- Begin EmuAudio.java ------------------------------------------------
void emu_palySound(const char *path, int loop)
{
	jstring jspath = (*getJniEnv())->NewStringUTF(jniEnv, path);
	(*jniEnv)->CallVoidMethod(jniEnv, obj_emuAudio,
					id_playSound,
					jspath, loop);
}

void emu_stopSound(int id)
{
	(*getJniEnv())->CallVoidMethod(jniEnv, obj_emuAudio,
						id_stopSound);
}

void emu_musicLoadFile(const char *path)
{
	(*getJniEnv())->CallVoidMethod(jniEnv, obj_emuAudio,
							id_musicLoadFile,
							(*jniEnv)->NewStringUTF(jniEnv, path));
}

int emu_musicCMD(int cmd, int arg0, int arg1)
{
	return (*getJniEnv())->CallIntMethod(jniEnv, obj_emuAudio,
								id_musicCMD,
								cmd, arg0, arg1);
}

void emu_startShake(int ms)
{
	(*getJniEnv())->CallVoidMethod(jniEnv, obj_emuAudio,
				id_startShake, ms);
}

void emu_stopShake()
{
	(*getJniEnv())->CallVoidMethod(jniEnv, obj_emuAudio,
				id_stopShake);
}

//-------------- End EmuAudio.java ------------------------------------------------


///////////////////////////////////////
/* On Android NDK, rand is inlined function, but postproc needs rand symbol */
#if defined(__ANDROID__)
#define rand __rand
#include <stdlib.h>
#include "dsm.h"
#undef rand

extern int rand(void)
{
  return __rand();
}
#endif
