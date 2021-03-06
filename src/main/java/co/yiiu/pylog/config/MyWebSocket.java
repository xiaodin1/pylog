package co.yiiu.pylog.config;

import co.yiiu.pylog.handler.LogHandler;
import co.yiiu.pylog.util.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@ServerEndpoint(value = "/websocket", encoders = MessageEncoder.class, decoders = MessageDecoder.class)
@Component
@Slf4j
public class MyWebSocket {

  //在线人数
  private static int online = 0;
  //所有的对象，用于群发
  private static List<Session> webSockets = new CopyOnWriteArrayList<>();

  //建立连接
  @OnOpen
  public void onOpen(Session session) {
    online++;
    webSockets.add(session);
    log.info("有用户打开连接，当前有{}个用户连接", online);
  }

  //连接关闭
  @OnClose
  public void onClose(Session session) {
    online--;
    webSockets.remove(session);
    log.info("有用户断开连接，当前有{}个用户连接", online);
  }

  //收到客户端的消息
  @OnMessage
  public void onMessage(Session session, Message message) {
    try {
      log.info("session.id: {}, message: {}", session.getId(), message);
      if (message.getType().equals("fetchLogs")) {
        SiteConfig siteConfig = SpringContextUtil.getBean(SiteConfig.class);
        Map<String, String> logs = siteConfig.getLogs();
        session.getBasicRemote().sendObject(new Message("fetchLogs", logs));
      } else {
        new Thread(() -> new LogHandler(session, message).sendMessage()).start();
      }
    } catch (Exception e) {
      log.error(e.getMessage());
    }
  }
}
