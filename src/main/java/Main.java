import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.util.RoundRobinLoadBalancerFactory;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import org.eclipse.jetty.alpn.ALPN;

import java.io.File;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ihaken on 6/8/17.
 */
public class Main {

    private static Server startServer() throws Exception {
        NettyServerBuilder builder = NettyServerBuilder.forAddress(new InetSocketAddress("localhost", 8980));

        // Generate cert/key with this command:
        //   openssl req -x509 -subj '/CN=localhost' -newkey rsa:2048 -keyout key.pem -out cert.pem -nodes -days 36500
        SslContext serverSslContext = GrpcSslContexts.forServer(
                new File("cert.pem"), new File("key.pem"))
                .sslProvider(SslProvider.JDK)
                .clientAuth(ClientAuth.NONE)
                .build();

        builder.sslContext(serverSslContext);

        Server grpcServer = builder.addService(new DummyService()).build();
        grpcServer.start();
        return grpcServer;
    }

    public static void runTest() throws Exception {

        final ManagedChannel channel = NettyChannelBuilder.forTarget("static://localhost:8980,localhost:16000")
                .usePlaintext(false)
                .sslContext(GrpcSslContexts.forClient()
                        .trustManager(new File("cert.pem"))
                        .build())
                .negotiationType(NegotiationType.TLS)
                .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
                .nameResolverFactory(new StaticNameResolverFactory())
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {

                        while (true) {
                            try {
                                ClientCalls.blockingUnaryCall(channel, DummyService.INVOKE_METHOD, CallOptions.DEFAULT, 37);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            });
        }

        while (true) {
            Field objectsField = ALPN.class.getDeclaredField("objects");
            objectsField.setAccessible(true);
            System.out.println(((Map<?, ?>) objectsField.get(null)).size());
            Thread.sleep(1000L);
        }
    }

    /** To run this test, you must launch with jetty-alpn added to the boot path, i.e.
     *   "java -Xbootclasspath/p:alpn-boot-8.1.11.v20170118.jar ..."
     *
     *  This test also assumes that 8980 is an available local port and that 16000 is a port on which
     *  nothing is listening (i.e. connections will be refused.
     *
     *  This test continually prints the number of objects in the ALPN.objects map. This test shows
     *  that the map continues to grow in size, which is to say that objects are not getting cleared
     *  out when connections to localhost:16000 fail.
     */
    public static void main(String[] args) throws Exception {
        Server server = startServer();
        runTest();
    }

}
