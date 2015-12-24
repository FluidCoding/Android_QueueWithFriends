package com.fluidcoding.brian.queuewithfriends;

/**
 * Basic POJO Class for chat message structure
 * Created by brian on 11/28/2015.
 */
public class Message {
    private String name, message;

    // Empty Constructor For Serialization
    public Message(){

    }

    public Message(String name, String message){
        this.name = name;
        this.message = message;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
