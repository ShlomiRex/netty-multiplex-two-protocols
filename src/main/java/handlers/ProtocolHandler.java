package handlers;

import handlers.Http200OkHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderResultProvider;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpServerCodec;

public class ProtocolHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        in.retain();

        in.markReaderIndex();
        EmbeddedChannel ch = new EmbeddedChannel(new HttpRequestDecoder());
        assert ch.writeInbound(msg) : "failed to write inbound message";
        DecoderResultProvider httpRequest = ch.readInbound();
        in.resetReaderIndex();

        ChannelPipeline pipeline = ctx.pipeline();
        boolean failedToDecodeHttp = httpRequest.decoderResult().isFailure();
        pipeline.remove(this);

        ChannelPipeline nextPipe;
        if (failedToDecodeHttp) {
            nextPipe = pipeline.addLast(new TcpEchoHandler());
        } else {
            pipeline.addLast(new HttpServerCodec());
            nextPipe = pipeline.addLast(new Http200OkHandler());
        }
        nextPipe.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
