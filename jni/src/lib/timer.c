#include "timer.h"

#include <sys\types.h>
#include <asm\signal.h>
#include <linux\time.h>
#include <malloc.h>
#include <string.h>
#include <signal.h>
#include <sys\time.h>


typedef struct {
	struct itimerval tick;
	timerCallBack cb;
	int loop;
}T_TIMER, *PT_TIMER;



int32 timerStart(uint32 dru, timerCallBack cb, int loop)
{
	PT_TIMER timer = malloc(sizeof(T_TIMER));

	memset(timer, 0, sizeof(T_TIMER));

	timer->cb = cb;
	timer->tick.it_value.tv_sec = dru / 1000;
	timer->tick.it_value.tv_usec = dru * 1000 % 1000000;
	if(loop){
		timer->tick.it_interval.tv_sec = dru / 1000;
		timer->tick.it_interval.tv_usec = dru * 1000 % 1000000;
	}

	signal(SIGALRM, (sighandler_t)cb);

	// ITIMER_REAL，表示以real-time方式减少timer，在timeout时会送出SIGALRM signal
	if (setitimer(ITIMER_REAL, &timer->tick, NULL) == -1){
		return MR_FAILED;
	}

	return (int32)timer;
}

void timerStop(int32 t)
{
	PT_TIMER timer = (PT_TIMER)t;

	timerclear(&timer->tick.it_value);
	timerclear(&timer->tick.it_interval);
	free(timer);
}

int32 timerCreate()
{
	return (int32)malloc(sizeof(T_TIMER));
}

void timerDelete(int32 tm)
{

}