package com.shuijing.grpc.client;

import com.shuijing.grpc.api.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * @author liushuijing
 * @date 2022/5/27
 */
@RestController
public class GrpcController {

    @Autowired
    private GrpcClientService service;

    @GetMapping
    public String hello(String message) {
        return service.hello(message);
    }

    @GetMapping("/users/{userId}")
    public User getUser(@PathVariable("userId") Long userId) throws IOException {
        return service.getUser(userId);
    }

    @GetMapping("/users/{name}:search")
    public List<User> queryUser(@PathVariable("name") String name) throws IOException {
        return service.queryUser(name);
    }

    @PutMapping("/users")
    public String updateUser() {
        return service.updateUser();
    }

    @PutMapping("/users:process")
    public List<User> processUser() {
        return service.processUser();
    }
}
