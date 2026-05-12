package com.supermarket.common.core.constants;

public interface GlobalConstants {

    String TRACE_ID = "X-Trace-Id";
    String USER_ID = "X-User-Id";
    String USER_ROLES = "X-User-Roles";

    String REDIS_PREFIX = "smt:";

    int PAGE_NO_DEFAULT = 1;
    int PAGE_SIZE_DEFAULT = 20;
    int PAGE_SIZE_MAX = 100;
}
