#add_library(checkSignature STATIC
#        integrity/v2sign.cc)
#target_compile_options(checkSignature PUBLIC
#        -Oz -Wall -Wextra -Wshadow -fvisibility=hidden)
#target_include_directories(checkSignature PUBLIC
#        integrity
#        ../../libs/linux-syscall-support)
#target_link_libraries(checkSignature
#    rust)

add_library(norust STATIC
        integrity/log_common.cc
        integrity/time_utils.cc
        integrity/remove_rust.cc
)

target_compile_options(norust PUBLIC -Oz -Wall -Wextra -Wshadow -fvisibility=hidden)
target_include_directories(norust PUBLIC integrity/include)
target_link_libraries(norust log)
