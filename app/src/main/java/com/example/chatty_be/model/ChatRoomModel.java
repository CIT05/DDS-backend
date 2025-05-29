package com.example.chatty_be.model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatRoomModel {
    String chatRoomId;
    List<String> userIds;
    Timestamp lastMessageTimestamp;
    String lastMessageSenderId;
    String lastMessage;
    private Map<String, Object> offer;
    private Map<String, Object> answer;
    private List<String> active;

    public ChatRoomModel() {
        this.active = new ArrayList<>();
    }

    public ChatRoomModel(String chatRoomId, List<String> userIds, Timestamp lastMessageTimestamp, String lastMessage) {
        this.chatRoomId = chatRoomId;
        this.userIds = userIds;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastMessage = lastMessage;
        this.active = new ArrayList<>();
        this.offer = null;
        this.answer = null;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Map<String, Object> getOffer() {
        return offer;
    }

    public void setOffer(Map<String, Object> offer) {
        this.offer = offer;
    }

    public Map<String, Object> getAnswer() {
        return answer;
    }

    public void setAnswer(Map<String, Object> answer) {
        this.answer = answer;
    }

    public List<String> getActive() {
        return active;
    }

    public void setActive(List<String> active) {
        this.active = active;
    }
}