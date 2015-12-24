package com.fluidcoding.brian.queuewithfriends;

import java.util.ArrayList;

/**
 * Created by Brian Sonnie on 10/4/2015.
 *
 */
public class RoomMap {
    String createdBy;
    QueueList queueList;
    String roomTitle;
    Boolean isPublic;

    // Empty Constructor for Firebase serialization
    public RoomMap(){
        
    }

    public RoomMap(String _createdBy, String _roomTitle, Boolean _isPublic){
        createdBy = _createdBy;
        roomTitle = _roomTitle;
        isPublic = _isPublic;
        queueList = new QueueList(new ArrayList<String>(), new ArrayList<String>(),new ArrayList<String>());
    }

    public QueueList getQueueList(){
        return queueList;
    }

    public String getRoomTitle(){
        return roomTitle;
    }

    public Boolean getIsPublic(){
        return isPublic;
    }

    public String getCreatedBy(){
        return createdBy;
    }

    public void setCreatedBy(String _createdBy){
        createdBy = _createdBy;
    }

    public void setIsPublic(Boolean _isPublic){
        isPublic = _isPublic;
    }

    public void setQueueList(QueueList _queueList){
        queueList = _queueList;
    }

    public void setRoomTitle(String _roomTitle){
        roomTitle = _roomTitle;
    }
}
