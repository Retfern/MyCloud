package client.handlers;
import client.auth.AuthException;
import common.message.AbstractMessage;
import common.message.AuthMessage;
import common.message.CommandMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import swing.MainWindow;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private String userName;
    private MainWindow parent;

    public AuthHandler(MainWindow parent) {
        this.parent = parent;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        AbstractMessage mes = (AbstractMessage)msg;
        if (mes instanceof AuthMessage) {
            AuthMessage authMsg = (AuthMessage) mes;
            if (!authMsg.isAuthSuccessfull()) {
                parent.getLoginDialog().setConnected(false);
                parent.getLoginDialog().tryCloseLoginDialog(new AuthException());
            }else {
                parent.getLoginDialog().setConnected(true);
                parent.getLoginDialog().tryCloseLoginDialog(null);
                userName = authMsg.getUserName();
                parent.setUserName(userName);
                ctx.fireChannelRead(new CommandMessage (CommandMessage.Command.SET_USER_NAME, userName));
                changePipeline(ctx);
            }
        }
    }

    private void changePipeline(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this.getClass());
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        cause.printStackTrace();
    }
}
