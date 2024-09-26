package server.handler;

import common.handlers.InputDownloadFileHandler;
import common.handlers.OutSendFileHandler;
import common.message.AbstractMessage;
import common.message.CommandMessage;
import common.message.FileMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;

public class ServerReadMessageHandler extends ChannelInboundHandlerAdapter {
    private final String SERVER_STORAGE = "server_storage/";
    private final int INIT_PART = 0;
    private String userName;

    public ServerReadMessageHandler(String userName) {
        this.userName = userName;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        AbstractMessage mes = (AbstractMessage) msg;
        if (mes instanceof CommandMessage) {
            CommandMessage command = (CommandMessage) mes;
            if (command.getCommand() == CommandMessage.Command.DELETE) {
                deleteFileOnServer(ctx, command);
            } else if (command.getCommand() == CommandMessage.Command.ADD) {
                changeHandlerForDownloadFile(ctx, command);
            } else if (command.getCommand() == CommandMessage.Command.DOWNLOAD) {
                changeHandlerForSendFile(ctx, command);
            } else if (command.getCommand() == CommandMessage.Command.STOP) {
                ctx.close();
            } else if (command.getCommand() == CommandMessage.Command.FILE_DOWNLOAD_NEXT_PART) {
                ctx.channel().writeAndFlush(command);
            }
        } else if (mes instanceof FileMessage) {
            FileMessage message = (FileMessage) mes;
            ctx.fireChannelRead(message);
        }
    }

    private void deleteFileOnServer(ChannelHandlerContext ctx, CommandMessage command) {
        boolean result = false;
        File file = new File(SERVER_STORAGE + userName + "/" + command.getPath());
        Integer idx = command.getIdx();
        while (file.exists()) {
            result = file.delete();
            System.out.println("File delete result " + result);
        }
        if (idx != null) {
            if (result) {
                ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.DELETE, true, idx));
            } else {
                ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.DELETE, false, idx));
            }
        }
    }

    private void changeHandlerForSendFile(ChannelHandlerContext ctx, CommandMessage command) {
        ctx.pipeline().addLast(new ChannelHandler[]{new OutSendFileHandler(SERVER_STORAGE + userName + "/" + command.getPath())});
        ctx.channel().writeAndFlush(new CommandMessage(CommandMessage.Command.FILE_DOWNLOAD_NEXT_PART, INIT_PART));
    }

    private void changeHandlerForDownloadFile(ChannelHandlerContext ctx, CommandMessage command) {
        File file = new File(SERVER_STORAGE + userName + "/" + command.getPath());
        if (file.exists()) {
            ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.FILE_EXIST_TRUE));
        } else {
            ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.FILE_EXIST_FALSE));
            ctx.pipeline().addLast(new ChannelHandler[]{new InputDownloadFileHandler(SERVER_STORAGE,
                                   command.getPath(), userName, command.getSize(), null)});
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
