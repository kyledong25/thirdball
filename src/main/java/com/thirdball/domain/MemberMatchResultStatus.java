package com.thirdball.domain;

/** Lifecycle of a member-submitted result before it becomes an official match. */
public enum MemberMatchResultStatus {
    PENDING,
    AGREED,
    DECLINED
}
