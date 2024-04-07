//
// Created by sulfate on 2024-03-27.
//
#include "include/rust.h"

#include "log_common.h"

#include <string>

using mmelfloader::utils::Log;

static uint8_t common_log_handler(Log::Level level, const char *tag,
                                  const char *msg) {
    std::string tag_str;
    if (tag != nullptr) {
        tag_str = tag;
    } else {
        tag_str = "[null]";
    }
    std::string msg_str;
    if (msg != nullptr) {
        msg_str = msg;
    }
    Log::LogMessage(level, tag_str, msg_str);
    return 0;
}

uint8_t logd(const char *tag, const char *msg) {
    return common_log_handler(Log::Level::kDebug, tag, msg);
}

uint8_t logi(const char *tag, const char *msg) {
    return common_log_handler(Log::Level::kInfo, tag, msg);
}

uint8_t logw(const char *tag, const char *msg) {
    return common_log_handler(Log::Level::kWarn, tag, msg);
}

uint8_t loge(const char *tag, const char *msg) {
    return common_log_handler(Log::Level::kError, tag, msg);
}
