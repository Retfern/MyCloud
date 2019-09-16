package common.handlers;

import common.message.CommandMessage;
import common.message.FileMessage;
import io.netty.channel.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class OutSendFileHandler extends ChannelOutboundHandlerAdapter {
    private final int BUFF_SIZE = 10485760;
    private final int PART_STOP = 100;
    private String pathFile;
    private long position;

    public OutSendFileHandler(String pathFile) {
        this.pathFile = pathFile;
        position = 0;
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof CommandMessage) {
            CommandMessage message = (CommandMessage) msg;
            File file = new File(pathFile);
            if (message.getCurrentSize()!= null) {position = message.getCurrentSize();}
            int partsCount = (new Long((file.length() - position) / (long) BUFF_SIZE)).intValue();
            if (file.length() % (long) BUFF_SIZE != 0L) {++partsCount;}
            FileMessage fmOut = new FileMessage(pathFile, -1, partsCount, new byte[BUFF_SIZE]);
            RandomAccessFile inRnd = new RandomAccessFile(file, "r");
            int i = message.getIdx();
            while (i < partsCount) {
                if (partsCount == 1 || i == partsCount - 1) {
                    if (i==0 && position!=0){inRnd.seek(position);}
                    int readedBytes = inRnd.read(fmOut.data);
                    if (readedBytes < BUFF_SIZE) {
                        fmOut.data = Arrays.copyOfRange(fmOut.data, 0, readedBytes);
                    }
                } else if (i == 0 && partsCount > 1) {
                    if (position!=0){inRnd.seek(position);}
                    System.out.println("inRnd.getFilePointer() " + inRnd.getFilePointer());
                    inRnd.readFully(fmOut.data);
                } else {
                    inRnd.seek((long) BUFF_SIZE * i + position);
                    inRnd.readFully(fmOut.data);
                }
                fmOut.partNumber = i + 1;
                ctx.writeAndFlush(fmOut);
                i++;
                if (i % PART_STOP == 0L) {
                    break;
                }
            }
            inRnd.close();
        } else {
            System.out.println("It is not FILE_DOWNLOAD_NEXT_PART");
        }
    }
}
