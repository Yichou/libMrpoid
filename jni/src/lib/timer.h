#ifndef _MR_TIMER_H
#define _MR_TIMER_H

#include "mr_types.h"


typedef void (*timerCallBack)(int);

//返回定时器句柄
int32 timerStart(uint32 dru, timerCallBack cb, int loop);

//handel：定时器句柄
void timerStop(int32 handel);

#endif