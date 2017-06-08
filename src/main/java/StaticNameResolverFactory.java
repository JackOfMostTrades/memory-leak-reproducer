import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ihaken on 6/8/17.
 */

public class StaticNameResolverFactory extends NameResolver.Factory {
    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        final String[] addresses = targetUri.toString().substring(targetUri.getScheme().length() + 3).split(",");
        List<InetSocketAddress> socketAddresses = new ArrayList<>(addresses.length);
        for (String address : addresses) {
            String[] ipPort = address.split(":", 2);
            socketAddresses.add(new InetSocketAddress(ipPort[0], Integer.parseInt(ipPort[1])));
        }
        final String authority = socketAddresses.get(0).getHostString();

        return new NameResolver() {
            @Override
            public String getServiceAuthority() {
                return authority;
            }

            @Override
            public void start(Listener listener) {
                List<EquivalentAddressGroup> equivalentAddressGroups = new ArrayList<>(socketAddresses.size());
                for (InetSocketAddress address : socketAddresses) {
                    equivalentAddressGroups.add(new EquivalentAddressGroup(Arrays.asList(address), Attributes.EMPTY));
                }
                listener.onAddresses(equivalentAddressGroups, Attributes.EMPTY);
            }

            @Override
            public void shutdown() {
            }
        };
    }

    @Override
    public String getDefaultScheme() {
        return "static";
    }
}
