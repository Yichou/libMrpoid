#ifndef _DSM_H
#define _DSM_H


#define DSM_ENWORD_H	16
#define DSM_ENWORD_W	8
#define DSM_CHWORD_H	16
#define DSM_CHWORD_W	16

#define DSM_SUPPROT_SOC_NUM                        (5)

/**
 * 协议：SDCARD 作为跟目录 以 /结尾
 *		dsmWorkPath 以 / 结尾，切换到跟路径后为空
 */
#define DSM_ROOT_PATH		"mythroad/"
#define SDCARD_PATH			"/mnt/sdcard/"

#define DSM_HIDE_DRIVE		".disk/"
#define DSM_DRIVE_A			"a/"
#define DSM_DRIVE_B			"b/"
#define DSM_ROOT_PATH_SYS	"mythroad/"

#define TMP_PATH			".tmp"

//------------------------------------------------
#define DSM_FACTORY			"mrpej"
#define DSM_TYPE			"android"


/*请不要做修改*/
#define DSM_FACTORY_ID                                  "skymp"				/*厂商标识，最多七个小写字符*/

#define MT6235

/*请不要修改这些值*/
#if (defined(MT6223P)||defined(MT6223)||defined(MT6223P_S00))
#define DSM_PLAT_VERSION                                       (2) /*手机平台区分(1~99)*/
#elif (defined(MT6226)||defined(MT6226M)||defined(MT6226D))
#define DSM_PLAT_VERSION                                       (4)  /*手机平台区分(1~99)*/
#elif (defined(MT6228))
#define DSM_PLAT_VERSION                                       (5)  /*手机平台区分(1~99)*/
#elif (defined(MT6225))
#define DSM_PLAT_VERSION                                        (3)  /*手机平台区分(1~99)*/
#elif (defined(MT6230))
#define DSM_PLAT_VERSION                                        (6)  /*手机平台区分(1~99)*/
#elif (defined(MT6227)||defined(MT6227D))
#define DSM_PLAT_VERSION                                        (7)
#elif (defined(MT6219))
#define DSM_PLAT_VERSION                                        (1)
#elif(defined(MT6235)||defined(MT6235B))
#define DSM_PLAT_VERSION                                        (8)
#elif(defined(MT6229))
#define DSM_PLAT_VERSION                                        (9)
#elif(defined(MT6253)||defined(MT6253T))
#define DSM_PLAT_VERSION                                        (10)
#elif(defined(MT6238))
#define DSM_PLAT_VERSION                                         (11)
#elif(defined(MT6239))
#define DSM_PLAT_VERSION                                          (12)
#else
#error PLATFORM NOT IN LIST PLEASE CALL SKY TO ADD THE PLATFORM
#endif

#ifdef DSM_IDLE_APP
#define DSM_FAE_VERSION                                       (180)    /*由平台组统一分配版本号，有需求请联系平台组*/
#else
#define DSM_FAE_VERSION                                       (182)    /*由平台组统一分配版本号，有需求请联系平台组*/
#endif


#define DSM_HANDSET_ID                                  "android"				 /*手机标识，最多七个小写字符*/


//---------------------------------
typedef enum {
	NETTYPE_WIFI=0,
	NETTYPE_CMWAP=1,
	NETTYPE_CMNET=2,
	NETTYPE_UNKNOW=3
}AND_NETTYPE;

typedef enum
{
	DSM_SOC_CLOSE,
	DSM_SOC_OPEN,
	DSM_SOC_CONNECTING,
	DSM_SOC_CONNECTED,
	DSM_SOC_ERR
}T_DSM_SOC_STAT_ENUM;

typedef enum
{
	DSM_SOC_NOREAD,
	DSM_SOC_READABLE
}T_DSM_SOC_READ_STAT;

typedef enum
{
	DSM_SOC_NOWRITE,
	DSM_SOC_WRITEABLE
}T_DSM_SOC_WRITE_STAT;


typedef struct
{
	uint8 mod_id;
	uint8 identifier;   
	int event_id;
	int result;
} mr_socket_event_struct;

typedef struct
{
	int socketId; //socket 句柄
	int realSocketId; //真实 socket 句柄（代理有效）
	int isProxy; //代理标志
	int realConnected; //真实连接上标志

	int socStat;
	int readStat;
	int writeStat;
}T_DSM_SOC_STAT;

typedef struct
{     
	void *callBack;
} mr_socket_struct;

typedef struct
{
   int32 pos;  //单位,秒s.
}T_SET_PLAY_POS;

typedef struct
{
	int32 pos;
} T_MEDIA_TIME;

/*回调有两种可能的返回值：
ACI_PLAY_COMPLETE   0  //播放结束
ACI_PLAY_ERROR       1  //播放时遇到错误
Loop ：1，循环播放；0，不循环播放；2，PCM循环播放模式
Block：1，阻塞播放；0，不阻塞播放*/
typedef void (*ACI_PLAY_CB)(int32 result);

typedef struct
{
	ACI_PLAY_CB cb; //回调函数
	int32 loop;
	int32 block;
}T_DSM_MEDIA_PLAY;

typedef struct
{
	char *src;
	int32 len;
	int32 src_type;// MRAPP_SRC_TYPE
} MRAPP_IMAGE_ORIGIN_T;

typedef struct
{
   int32 width;    //图片的宽度
   int32 height;   //图片的高度
}MRAPP_IMAGE_SIZE_T;

typedef struct
{
	char *src;
	int32 src_len;
	int32 src_type;
	int32 ox;
	int32 oy;
	int32 w;
	int32 h;
} T_DRAW_DIRECT_REQ;

typedef struct {
	uint8 level;
	uint8 current_band;
	uint8 rat;
	uint8 flag;
} T_RX;


//---------------------------------
void dsm_init(void);
void dsm_reset(void);


int32 mr_exit(void);
int mr_getSocketState(int s);
void mr_cacheFlush(int id);

/** 设置 SD卡路径，参数底层不做错误检查 */
void SetDsmSDPath(const char * path);
void SetDsmWorkPath(const char *path);

#endif
