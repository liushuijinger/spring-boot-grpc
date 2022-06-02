package com.shuijing.grpc.server;

import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.shuijing.grpc.api.HelloGRPCServiceGrpc;
import com.shuijing.grpc.api.ProtobufBeanUtil;
import com.shuijing.grpc.api.User;
import com.shuijing.grpc.api.UserGrpc;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author liushuijing
 * @date 2022/5/27
 */
@Slf4j
@GrpcService
public class GrpcServerServiceImpl extends HelloGRPCServiceGrpc.HelloGRPCServiceImplBase {

    private UserGrpc user1 = UserGrpc.newBuilder().setId(1L)
            .setAge(18).setName("xiaoliu").setCreateTime(System.currentTimeMillis()).build();
    private UserGrpc user2 = UserGrpc.newBuilder().setId(2L)
            .setAge(18).setName("xiaoshui").setCreateTime(System.currentTimeMillis()).build();
    private UserGrpc user3 = UserGrpc.newBuilder().setId(3L)
            .setAge(18).setName("xiaojing").setCreateTime(System.currentTimeMillis()).build();
    private UserGrpc user4 = UserGrpc.newBuilder().setId(4L)
            .setAge(18).setName("shuijing").setCreateTime(System.currentTimeMillis()).build();

    private Map<Long, UserGrpc> userMap = Stream.of(
            new HashMap.SimpleEntry<>(user1.getId(), user1),
            new HashMap.SimpleEntry<>(user2.getId(), user2),
            new HashMap.SimpleEntry<>(user3.getId(), user3),
            new HashMap.SimpleEntry<>(user4.getId(), user4))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    @Override
    public void getUser(Int64Value request, StreamObserver<UserGrpc> responseObserver) {
        User user = new User()
                .setId(request.getValue())
                .setAge(18).setName("hhh")
                .setCreateTime(System.currentTimeMillis())
                .setUpdateTime(System.currentTimeMillis());

        log.info("gRPC Server user: {}", user);

        UserGrpc userGrpc = null;
        try {
            userGrpc = ProtobufBeanUtil.toProtoBean(UserGrpc.newBuilder(), user);
//            Thread.sleep(1100L);
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseObserver.onNext(userGrpc);
        responseObserver.onCompleted();
    }

    @Override
    public void sayHello(StringValue request, StreamObserver<StringValue> responseObserver) {
        log.info("gRPC Server request: {}", request);
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }

    @SneakyThrows
    @Override
    public void searchUsers(StringValue request, StreamObserver<UserGrpc> responseObserver) {

        for (Map.Entry<Long, UserGrpc> userEntry : userMap.entrySet()) {
            UserGrpc userGrpc = userEntry.getValue();
            String name = userGrpc.getName();
            if (name.contains(request.getValue())) {
                log.info("name found " + name);
                Thread.sleep(5000L);
                responseObserver.onNext(userGrpc);
            }
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<UserGrpc> updateUsers(StreamObserver<StringValue> responseObserver) {
        return new StreamObserver<UserGrpc>() {
            StringBuilder updatedUserStrBuilder = new StringBuilder().append("Updated User IDs : ");

            @Override
            public void onNext(UserGrpc value) {
                if (value != null) {
                    userMap.put(value.getId(), value);
                    updatedUserStrBuilder.append(value.getId()).append(", ");
                    log.info("User ID : " + value.getId() + " - Updated");
                }
            }

            @Override
            public void onError(Throwable t) {
                log.info("User ID update error " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                log.info("Update users - Completed");
                StringValue updatedUsers = StringValue.newBuilder().setValue(updatedUserStrBuilder.toString()).build();
                responseObserver.onNext(updatedUsers);
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Int64Value> processUsers(StreamObserver<UserGrpc> responseObserver) {

        return new StreamObserver<Int64Value>() {
            @Override
            public void onNext(Int64Value value) {
                log.info("User Proc : ID - " + value.getValue());
                UserGrpc userGrpc = userMap.get(value.getValue());
                if (userGrpc == null) {
                    log.info("No User found. ID - " + value.getValue());
                    return;
                }

                log.info("user age : {}", userGrpc.getAge());
                userGrpc = userGrpc.toBuilder().setAge(userGrpc.getAge() + 1).build();

                responseObserver.onNext(userGrpc);

            }

            @Override
            public void onError(Throwable t) {
                log.info(" error " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }

        };
    }
}
