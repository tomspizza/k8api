package com.tomspizza.k8api.util;


import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtilTest {

    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Test
    public void testGetAge() {
        String age = DateUtil.getAge("2021-08-06T21:15:08Z");
        System.out.println(age);
    }

    @Test
    public void testGetAge0() {
        String other = "2021-08-10T23:56:08Z";
        LocalDateTime otherTime = LocalDateTime.parse(other, dtf);

        String age = DateUtil.getAge("2021-08-06T21:15:08Z", otherTime);
        Assert.assertEquals("4d", age);
    }

    @Test
    public void testGetAge1() {
        String other = "2021-08-06T23:56:08Z";
        LocalDateTime otherTime = LocalDateTime.parse(other, dtf);

        String age = DateUtil.getAge("2021-08-06T21:15:08Z", otherTime);
        Assert.assertEquals("2h", age);
    }

    @Test
    public void testGetAge2() {
        String other = "2021-08-06T21:56:08Z";
        LocalDateTime otherTime = LocalDateTime.parse(other, dtf);

        String age = DateUtil.getAge("2021-08-06T21:15:08Z", otherTime);
        Assert.assertEquals("41m", age);
    }

    @Test
    public void testGetAge3() {
        String other = "2021-08-06T21:15:15Z";
        LocalDateTime otherTime = LocalDateTime.parse(other, dtf);

        String age = DateUtil.getAge("2021-08-06T21:15:08Z", otherTime);
        Assert.assertEquals("0m", age);
    }
}
