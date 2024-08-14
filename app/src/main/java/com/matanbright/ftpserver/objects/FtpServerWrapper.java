package com.matanbright.ftpserver.objects;

import android.os.Environment;

import androidx.annotation.Nullable;

import org.apache.ftpserver.ConnectionConfig;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfiguration;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.util.ArrayList;


public class FtpServerWrapper {

    public static class FtpUser {
        public static final String ANONYMOUS_USER_NAME = "anonymous";
        private final String name;
        private final String password;
        public FtpUser(String name, String password) {
            this.name = name;
            this.password = password;
        }
        public String getName() {
            return name;
        }
        public String getPassword() {
            return password;
        }
        public boolean isValid() {
            return ((name != null && !name.isEmpty()) &&
                (password != null && !password.isEmpty()));
        }
    }

    public static class FtpEncryption {
        private final String keystoreFilePath;
        private final String keyAlias;
        private final String keyPassword;
        private final boolean forceEncryption;
        public FtpEncryption(String keystoreFilePath,
                             String keyAlias,
                             String keyPassword,
                             boolean forceEncryption) {
            this.keystoreFilePath = keystoreFilePath;
            this.keyAlias = keyAlias;
            this.keyPassword = keyPassword;
            this.forceEncryption = forceEncryption;
        }
        public String getKeystoreFilePath() {
            return keystoreFilePath;
        }
        public String getKeyAlias() {
            return keyAlias;
        }
        public String getKeyPassword() {
            return keyPassword;
        }
        public boolean isForcingEncryption() {
            return forceEncryption;
        }
    }

    public static class UnableToCreateServerException extends Exception {
        private static final String ERROR_MESSAGE = "Error: Unable to create server!";
        public UnableToCreateServerException() {
            super(ERROR_MESSAGE);
        }
    }

    public static class UnableToStartServerException extends Exception {
        private static final String ERROR_MESSAGE = "Error: Unable to start server!";
        public UnableToStartServerException() {
            super(ERROR_MESSAGE);
        }
    }

    private static final String DEFAULT_FTP_SERVER_LISTENER_NAME = "default";

    private final FtpServer ftpServer;
    private final int port;
    private final @Nullable FtpUser ftpUser;
    private final @Nullable FtpEncryption ftpEncryption;
    private final boolean enableWriting;

    public FtpServerWrapper(int port,
                            @Nullable FtpUser ftpUser,
                            @Nullable FtpEncryption ftpEncryption,
                            boolean enableWriting) throws UnableToCreateServerException {
        ftpServer = createFtpServer(port, ftpUser, ftpEncryption, enableWriting);
        if (ftpServer == null)
            throw new UnableToCreateServerException();
        this.port = port;
        this.ftpUser = ftpUser;
        this.ftpEncryption = ftpEncryption;
        this.enableWriting = enableWriting;
    }

    public int getPort() {
        return port;
    }

    @Nullable
    public FtpUser getFtpUser() {
        return ftpUser;
    }

    @Nullable
    public FtpEncryption getFtpEncryption() {
        return ftpEncryption;
    }

    public boolean isWritingEnabled() {
        return enableWriting;
    }

    public void start() throws UnableToStartServerException {
        try {
            ftpServer.start();
        } catch (Exception e) {
            throw new UnableToStartServerException();
        }
    }

    public void stop() {
        ftpServer.stop();
    }

    @Nullable
    private static FtpServer createFtpServer(int port,
                                             @Nullable FtpUser ftpUser,
                                             @Nullable FtpEncryption ftpEncryption,
                                             boolean enableWriting) {
        if (port == 0 || (ftpUser != null && !ftpUser.isValid()))
            return null;
        try {
            ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
            connectionConfigFactory.setAnonymousLoginEnabled(true);
            ConnectionConfig connectionConfig = connectionConfigFactory.createConnectionConfig();
            ListenerFactory listenerFactory = new ListenerFactory();
            listenerFactory.setPort(port);
            if (ftpEncryption != null) {
                SslConfigurationFactory sslConfigurationFactory = new SslConfigurationFactory();
                sslConfigurationFactory.setKeystoreFile(new File(ftpEncryption.getKeystoreFilePath()));
                sslConfigurationFactory.setKeystorePassword("");
                sslConfigurationFactory.setKeyAlias(ftpEncryption.getKeyAlias());
                sslConfigurationFactory.setKeyPassword(ftpEncryption.getKeyPassword());
                SslConfiguration sslConfiguration = sslConfigurationFactory.createSslConfiguration();
                listenerFactory.setSslConfiguration(sslConfiguration);
                listenerFactory.setImplicitSsl(ftpEncryption.isForcingEncryption());
            }
            Listener listener = listenerFactory.createListener();
            BaseUser baseUser = new BaseUser();
            baseUser.setName((ftpUser == null) ? FtpUser.ANONYMOUS_USER_NAME : ftpUser.getName());
            if (ftpUser != null)
                baseUser.setPassword(ftpUser.getPassword());
            baseUser.setHomeDirectory(Environment.getExternalStorageDirectory().getPath());
            if (enableWriting) {
                ArrayList<Authority> authorities = new ArrayList<>();
                authorities.add(new WritePermission());
                baseUser.setAuthorities(authorities);
            }
            FtpServerFactory ftpServerFactory = new FtpServerFactory();
            ftpServerFactory.setConnectionConfig(connectionConfig);
            ftpServerFactory.addListener(DEFAULT_FTP_SERVER_LISTENER_NAME, listener);
            ftpServerFactory.getUserManager().save(baseUser);
            return ftpServerFactory.createServer();
        } catch (Exception e) {
            return null;
        }
    }
}
