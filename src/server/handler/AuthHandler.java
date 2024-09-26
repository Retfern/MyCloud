package server.handler;

import client.auth.AuthServiceImpl;
import common.message.*;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private String userName;

    public AuthHandler() {

    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        AbstractMessage mes = (AbstractMessage) msg;
        if (mes instanceof NewChanelForSendFileMessage) {
            userName = ((NewChanelForSendFileMessage) mes).getUserName();
            changePipeline(ctx);
        } else if (mes instanceof AuthMessage) {
            AuthMessage authMsg = (AuthMessage) mes;
            userAuthorization(ctx, authMsg);
        }
    }

    private void userAuthorization(ChannelHandlerContext ctx, AuthMessage authMsg) throws Exception {
        AuthServiceImpl authService = new AuthServiceImpl();
        if (authService.authUser(authMsg.getUserName(), authMsg.getPassword())) {
            userName = authMsg.getUserName();
            ctx.writeAndFlush(new AuthMessage(true, userName));
            ctx.writeAndFlush(new ListFilesMessage(userName));
            authService.close();
            changePipeline(ctx);
        } else {
            ctx.writeAndFlush(new AuthMessage(false, userName));
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }

    private void changePipeline(ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(new ChannelHandler[]{new ServerReadMessageHandler(userName)});
        ctx.pipeline().remove(this.getClass());
    }
}
