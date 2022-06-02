package com.shuijing.grpc.client;

import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.shuijing.grpc.api.HelloGRPCServiceGrpc;
import com.shuijing.grpc.api.ProtobufBeanUtil;
import com.shuijing.grpc.api.User;
import com.shuijing.grpc.api.UserGrpc;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author liushuijing
 * @date 2022/5/27
 */
@Slf4j
@Service
public class GrpcClientService {

    @GrpcClient("grpc-server")
    private HelloGRPCServiceGrpc.HelloGRPCServiceBlockingStub stub;

    @GrpcClient("grpc-server")
    private HelloGRPCServiceGrpc.HelloGRPCServiceStub asyncStub;

    public String hello(String message) {
        StringValue response = stub.sayHello(StringValue.of(message));
        return response.getValue();
    }

    public User getUser(Long id) throws IOException {
        final UserGrpc userGrpc = stub.getUser(Int64Value.of(id));
//        final UserGrpc userGrpc = stub.withDeadlineAfter(1000L, TimeUnit.MILLISECONDS).getUser(Int64Value.of(id));

        return ProtobufBeanUtil.toPojoBean(User.class, userGrpc);
    }

    public List<User> queryUser(String name) throws IOException {
        final Iterator<UserGrpc> userGrpcIterator = stub.searchUsers(StringValue.of(name));

        List<User> list = new ArrayList<>();
        while (userGrpcIterator.hasNext()) {
            final User user = ProtobufBeanUtil.toPojoBean(User.class, userGrpcIterator.next());
            list.add(user);
            log.info(user.toString());
        }
        return list;
    }

    @SneakyThrows
    public String updateUser() {

        UserGrpc user1 = UserGrpc.newBuilder().setId(1L)
                .setAge(18).setName("xiaoliu").setCreateTime(System.currentTimeMillis()).build();
        UserGrpc user2 = UserGrpc.newBuilder().setId(2L)
                .setAge(18).setName("xiaoshui").setCreateTime(System.currentTimeMillis()).build();
        UserGrpc user3 = UserGrpc.newBuilder().setId(3L)
                .setAge(18).setName("xiaojing").setCreateTime(System.currentTimeMillis()).build();
        UserGrpc user4 = UserGrpc.newBuilder().setId(4L)
                .setAge(18).setName("shuijing").setCreateTime(System.currentTimeMillis()).build();

        final CountDownLatch finishLatch = new CountDownLatch(1);

        StringBuilder builder = new StringBuilder();

        StreamObserver<StringValue> updateUserResponseObserver = new StreamObserver<StringValue>() {
            @Override
            public void onNext(StringValue value) {
                log.info("Update Users Res : " + value.getValue());
                builder.append(value.getValue());
            }

            @Override
            public void onError(Throwable t) {
                log.error("client error");
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("Update users response  completed!");
                finishLatch.countDown();
            }
        };

        StreamObserver<UserGrpc> updateUserRequestObserver = asyncStub.updateUsers(updateUserResponseObserver);

        final List<UserGrpc> userGrpcList = Stream.of(user1, user2, user3, user4).collect(Collectors.toList());

        for (UserGrpc userGrpc : userGrpcList) {
            updateUserRequestObserver.onNext(userGrpc);
            if ("xiaoshui".equals(userGrpc.getName())) {
                ((ClientCallStreamObserver) updateUserRequestObserver).cancel("", new RuntimeException());
                log.info("client cancel");
                break;
            }
            Thread.sleep(2000L);
        }

        if (finishLatch.getCount() == 0) {
            log.info("RPC completed or errored before we finished sending.");
            return builder.toString();
        }
        updateUserRequestObserver.onCompleted();

        if (!finishLatch.await(10, TimeUnit.SECONDS)) {
            log.info("FAILED : Process users cannot finish within 10 seconds");
        }

        return builder.toString();
    }

    @SneakyThrows
    public List<User> processUser() {

        final CountDownLatch finishLatch = new CountDownLatch(1);

        List<User> userList = new ArrayList<>();

        StreamObserver<UserGrpc> userProcessResponseObserver = new StreamObserver<UserGrpc>() {
            @SneakyThrows
            @Override
            public void onNext(UserGrpc value) {
                log.info("User id : {}, User age : {}", value.getId(), value.getAge());
                final User user = ProtobufBeanUtil.toPojoBean(User.class, value);
                userList.add(user);
            }

            @Override
            public void onError(Throwable t) {
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.info("User Processing completed!");
                finishLatch.countDown();
            }
        };

        StreamObserver<Int64Value> userProcessRequestObserver = asyncStub.processUsers(userProcessResponseObserver);

        final List<Int64Value> list = Stream.of(Int64Value.newBuilder().setValue(1L).build(),
                Int64Value.newBuilder().setValue(2L).build(),
                Int64Value.newBuilder().setValue(3L).build(),
                Int64Value.newBuilder().setValue(4L).build())
                .collect(Collectors.toList());

        for (Int64Value id : list) {
            userProcessRequestObserver.onNext(id);
            Thread.sleep(2000L);
        }

        if (finishLatch.getCount() == 0) {
            log.info("RPC completed or errored before we finished sending.");
            return userList;
        }
        userProcessRequestObserver.onCompleted();

        if (!finishLatch.await(60, TimeUnit.SECONDS)) {
            log.info("FAILED : Process users cannot finish within 60 seconds");
        }

        return userList;
    }
}
