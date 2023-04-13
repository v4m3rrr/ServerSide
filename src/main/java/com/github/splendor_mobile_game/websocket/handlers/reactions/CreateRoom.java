package com.github.splendor_mobile_game.websocket.handlers.reactions;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.splendor_mobile_game.database.Database;
import com.github.splendor_mobile_game.websocket.handlers.exceptions.*;
import com.github.splendor_mobile_game.game.Exceptions.NotEnoughTokensException;
import com.github.splendor_mobile_game.game.Exceptions.SameTokenTypesException;
import com.github.splendor_mobile_game.game.enums.CardTier;
import com.github.splendor_mobile_game.game.enums.TokenType;
import com.github.splendor_mobile_game.game.model.Card;
import com.github.splendor_mobile_game.game.model.Room;
import com.github.splendor_mobile_game.game.model.User;
import com.github.splendor_mobile_game.websocket.communication.ServerMessage;
import com.github.splendor_mobile_game.websocket.communication.UserMessage;
import com.github.splendor_mobile_game.websocket.handlers.DataClass;
import com.github.splendor_mobile_game.websocket.handlers.Messenger;
import com.github.splendor_mobile_game.websocket.handlers.Reaction;
import com.github.splendor_mobile_game.websocket.handlers.ReactionName;
import com.github.splendor_mobile_game.websocket.handlers.ServerMessageType;
import com.github.splendor_mobile_game.websocket.response.ErrorResponse;
import com.github.splendor_mobile_game.websocket.response.Result;
import com.github.splendor_mobile_game.websocket.utils.Log;

// TODO: This whole class can be unit tested

/**
 * Reaction sent when player wants to create a new room.
 * react() function should send generated by server room's UUID and enterCode back to the user. Message type should be equivalent to `CREATE_ROOM_RESPONSE`
 *
 * Example user request
 {
    "contextId": "80bdc250-5365-4caf-8dd9-a33e709a0116",
    "type": "CREATE_ROOM",
    "data": {
        "roomDTO": {
            "name": "TajnyPokoj",
            "password": "Tajne6Przez2Poufne.;"
        },
        "userDTO": {
            "uuid": "f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454",
            "name": "James"
        }
    }
 }
 *
 *
 * If everything is alright, then the server should generate a response containing user and room information.
 *
 *
 * Example server response
 {
    "contextId":"80bdc250-5365-4caf-8dd9-a33e709a0116",
    "type":"CREATE_ROOM_RESPONSE",
    "result":"OK",
    "data":{
        "user":{
            "id":"f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454",
            "name":"James"
        },
        "room":{
            "uuid":"59913c86-bc7e-44a4-ad8e-2ffadd574df3",
            "name":"TajnyPokoj",
            "enterCode":"zCiDsC"
        }
    }
 }
 *
 *
 *
 * Otherwise, server should generate an ERROR response sent only to the author of the request.
 * Example response while error occurs:
 {
    "contextId":"80bdc250-5365-4caf-8dd9-a33e709a0116",
    "type":"CREATE_ROOM_RESPONSE",
    "result":"FAILURE",
    "data":{
        "error":"Leave your current room before joining another."
    }
 }

 *
 *
 * Validation:
 * -> regex user uuid
 * -> regex room's enterCode
 * -> check if room exists
 * -> check if user is already in any room
 * -> check if room still has room for new player (player count less than 4)
 * -> validate password correctness
 *
 *
 * Model specification:
 * -> add new user to the database
 * -> add user to the Room.java instance (find room by id specified by user)
 *
 */
@ReactionName("CREATE_ROOM")
public class CreateRoom extends Reaction {

    public CreateRoom(int connectionHashCode, UserMessage userMessage, Messenger messenger, Database database) {
        super(connectionHashCode, userMessage, messenger, database);
    }

    /**
     * Room information sent by user
     */
    public static class RoomDTO {
        public String name;
        public String password;

        public RoomDTO(String name, String password) {
            this.name = name;
            this.password = password;
        }

    }

    /**
     * Sender information sent by user
     */
    public static class UserDTO {
        public UUID uuid;
        public String name;

        public UserDTO(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

    }

    /**
     * Data sent by the user
     */
    @DataClass
    public static class DataDTO {
        public RoomDTO roomDTO;
        public UserDTO userDTO;

        public DataDTO(RoomDTO roomDTO, UserDTO userDTO) {
            this.roomDTO = roomDTO;
            this.userDTO = userDTO;
        }

    }


    /**
     * Data sent by the server
     */
    public class ResponseData {
        public UserDataResponse user;
        public RoomDataResponse room;

