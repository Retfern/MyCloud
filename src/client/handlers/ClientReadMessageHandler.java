package client.handlers;

import client.auth.AuthException;
import common.handlers.OutSendFileHandler;
import common.message.*;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import swing.MainWindow;

public class ClientReadMessageHandler extends ChannelInboundHandlerAdapter {

    private String userName;
    private MainWindow parent;
    private InfoFileClass fileData;

    public ClientReadMessageHandler(MainWindow parent, InfoFileClass fileData) {
        this.parent = parent;
        this.fileData = fileData;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        AbstractMessage mes = (AbstractMessage) msg;
        if (mes instanceof ListFilesMessage) {
            ListFilesMessage listFilesMessage = (ListFilesMessage) mes;
            parent.getTableFiles().updateTable(listFilesMessage.getArr());
        } else if (mes instanceof CommandMessage) {
            CommandMessage command = (CommandMessage) mes;
            readCommandMessage(ctx, command);
        } else if (mes instanceof FileMessage) {
            FileMessage message = (FileMessage) mes;
            ctx.fireChannelRead(message);
        }
    }

    private void readCommandMessage(ChannelHandlerContext ctx, CommandMessage command) {
        if (command.getCommand() == CommandMessage.Command.DELETE) {
            tryDeleteFile(ctx, command);
        } else if (command.getCommand() == CommandMessage.Command.SET_USER_NAME) {
            System.out.println("Command.SET_USER_NAME " + command.getPath());
            userName = command.getPath();
        } else if (command.getCommand() == CommandMessage.Command.FILE_EXIST_TRUE) {
            tryToChangeFile(ctx);
        } else if (command.getCommand() == CommandMessage.Command.FILE_EXIST_FALSE) {
            String[][] arr = {{fileData.getFileName(), "Download..."}};
            parent.getTableFiles().updateTable(arr);
            changeHandlerForSendFile(ctx, fileData.getPath());
        } else if (command.getCommand() == CommandMessage.Command.FILE_DOWNLOAD_NEXT_PART) {
            ctx.channel().writeAndFlush(command);
        } else if (command.getCommand() == CommandMessage.Command.FILE_UPLOAD_COMPLETED) {
            fileUploadCompleted(ctx);
        }
    }

    private void fileUploadCompleted(ChannelHandlerContext ctx) {
        System.out.println("Загрузка файла завершена");
        parent.getTableFiles().removeFile(parent.searchEqualsFileName(fileData.getFileName()));
        String[][] arr = {{fileData.getFileName(), fileData.getSize() + " byte"}};
        parent.getTableFiles().updateTable(arr);
        ctx.channel().close();
    }

    private void tryToChangeFile(ChannelHandlerContext ctx) {
        Integer idx = parent.showMessageFileExist(fileData.getFileName());
        if (idx != null) {
            ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.DELETE, fileData.getFileName(), idx));
        } else {
            ctx.channel().close();
        }
    }

    private void tryDeleteFile(ChannelHandlerContext ctx, CommandMessage command) {
        if (command.isResult()) {
            if (command.getIdx() != null) {
                parent.getTableFiles().removeFile(command.getIdx());
            }
            if (fileData != null) {
                ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.ADD, fileData.getFileName(), fileData.getSize()));
            }
        } else {
            System.out.println("File delete false");
        }
    }

    public void changeHandlerForSendFile(ChannelHandlerContext ctx, String path) {
        ctx.channel().pipeline().addLast(new ChannelHandler[]{new OutSendFileHandler(path)});
        ctx.channel().writeAndFlush(new CommandMessage(CommandMessage.Command.FILE_DOWNLOAD_NEXT_PART, 0));
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof AuthException) {
            throw new AuthException();
        } else {
            parent.getLoginDialog().tryCloseLoginDialog(new Exception());
            ctx.close();
            cause.printStackTrace();
        }
    }
}
