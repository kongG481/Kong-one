package com.example;

import com.example.utils.Md5Util;

public class TestEncrypt {
    public static void main(String[] args) {
        System.out.println("admin123加密后：" + Md5Util.encrypt("admin123"));
        System.out.println("123456加密后：" + Md5Util.encrypt("123456"));
        System.out.println("111111加密后：" + Md5Util.encrypt("111111"));
        System.out.println("zhangsan123加密后：" + Md5Util.encrypt("zhangsan123"));
        System.out.println("lisi123加密后：" + Md5Util.encrypt("lisi123"));
        System.out.println("wangwu123加密后：" + Md5Util.encrypt("wangwu123"));
    }
}
