// SPDX-FileCopyrightText: 2024 ACh Sulfate
// SPDX-License-Identifier: Apache-2.0
//
// Created by sulfate on 2024-02-23.
//

#include "log_common.h"

#include <cstring>
#include <cstdlib>
#include <unistd.h>

#include "time_utils.h"

#ifdef __ANDROID__
#include <android/log.h>
#include <android/set_abort_message.h>
#endif

namespace mmelfloader::utils {


#ifdef __ANDROID__

static void DefaultLogToAndroidLogHandler(Log::Level level, std::string_view tag, std::string_view msg) noexcept;

constinit volatile Log::LogHandler Log::sLogHandler = &DefaultLogToAndroidLogHandler;

static void DefaultLogToAndroidLogHandler(Log::Level level, std::string_view tag, std::string_view msg) noexcept {
    int prio;
    switch (level) {
        using Level = Log::Level;
        case Level::kVerbose:
            prio = ANDROID_LOG_VERBOSE;
            break;
        case Level::kDebug:
            prio = ANDROID_LOG_DEBUG;
            break;
        case Level::kInfo:
            prio = ANDROID_LOG_INFO;
            break;
        case Level::kNotice:
            prio = ANDROID_LOG_INFO;
            break;
        case Level::kWarn:
            prio = ANDROID_LOG_WARN;
            break;
        case Level::kError:
            prio = ANDROID_LOG_ERROR;
            break;
        default:
            prio = ANDROID_LOG_UNKNOWN;
            break;
    }
    __android_log_write(prio, tag.data(), msg.data());
}

#else

static void DefaultLogToStderrHandler(Log::Level level, std::string_view tag, std::string_view msg) noexcept;

constinit volatile Log::LogHandler Log::sLogHandler = &DefaultLogToStderrHandler;

static void DefaultLogToStderrHandler(Log::Level level, std::string_view tag, std::string_view msg) noexcept {
    auto time = GetCurrentLocalDateTime();
    char levelChar;
    switch (level) {
        using Level = Log::Level;
        case Level::kVerbose:
            levelChar = 'V';
            break;
        case Level::kDebug:
            levelChar = 'D';
            break;
        case Level::kInfo:
            levelChar = 'I';
            break;
        case Level::kNotice:
            levelChar = 'N';
            break;
        case Level::kWarn:
            levelChar = 'W';
            break;
        case Level::kError:
            levelChar = 'E';
            break;
        default:
            levelChar = '?';
            break;
    }
    // format
    // MM-dd HH:mm:ss.SSSSSS L TAG MSG
    auto line = fmt::format("{:02}-{:02} {:02}:{:02}:{:02}.{:03}{:03} {} {}: {}\n",
                            time.month, time.day, time.hour, time.minute, time.second, time.millisecond,
                            time.microsecond, levelChar, tag, msg);
    write(STDERR_FILENO, line.data(), line.size());
    // ignore error
}

#endif

[[noreturn]] void Abort(std::string_view msg) noexcept {
    if (!msg.empty()) {
        const char oomWhenAborting[] = "Out of memory when trying to allocate memory for abort message.";
        auto* buf = reinterpret_cast<char*>(malloc(msg.size() + 1));
        size_t len;
        if (buf == nullptr) {
            len = sizeof(oomWhenAborting);
            buf = const_cast<char*>(oomWhenAborting);
        } else {
            len = msg.size();
            std::memcpy(buf, msg.data(), len);
            buf[len] = '\0';
        }
#ifdef __ANDROID__
        __android_log_write(ANDROID_LOG_FATAL, "DEBUG", buf);
        android_set_abort_message(buf);
#else
        ::write(STDERR_FILENO, buf, len);
#endif
    }
    ::abort();
}

} // namespace mmelfloader::utils
