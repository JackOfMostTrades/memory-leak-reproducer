import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class DummyService implements BindableService {

    public static class IntMarshaller implements MethodDescriptor.Marshaller<Integer> {
        public InputStream stream(Integer value) {
            return new ByteArrayInputStream(value.toString().getBytes());
        }
        public Integer parse(InputStream stream) {
            java.util.Scanner s = new java.util.Scanner(stream).useDelimiter("\\A");
            return Integer.parseInt(s.hasNext() ? s.next() : "");
        }
    }
    private static IntMarshaller INT_MARSHALLER = new IntMarshaller();

    public static MethodDescriptor<Integer, Integer> INVOKE_METHOD =
            MethodDescriptor.create(MethodDescriptor.MethodType.UNARY,
                    MethodDescriptor.generateFullMethodName("DummyService", "Invoke"),
                    INT_MARSHALLER,
                    INT_MARSHALLER);

    public ServerServiceDefinition bindService() {
        return ServerServiceDefinition.builder("DummyService")
                .addMethod(INVOKE_METHOD,
                        ServerCalls.asyncUnaryCall(new ServerCalls.UnaryMethod<Integer, Integer>() {
                            @Override
                            public void invoke(Integer request, StreamObserver<Integer> responseObserver) {
                                responseObserver.onNext(42);
                                responseObserver.onCompleted();
                            }
                        }))
                .build();
    }
}

