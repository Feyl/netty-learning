package com.feyl.netty.advance.chat_room.server;

import com.feyl.netty.advance.chat_room.protocol.MessageCodecSharable;
import com.feyl.netty.advance.chat_room.protocol.ProtocolFrameDecoder;
import com.feyl.netty.advance.chat_room.server.handler.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Feyl
 */
@Slf4j
public class ChatServer {
    public static void main(String[] args) {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        LoggingHandler LOGGING_HANDLER = new LoggingHandler(LogLevel.DEBUG);
        MessageCodecSharable MESSAGE_CODEC = new MessageCodecSharable();
        LoginRequestMessageHandler LOGIN_HANDLER = new LoginRequestMessageHandler();
        ChatRequestMessageHandler CHAT_HANDLER = new ChatRequestMessageHandler();
        GroupCreateRequestMessageHandler GROUP_CREATE_HANDLER = new GroupCreateRequestMessageHandler();
        GroupJoinRequestMessageHandler GROUP_JOIN_HANDLER = new GroupJoinRequestMessageHandler();
        GroupMembersRequestMessageHandler GROUP_MEMBERS_HANDLER = new GroupMembersRequestMessageHandler();
        GroupExitRequestMessageHandler GROUP_EXIT_HANDLER = new GroupExitRequestMessageHandler();
        GroupChatRequestMessageHandler GROUP_CHAT_HANDLER = new GroupChatRequestMessageHandler();
        QuitHandler QUIT_HANDLER = new QuitHandler();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel sc) throws Exception {
                    sc.pipeline().addLast(new ProtocolFrameDecoder());
                    sc.pipeline().addLast(LOGGING_HANDLER);
                    sc.pipeline().addLast(MESSAGE_CODEC);
                    // ????????????????????? ??????????????????????????? ?????????????????????
                    // 5s ????????????????????? channel ??????????????????????????? IdleState#READER_IDLE ??????
                    sc.pipeline().addLast(new IdleStateHandler(5, 0, 0));
                    // ChannelDuplexHandler ??????????????????????????????????????????
                    sc.pipeline().addLast(new ChannelDuplexHandler(){
                        // ????????????????????????
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                            // ????????????????????????
                            IdleStateEvent event = (IdleStateEvent) evt;
                            if (event.state() == IdleState.READER_IDLE) {
                                log.debug("?????? 5s ?????????????????????");
                                ctx.channel().close();
                            }
                        }
                    });
                    sc.pipeline().addLast(LOGGING_HANDLER);
                    sc.pipeline().addLast(CHAT_HANDLER);
                    sc.pipeline().addLast(GROUP_CREATE_HANDLER);
                    sc.pipeline().addLast(GROUP_JOIN_HANDLER);
                    sc.pipeline().addLast(GROUP_MEMBERS_HANDLER);
                    sc.pipeline().addLast(GROUP_EXIT_HANDLER);
                    sc.pipeline().addLast(GROUP_CHAT_HANDLER);
                    sc.pipeline().addLast(QUIT_HANDLER);
                }
            });
            Channel channel = serverBootstrap.bind(8080).sync().channel();
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
            log.error("server error", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }

    }
}
