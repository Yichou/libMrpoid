/*
 * msgbox.h
 *
 *  Created on: 2012-12-26
 *      Author: Yichou
 *        form: http://www.cnblogs.com/westfly/archive/2012/03/15/2398630.html
 */
#ifndef MSGBOX_H_
#define MSGBOX_H_

#include <unistd.h>
#include <pthread.h>



typedef struct ares_msg_block
{
    pthread_t   pid;
    void        *msg;
}msg_block_t, *pmsg_block_t;

typedef struct ares_msg_box
{
    pthread_cond_t  not_full; 	//条件 满
    pthread_cond_t  not_empty; 	//条件 空
    pthread_mutex_t mutex; 		//互斥锁

    //消息的循环队列
    size_t          first;
    size_t          last;
    size_t          size;			//最大消息数
    size_t          nready;         //多少个消息，判断满与空
    pmsg_block_t    msg_array[0];   //柔性数组
}msg_box_t, *pmsg_box_t;


typedef void (*deleteMsgCallback)(pmsg_block_t pMsg);


/**
 @note 清空所有消息
 */
void msgbox_clear(msg_box_t *mbox, deleteMsgCallback cb);

/**
 @note 创建消息盒子
 @param size: 消息容量，超过了将阻塞
 */
msg_box_t * msgbox_new(size_t size);

/**
 @note 删除消息盒子
 */
void msgbox_delete(msg_box_t *mbox);

/**
 @note 发消息
 */
void msgbox_post(msg_box_t *mbox, msg_block_t *msg);

/**
 @note 取消息
 */
void msgbox_fetch(msg_box_t *mbox, msg_block_t **pmsg);

#endif /* MSGBOX_H_ */
