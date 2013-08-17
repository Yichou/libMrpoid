/*
 * msgbox.c
 *
 *  Created on: 2012-12-26
 *      Author: Yichou
 */

#include "msgbox.h"
#include <assert.h>

#include "emulator.h"



///初始化
static void msgbox_init(msg_box_t *mbox, size_t size)
{
	assert(mbox);

	pthread_mutex_init(&(mbox->mutex), NULL);
	pthread_cond_init(&(mbox->not_full), NULL);
	pthread_cond_init(&(mbox->not_empty), NULL);
	mbox->size = size;
	mbox->nready = 0;
	mbox->first = 0;
	mbox->last = 0;
	memset(mbox->msg_array, sizeof(void *) * size, 0);
}

msg_box_t * msgbox_new(size_t size)
{
	msg_box_t *mbox = (msg_box_t *) malloc(sizeof(msg_box_t) + sizeof(void *) * size); //加了消息个数
	if (mbox)
		msgbox_init(mbox, size);
	return mbox;
}

void msgbox_delete(msg_box_t *mbox)
{
	///等待所有的信号量结束
	pthread_mutex_lock(&(mbox->mutex));

	pthread_cond_destroy(&(mbox->not_full));
	pthread_cond_destroy(&(mbox->not_empty));
	pthread_mutex_unlock(&(mbox->mutex));
	pthread_mutex_destroy(&(mbox->mutex));

	free(mbox);
}

void msgbox_post(msg_box_t *mbox, msg_block_t *msg)
{
	int wakeup = 0;

	pthread_mutex_lock(&(mbox->mutex)); //占有互斥锁（阻塞操作）
	//等待到有存放的空间
	while (mbox->nready == mbox->size){
		LOGW("msgbox_post wait...");

		pthread_cond_wait(&(mbox->not_full), &(mbox->mutex)); //阻塞
	}

	mbox->msg_array[mbox->last] = msg;
	mbox->last = (mbox->last + 1) % mbox->size;
	if (0 == mbox->nready)
		wakeup = 1;
	mbox->nready++;
	pthread_mutex_unlock(&(mbox->mutex)); //释放互斥锁
	if (wakeup)
		pthread_cond_signal(&(mbox->not_empty)); //唤醒第一个调用 pthread_cond_wait() 而进入睡眠的线程
}

void msgbox_fetch(msg_box_t *mbox, msg_block_t **pmsg)
{
	int wakeup = 0;

	pthread_mutex_lock(&(mbox->mutex));
	while (0 == mbox->nready){
		//LOGW("msgbox_fetch wait...");
		pthread_cond_wait(&(mbox->not_empty), &(mbox->mutex));
	}

	*pmsg = mbox->msg_array[mbox->first];
	mbox->first = (mbox->first + 1) % mbox->size;

	if (mbox->nready == mbox->size)
		wakeup = 1;
	--(mbox->nready);
	pthread_mutex_unlock(&(mbox->mutex));
	if (wakeup)
		pthread_cond_signal(&(mbox->not_full));
}

void msgbox_clear(msg_box_t *mbox, deleteMsgCallback cb)
{
	msg_block_t *pmsg;

	pthread_mutex_lock(&(mbox->mutex));
	while (0 != mbox->nready)
	{
		pmsg = mbox->msg_array[mbox->first];
		cb(pmsg);
		mbox->first = (mbox->first + 1) % mbox->size;
		--mbox->nready;
	}
	pthread_mutex_unlock(&(mbox->mutex));
}
