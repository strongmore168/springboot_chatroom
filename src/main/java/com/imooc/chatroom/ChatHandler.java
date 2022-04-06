package com.imooc.chatroom;

import com.alibaba.fastjson.JSON;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class ChatHandler extends TextWebSocketHandler {

  // 在线用户列表 ip->session
  private static final Map<String, WebSocketSession> idSessions = new ConcurrentHashMap<>();
  private static final Map<String, String> idNames = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String name = parseName(session);
    if (StringUtils.isBlank(name)) {
      return;
    }
    if (idNames.values().contains(name)) {
      return;
    }
    idSessions.putIfAbsent(session.getId(), session);
    idNames.putIfAbsent(session.getId(), name);
    for (Entry<String, WebSocketSession> entry : idSessions.entrySet()) {
      entry.getValue().sendMessage(new TextMessage(createAllUserInfo(name)));
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    String name = idNames.getOrDefault(session.getId(), "");
    if (StringUtils.isBlank(name)) {
      return;
    }
    idSessions.remove(session.getId());
    idNames.remove(session.getId());
    for (Entry<String, WebSocketSession> entry : idSessions.entrySet()) {
      entry.getValue().sendMessage(new TextMessage(createAllUserInfo(name)));
    }
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    String name = idNames.getOrDefault(session.getId(), "");
    if (StringUtils.isBlank(name)) {
      return;
    }
    String msg = message.getPayload();
    for (Entry<String, WebSocketSession> entry : idSessions.entrySet()) {
      entry.getValue().sendMessage(new TextMessage(createMessageInfo(name, msg)));
    }
  }

  private String parseName(WebSocketSession session) {
    URI uri = session.getUri();
    Map<String, String> params = new HashMap<>();
    if (Objects.nonNull(uri)) {
      String query = uri.getQuery();
      String[] rawKeyValues = query.split("&");
      for (String rawKeyValue : rawKeyValues) {
        String[] keyValues = rawKeyValue.split("=");
        if (keyValues.length == 2) {
          params.put(keyValues[0], keyValues[1]);
        }
      }
    }
    return params.getOrDefault("name", "");
  }

  private String createAllUserInfo(String fromName) {
    TextInfo textInfo = new TextInfo();
    textInfo.setType("allUser");
    textInfo.setFrom(fromName);
    textInfo.setUsers(List.copyOf(idNames.values()));
    return JSON.toJSONString(textInfo);
  }

  private String createMessageInfo(String fromName, String msg) {
    TextInfo textInfo = new TextInfo();
    textInfo.setType("chat");
    textInfo.setFrom(fromName);
    textInfo.setMsg(msg);
    return JSON.toJSONString(textInfo);
  }

  @Setter
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  static final class TextInfo {

    private String type;
    private String from;
    private List<String> users;
    private String msg;
  }

}
