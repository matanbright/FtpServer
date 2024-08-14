package com.matanbright.ftpserver.utilities;

import androidx.annotation.NonNull;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public final class NetworkUtilities {

    public static class UnableToGetIpEndpointStringException extends Exception {
        private static final String ERROR_MESSAGE = "Error: Unable to get IP endpoint string!";
        public UnableToGetIpEndpointStringException() {
            super(ERROR_MESSAGE);
        }
    }

    private static final String[] NETWORK_INTERFACE_NAMES_BY_SCAN_ORDER = {
        "eth.*",
        "wlan.*",
        ".*"
    };

    private NetworkUtilities() {}

    @NonNull
    public static ArrayList<InetAddress> getDeviceIpAddresses() {
        return getDeviceIpAddresses(false);
    }

    @NonNull
    public static ArrayList<InetAddress> getDeviceIpAddresses(boolean preferIpv6AddressesFirst) {
        ArrayList<InetAddress> deviceIpAddresses = new ArrayList<>();
        try {
            ArrayList<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            networkInterfaces.sort(Comparator.comparing(NetworkInterface::getName));
            networkInterfaces.sort((o1, o2) -> {
                try {
                    return ((o1.isLoopback() == o2.isLoopback()) ? 0 : (o1.isLoopback() ? 1 : -1));
                } catch (Exception ignored) {}
                return 0;
            });
            for (String networkInterfaceName : NETWORK_INTERFACE_NAMES_BY_SCAN_ORDER) {
                ArrayList<NetworkInterface> networkInterfacesCopy = new ArrayList<>(networkInterfaces);
                for (NetworkInterface networkInterface : networkInterfacesCopy) {
                    if (networkInterface.getName().matches(networkInterfaceName)) {
                        ArrayList<InetAddress> interfaceIpAddresses = Collections.list(networkInterface.getInetAddresses());
                        deviceIpAddresses.addAll(interfaceIpAddresses);
                        networkInterfaces.remove(networkInterface);
                    }
                }
            }
        } catch (Exception ignored) {}
        deviceIpAddresses.sort((o1, o2) -> {
            if (o1.getClass() == o2.getClass())
                return 0;
            if (o1.getClass() == Inet4Address.class && o2.getClass() == Inet6Address.class)
                return (preferIpv6AddressesFirst ? 1 : -1);
            if (o1.getClass() == Inet6Address.class && o2.getClass() == Inet4Address.class)
                return (preferIpv6AddressesFirst ? -1 : 1);
            if (o1.getClass() == Inet4Address.class || o1.getClass() == Inet6Address.class)
                return -1;
            if (o2.getClass() == Inet4Address.class || o2.getClass() == Inet6Address.class)
                return 1;
            return 0;
        });
        return deviceIpAddresses;
    }

    @NonNull
    public static String getIpEndpointString(@NonNull InetAddress ipAddress, int port) throws UnableToGetIpEndpointStringException {
        if (port < 0)
            throw new UnableToGetIpEndpointStringException();
        String ipAddressString = ipAddress.getHostAddress();
        if (ipAddressString == null)
            throw new UnableToGetIpEndpointStringException();
        if (ipAddress.getClass() == Inet4Address.class)
            return String.format("%1$s:%2$s", ipAddressString, port);
        if (ipAddress.getClass() == Inet6Address.class)
            return String.format("[%1$s]:%2$s", ipAddressString.split("%", 2)[0], port);
        throw new UnableToGetIpEndpointStringException();
    }
}
