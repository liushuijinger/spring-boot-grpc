package com.shuijing.grpc.api;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author liushuijing
 * @date 2022/5/27
 */
@Data
@Accessors(chain = true)
public class User {
    private long id;
    private String name;
    private int age;
    private long createTime;
    private long updateTime;
}
