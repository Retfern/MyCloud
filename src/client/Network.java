package client;
import client.handlers.AuthHandler;
import client.handlers.ClientReadMessageHandler;
import common.message.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import common.handlers.InputDownloadFileHandler;
import swing.MainWindow;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class Network {
    private Channel currentChannel;
    private MainWindow parent;
    private InfoFileClass fileData;
    private Thread thread;
    private final String CLIENT_STORAGE = "client_storage/";
    private final int MAX_OBJECT_SIZE = 104857600;
    private final int PORT = 8188;
    private final String HOST_NAME = "localhost";

    public Network(MainWindow parent) {
        this.parent = parent;
        networkThreadStart();
    }

    public Network(MainWindow parent, InfoFileClass fileData) {
        this.parent = parent;
        this.fileData = fileData;
        networkThreadStart();
    }

    private void networkThreadStart () {
        CountDownLatch networkStarter = new CountDownLatch(1);

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Сетевое подключение открыто");
                start(networkStarter);
                if (fileData!=null) {
                    parent.getListDownload().remove(fileData.getFileName());
                    System.out.println("remove network from listDownload");
                }
                System.out.println("Сетевое подключение закрыто");
            }
        });

        thread.start();

        try {
            networkStarter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public InfoFileClass getFileData () {
        return fileData;
    }

    public Thread getThread  () {
        return thread;
    }

    public Channel getCurrentChannel() {
        return this.currentChannel;
    }

    public void start(CountDownLatch countDownLatch) {
        NioEventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group);
            clientBootstrap.channel(NioSocketChannel.class);
            clientBootstrap.remoteAddress(new InetSocketAddress(HOST_NAME, PORT));
            clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(new ChannelHandler[]{
                              new ObjectDecoder(MAX_OBJECT_SIZE, ClassResolvers.cacheDisabled((ClassLoader) null)),
                              new ObjectEncoder(),
                              new AuthHandler(parent),
                              new ClientReadMessageHandler(parent, fileData)});
                    Network.this.currentChannel = socketChannel;
                }
            });
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            countDownLatch.countDown();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception var13) {
            var13.printStackTrace();
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException var12) {
                var12.printStackTrace();
            }
        }
    }

    public void stop() {
        getCurrentChannel().writeAndFlush(new CommandMessage(CommandMessage.Command.STOP));
        this.currentChannel.close();

    }

    public void changeHandlerForDownloadFile(String userName, String path, long size) {
        this.currentChannel.pipeline().addLast(new ChannelHandler[]{new InputDownloadFileHandler(CLIENT_STORAGE, path, userName, size, parent)});
    }

    public void changePipeline() {
        this.currentChannel.pipeline().remove(AuthHandler.class);
    }

    public void sendMessage (AbstractMessage message) {
        getCurrentChannel().writeAndFlush(message);
    }

}

