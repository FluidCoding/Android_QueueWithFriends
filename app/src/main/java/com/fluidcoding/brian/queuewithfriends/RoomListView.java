package com.fluidcoding.brian.queuewithfriends;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class RoomListView extends AppCompatActivity {
    FloatingActionButton fActBtn;   // Floating Action button for that delicious material design
    ListView lstRooms;    // UI Component to hold room names
    Firebase fbRoomRef;  // Firebase reference to room data
    Firebase userRef;   // Firebase reference to User Accounts
    RoomMap mRoom;     // Struct describing a room
    HashMap<String, RoomMap> roomList;
    String uID, token;  // Users Logged in User id
    RoomAdapter adpt;   // Adapter to update UI with room data
    ArrayList<RoomMap> roomsToBuild;    // Collection of rooms to pass to adapter
    ArrayList<String> roomIDs;          // IDs of rooms for ease of use
    SharedPreferences userAuth;         // Current login data cache
    static RoomDataListener dataListener;   // Listener for newly added rooms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        uID = i.getStringExtra("uID");
        setContentView(R.layout.activity_room_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userAuth = getSharedPreferences("auth", 0);

        roomList = new HashMap<>();

        AdapterView.OnItemClickListener roomClickListener = new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        };

        roomsToBuild = new ArrayList<>();
        roomIDs = new ArrayList<>();

        initFB();
        initUI();

        for(String key: roomList.keySet()){
            roomIDs.add(key);
            roomsToBuild.add(roomList.get(key));
            Log.d("Room Added ", key + " : " + roomList.get(key).getRoomTitle());
        }
        adpt = new RoomAdapter(this, roomsToBuild, roomIDs);
        lstRooms = (ListView)findViewById(R.id.listViewRooms);
        lstRooms.setAdapter(adpt);
        lstRooms.setOnItemClickListener(new RoomClickListener());
    }

    /**
     *
     */
    public void initUI(){
        fActBtn = (FloatingActionButton) findViewById(R.id.fab);

        fActBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createRoom();
            }
        });
    }

    /**
     *
     */
    public void initFB(){
        Firebase.setAndroidContext(this);
        userRef = new Firebase("https://queueusers.firebaseio.com/");
        token = userAuth.getString("token", "notlogin");
        uID = userAuth.getString("uID", "notlogin");

        fbRoomRef = new Firebase("https://youtubeq.firebaseio.com/roomList");

        // Get a reference for dataListener, so that
        // i can remove event listener when activity is changed.
        dataListener = new RoomDataListener();

        fbRoomRef.addValueEventListener(dataListener);

    }

    public boolean createRoom(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set this to final to access from inner class
        final View customView = LayoutInflater.from(this).inflate(R.layout.room_create_dialog, null);
        builder.setView(customView);

        final EditText txtRoomName = (EditText)customView.findViewById(R.id.txtRoomName);
        final CheckBox chkIsPublic = (CheckBox)customView.findViewById(R.id.checkIsPublic);

        builder.setPositiveButton(R.string.create_room, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String roomID = "";
                String roomName = txtRoomName.getText().toString();
                boolean isPublic = chkIsPublic.isChecked();

                mRoom = new RoomMap(uID, roomName, isPublic);

                if (fbRoomRef != null) {
                    Firebase roomGen = fbRoomRef.push();
                    //roomGen.getPath();
                    roomGen.setValue(mRoom);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        builder.setTitle(R.string.create_room_title);

        AlertDialog dialog = builder.create();
        dialog.show();

        return true;
    }

    private final class RoomDataListener implements ValueEventListener{
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            adpt.clear();
            roomsToBuild.clear();
            roomIDs.clear();
            for(DataSnapshot d: dataSnapshot.getChildren()) {
                // TODO: implement try catch for possible empty room list
                roomList.put(d.getKey(), d.getValue(RoomMap.class));
                roomIDs.add(d.getKey());
                roomsToBuild.add(roomList.get(d.getKey()));
//                    adpt.add(roomList.get(d.getKey()));
                Log.d("Room Added: ", d.getKey() + " : " + roomList.get(d.getKey()).getRoomTitle());
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }

    class RoomClickListener implements android.widget.AdapterView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TextView txtName = (TextView)((LinearLayout)view).findViewById(R.id.txtRoomName);

            Log.d("Room Name", txtName.getText().toString());
            Log.d("Room ID", txtName.getTag().toString());
            // Do real work
            Intent queueIntent = new Intent(getApplicationContext(), QueueView.class);
            queueIntent.putExtra("rID", txtName.getTag().toString());
            fbRoomRef.removeEventListener(dataListener);
            startActivity(queueIntent);
        }
    }
}
