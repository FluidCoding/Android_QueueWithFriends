package com.fluidcoding.brian.queuewithfriends;


import java.util.ArrayList;

/**
 * Data Structure for queue
 * Created by brian on 8/31/2015.
 */
public class QueueList {
    private int index  = 0;
    private int size = 0;

    boolean playing = false;
    private ArrayList<String> ids;
    private ArrayList<String> imgs;
    private ArrayList<String> titles;

    public QueueList(ArrayList<String> _id, ArrayList<String> _img, ArrayList<String> _title){
        ids = _id;
        imgs = _img;
        titles = _title;
        size=ids.size();
    }

    public QueueList(){
        ids=new ArrayList<>();
        imgs=new ArrayList<>();
        titles=new ArrayList<>();
        size=ids.size();
    }

    public void addItem(String _id, String _img, String _title){
        ids.add(_id);
        imgs.add(_img);
        titles.add(_title);
        size++;
    }
    public void removeItem(int index){
        if(index < size && size > 0) {
            ids.remove(index);
            imgs.remove(index);
            titles.remove(index);
            size--;
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean isPlaying) {
        this.playing = isPlaying;
    }

    public ArrayList<String> getVideoIds(){
        return ids;
    }

    public ArrayList<String> getThumbUrls(){
        return imgs;
    }

    public ArrayList<String> getTitles(){
        return titles;
    }

    public String getId(int index){
        return ids.get(index);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getIndex(){
        return index;
    }

    public void setIndex(int _index){
        index = _index;
    }
    public void setThumbUrls(ArrayList<String> _img){
        imgs = _img;
    }
    public void setVideoIds(ArrayList<String> _id){
        ids= _id;
    }
    public void setTitles(ArrayList<String> _title){
        titles = _title;
    }

}

