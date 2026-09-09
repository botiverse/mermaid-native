#include <stdbool.h>

bool com_tencent_kuikly_IsCurrentOnContextThread(const char* pagerId) {
    return true;
}

void com_tencent_kuikly_ScheduleContextTask(const char* pagerId, void (*onSchedule)(const char* pagerId)) {
    if (onSchedule) {
        onSchedule(pagerId);
    }
}