        public ResponseData(UserDataResponse user, RoomDataResponse room) {
            this.user = user;
            this.room = room;
        }
        
    }

    /**
     * User data sent by the server
     */
    public class UserDataResponse {
        public UUID id;
        public String name;

        public UserDataResponse(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
        
    }

    /**
     * Room data sent by the server
     */
    public class RoomDataResponse {
        public UUID uuid;
        public String name;
        public String enterCode;

        public RoomDataResponse(UUID uuid, String name, String enterCode) {
            this.name = name;
            this.uuid = uuid;
            this.enterCode = enterCode;
        }

    }


    @Override
    public void react() {

        DataDTO dataDTO = (DataDTO) userMessage.getData();

        try {
            validateData(dataDTO, this.database);

            User user = new User(dataDTO.userDTO.uuid, dataDTO.userDTO.name, this.connectionHashCode);
            Room room = new Room(UUID.randomUUID(), dataDTO.roomDTO.name, dataDTO.roomDTO.password, user, database);

            // Debugging purposes only
            Log.DEBUG("Kod pokoju: " + room.getEnterCode());
            Log.DEBUG("Uuid pokoju: " + room.getUuid());

            database.addUser(user);
            database.addRoom(room);

            // room.startGame(); // Testing purpose only

            UserDataResponse userDataResponse = new UserDataResponse(dataDTO.userDTO.uuid, dataDTO.userDTO.name);
            RoomDataResponse roomDataResponse = new RoomDataResponse(room.getUuid(), dataDTO.roomDTO.name, room.getEnterCode());
            ResponseData responseData = new ResponseData(userDataResponse, roomDataResponse);
            ServerMessage serverMessage = new ServerMessage(userMessage.getContextId(), ServerMessageType.CREATE_ROOM_RESPONSE, Result.OK, responseData);

            messenger.addMessageToSend(this.connectionHashCode, serverMessage);

        } catch (Exception e) {
            ErrorResponse errorResponse = new ErrorResponse(Result.FAILURE, e.getMessage(), ServerMessageType.CREATE_ROOM_RESPONSE, userMessage.getContextId().toString());
            messenger.addMessageToSend(connectionHashCode, errorResponse);
        }
    }


    /**
     *
     * @param dataDTO data provided by user
     * @param database database instance
     * @throws InvalidUUIDException thrown when UUID format is invalid
     * @throws InvalidUsernameException thrown when username format is invalid
     * @throws RoomAlreadyExistsException thrown when room already exists
     * @throws InvalidPasswordException thrown when provided password is incorrect
     * @throws UserAlreadyInRoomException thrown if user is already a member of any room
     */
    private void validateData(DataDTO dataDTO, Database database) throws InvalidUUIDException, InvalidUsernameException, RoomAlreadyExistsException, InvalidPasswordException, UserAlreadyInRoomException {
        Pattern uuidPattern     = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        Pattern usernamePattern = Pattern.compile("^(?=.*\\p{L})[\\p{L}\\p{N}\\s]+$");
        Pattern passwordPattern = Pattern.compile("^[a-zA-Z0-9ąćęłńóśźżĄĆĘŁŃÓŚŹŻ\\p{Punct}]+$");

        // Check if user UUID matches the pattern
        Matcher uuidMatcher = uuidPattern.matcher(dataDTO.userDTO.uuid.toString());
        if (!uuidMatcher.find())
            throw new InvalidUUIDException("Invalid UUID format.");


        // Check if user UUID matches the pattern
        Matcher usernameMatcher = usernamePattern.matcher(dataDTO.userDTO.name);
        if (!usernameMatcher.find())
            throw new InvalidUsernameException("Invalid username credentials.");


        // Check if user UUID matches the pattern
        usernameMatcher = usernamePattern.matcher(dataDTO.roomDTO.name);
        if (!usernameMatcher.find())
            throw new InvalidUsernameException("Invalid room name format.");


        // Check if user UUID matches the pattern
        Matcher passwordMatcher = passwordPattern.matcher(dataDTO.roomDTO.password);
        if (!passwordMatcher.find())
            throw new InvalidPasswordException("Invalid room password format.");


        // Check if room with specified name already exists NAME OF THE ROOM MUST BE UNIQUE
        if (database.getRoom(dataDTO.roomDTO.name) != null)
            throw new RoomAlreadyExistsException("Room with specified name already exists!");


        // Check if user is already a member of any room
        database.isUserInRoom(dataDTO.userDTO.uuid);

    }
}
