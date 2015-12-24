package com.fluidcoding.brian.queuewithfriends;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by brian on 10/9/2015.
 */
public class RoomAdapter extends ArrayAdapter<RoomMap> {
    private Context context;
    private ArrayList<String> roomIDs;

    public RoomAdapter(Context _context, ArrayList<RoomMap> rooms, ArrayList<String> _roomIDs) {
        super(_context, 0, rooms);
        roomIDs = _roomIDs;
        context = _context;
    }
    // TODO use view holder for performance
    public View getView(int position, View convertView, ViewGroup parent){
        RoomMap r = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.room_item, parent, false);
        }
        TextView tv = (TextView)convertView.findViewById(R.id.txtRoomName);
        tv.setText(r.getRoomTitle());
        tv.setTag(roomIDs.get(position));
        return convertView;
    }
}