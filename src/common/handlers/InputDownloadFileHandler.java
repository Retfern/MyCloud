package common.handlers;

import common.message.CommandMessage;
import common.message.FileMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import swing.MainWindow;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class InputDownloadFileHandler extends ChannelInboundHandlerAdapter {

    private String pathFile;
    private String fileName;
    private String userName;
    private long size;
    private BufferedOutputStream out;
    private MainWindow parent;
    private final int PART_STOP = 100;

    public InputDownloadFileHandler(String pathFile, String fileName, String userName, long size, MainWindow parent) {

        this.fileName = fileName;
        this.pathFile = pathFile;
        this.userName = userName;
        this.size = size;
        this.parent = parent;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FileMessage message = (FileMessage) msg;
        out = new BufferedOutputStream(new FileOutputStream(pathFile + userName + "/" + fileName, true));
        out.write(message.data);
        out.flush();

        if (message.partNumber % PART_STOP == 0L) {
            ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.FILE_DOWNLOAD_NEXT_PART, message.partNumber));
        }
        if (message.partNumber == message.partsCount) {
            out.close();
            if (parent == null) {
                ctx.writeAndFlush(new CommandMessage(CommandMessage.Command.FILE_UPLOAD_COMPLETED));
            } else {
                parent.showMessageDownloadComplited(fileName);
            }
            ctx.close();
        }
        out.close();
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            System.out.println("=================================");
            System.out.println("==== IT is IOException ====");
        }
        if (cause instanceof FileNotFoundException) {
            System.out.println("Передача файла на сервер не завершена ");
            System.out.println("==== IT is FileNotFoundException ====");
        }
        out.close();
        ctx.close();
        cause.printStackTrace();
    }
}
