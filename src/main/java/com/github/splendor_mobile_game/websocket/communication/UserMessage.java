package com.github.splendor_mobile_game.websocket.communication;

import java.util.UUID;

import com.github.splendor_mobile_game.websocket.handlers.UserRequestType;
import com.github.splendor_mobile_game.websocket.utils.json.JsonParser;
import com.github.splendor_mobile_game.websocket.utils.json.Optional;
import com.github.splendor_mobile_game.websocket.utils.json.exceptions.JsonParserException;
import com.google.gson.Gson;

// TODO: Java doc required
public class UserMessage {
    private UUID messageContextId;
    private UserRequestType type;
    @Optional
    private Object data;

    public UserMessage(String message) throws InvalidReceivedMessage {
        UserMessage msg = UserMessage.fromJson(message);
        this.messageContextId = msg.messageContextId;
        this.type = msg.type;
        this.data = msg.getData();
    }

    public UserMessage(UUID messageContextId, UserRequestType type, Object data) {
        this.messageContextId = messageContextId;
        this.type = type;
        this.data = data;
    }

    public void parseDataToClass(Class<?> clazz) throws InvalidReceivedMessage {
        try {
            // TODO: Perfomance loss because of redundant json parsing
            this.data = JsonParser.parseJson((new Gson()).toJson(this.data), clazz);
        } catch (JsonParserException e) {
            throw new InvalidReceivedMessage("Received message is invalid!", e);
        }
    }

    public static UserMessage fromJson(String inputJson) throws InvalidReceivedMessage {
        try {
            return JsonParser.parseJson(inputJson, UserMessage.class);
        } catch (JsonParserException e) {
            throw new InvalidReceivedMessage("Received message is invalid!", e);
        }
    }

    public UUID getMessageContextId() {
        return messageContextId;
    }

    public UserRequestType getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((messageContextId == null) ? 0 : messageContextId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserMessage other = (UserMessage) obj;
        if (messageContextId == null) {
            if (other.messageContextId != null)
                return false;
        } else if (!messageContextId.equals(other.messageContextId))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (data == null) {
            if (other.data != null)
                return false;
        } else if (!data.equals(other.data))
            return false;
        return true;
    }

}