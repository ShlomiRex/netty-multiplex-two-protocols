import handlers.Http200OkHandler;
import handlers.ProtocolHandler;
import handlers.TcpEchoHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

public class MainTest {

  private final static int PORT = 8080;
  private final static String HOST = "localhost";

  @BeforeClass
  public static void testEntireServer() throws InterruptedException {
    new Main().start();
  }

  @Test
  public void testEchoTCP() throws IOException {
    String[] cmd = {"/bin/sh", "-c", String.format("echo 'Hello World!' | nc %s %s", HOST, PORT)};
    Process process = Runtime.getRuntime().exec(cmd);
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    StringBuilder bufferedOutput = new StringBuilder();
    String s;
    while ((s = reader.readLine()) != null) {
      bufferedOutput.append(s);
    }
    assertEquals("Hello World!", bufferedOutput.toString());
  }

  @Test
  public void testHTTP200OK() throws IOException {
    String[] cmd = {"/bin/sh", "-c", String.format("curl -i %s:%s", HOST, PORT)};
    Process process = Runtime.getRuntime().exec(cmd);
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    StringBuilder bufferedOutput = new StringBuilder();
    String s;
    while ((s = reader.readLine()) != null) {
      bufferedOutput.append(s);
    }
    assertEquals("HTTP/1.0 200 OK", bufferedOutput.toString());
  }

  @Test
  public void testEchoTCPHandler() {
    EmbeddedChannel embeddedChannel =
            new EmbeddedChannel(new TcpEchoHandler());
    String content = "Hello World!";
    embeddedChannel.writeInbound(content);
    String outbound = embeddedChannel.readOutbound();
    //assertThat(outbound).isEqualTo(content);
    assertEquals(outbound, content);
    embeddedChannel.close();
  }

  @Test
  public void testHTTP200OKHandler() {
    EmbeddedChannel embeddedChannel =
            new EmbeddedChannel(new Http200OkHandler());
    DefaultFullHttpRequest httpRequest =
            new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
    embeddedChannel.writeInbound(httpRequest);
    HttpResponse response = embeddedChannel.readOutbound();
    //assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
    assertEquals(response.status(), HttpResponseStatus.OK);
    embeddedChannel.close();
  }

  @Test
  public void testProtocolHandlerHTTP() {
    DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
    EmbeddedChannel ch = new EmbeddedChannel(new HttpRequestEncoder());
    ch.writeOutbound(httpRequest);
    ByteBuf encoded = ch.readOutbound();

    EmbeddedChannel embeddedChannel = new EmbeddedChannel(new ProtocolHandler());
    embeddedChannel.writeInbound(encoded);
    Object response = embeddedChannel.readOutbound();

    EmbeddedChannel ch2 = new EmbeddedChannel(new HttpResponseDecoder());
    ch2.writeInbound(response);
    HttpResponse decodedResult = ch2.readInbound();

    assertEquals(decodedResult.status(), HttpResponseStatus.OK);
  }

  @Test
  public void testProtocolHandlerTCP() {
    String content = "Hello World!";

    ByteBuf buf = Unpooled.wrappedBuffer(content.getBytes());
    EmbeddedChannel embeddedChannel = new EmbeddedChannel(new ProtocolHandler());
    assert embeddedChannel.writeInbound(buf) : "failed to write inbound message"; // TCP Echo handler not called. Strage.
    ByteBuf response = embeddedChannel.readOutbound();

    String output = response.readCharSequence(content.length(), Charset.defaultCharset()).toString();

    assertEquals(output, content);
    embeddedChannel.close();
  }
}
