cmake_minimum_required(VERSION 3.22.1)

project("memscan")

add_library(
    memscan
    SHARED
    native-lib.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    memscan
    ${log-lib}
)
