package com.fluidcoding.brian.queuewithfriends;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class QueueView extends YouTubeBaseActivity {
    final String KEY = Auth.API_KEY;    // API KEY as static member variable of Auth
    final int MAX_MESSAGES = 7;  // Limit to chat messages per room
        // UI handles \\
    private ImageButton searchBtn;
    private EditText searchTxt;
    private EditText chatTxt;
    private EditText chatWindow;
    private ImageButton sendChatBtn;
    private ImageButton nextVid, pqVid, prevVid;
    private Button QCSwitchBtn;
    private ViewSwitcher viewSwitcherQC;
    private HorizontalScrollView hScrollSearch;
    private LinearLayout queuesView;
    private LinearLayout chatView;
    private LinearLayout queueView;
    private LinearLayout searchView;

    static int vidIndex = 0;
    private boolean isJoining = true;
    private String roomID, userName;                                    // Current room name, and logged in user.

    private Firebase fbRef, fbChat;                                     // Firebase handles
    private ValueEventListener queueListener, chatListener;             // Firebase Event Listeners

    private QueueList qL;
    private QueueList sList;


    // Objects for YouTube Player API Calls
    public static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    public static final JsonFactory JSON_FACTORY = new JacksonFactory();

                                                                // Queue Collections
    private ArrayList<YouTubeThumbnailLoader> queueThmLoader;
    private ArrayList<YouTubeThumbnailView> queueThmView;

    // Search Collections
    private ArrayList<YouTubeThumbnailView> searchThmView;
    private ArrayList<YouTubeThumbnailLoader> searchThmLoader;
                                                                // Collections for queue entries
    private ArrayList<String> searchIds;
    private ArrayList<String> searchThumbUrls;
    private ArrayList<String> searchTitles;

    private LinkedList<Message> chatMessageList;                // Chat Message Collection with Size limit MAX_MESSAGES

    private YouTubePlayerFragment youTubePlayerFragment;        // Fragment That Holds YouTubePlayer
    private YouTubePlayer youtubePlayer;
    private YouTube ytData;                                     // Object to make YouTube search queries/api calls


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve RoomID from parceable
        roomID = getIntent().getStringExtra("rID");
        SharedPreferences loginAuth = getSharedPreferences("auth", 0);
        // Get name from email
        userName = loginAuth.getString("uName", "nologin");
        userName = userName.substring(0, userName.indexOf("@"));
        isJoining=true;

        Log.d("QueueView: ", "User: " + userName + ", Joined: " + roomID);

        setContentView(R.layout.activity_queue_view);
        initUI();                                       // Hooks component views
        initYTFragment();                               // Initialize youtube fragment
        initFireBase();                                 // Instantiate FireBase functionality
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear potential memory leaks
        // anywhere activity context is passed
        if(youtubePlayer!=null)
            youtubePlayer.release();

        // Queue Thumbnail Loaders
        if (queueThmLoader != null) {
            for(int i = 0; i < queueThmLoader.size(); i++)
                queueThmLoader.get(i).release();
        }
        // Search Thumbnail Loaders
        if(searchThmLoader!=null) {
            for (int i = 0; i < searchThmLoader.size(); i++)
                searchThmLoader.get(i).release();
        }
        if(queueListener!=null)
            fbRef.removeEventListener(queueListener);

        if(chatListener!=null)
            fbChat.removeEventListener(chatListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_fire_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Init Firebase object add event listener with callback for async data
     *  Show Current Queue Items in the View
     */
    public void initFireBase(){
        Firebase.setAndroidContext(this);
        fbRef = new Firebase("https://youtubeq.firebaseio.com/roomList/" + roomID);
        fbChat = new Firebase("https://queueusers.firebaseio.com/Chat/" + roomID);
        qL = new QueueList();

        // Event listener for updates to the queue on server
        // TODO Add event listeners for other types of value changes
        //          I.e. child added/removed
        // Firebase Data Listeners
        queueListener = new QueueChangeListener();
        chatListener = new ChatChangeListener();
        fbRef.addValueEventListener(queueListener);
        fbChat.addValueEventListener(chatListener);
    }

    /**
     *  Initialize User Interface components
     */
    public void initUI(){
        // Hook View Components
        searchBtn = (ImageButton)findViewById(R.id.btnSearch);
        searchTxt = (EditText)findViewById(R.id.txtSearch);
        searchView = (LinearLayout)findViewById(R.id.linearLayoutSearch);
        hScrollSearch = (HorizontalScrollView)findViewById(R.id.horizontalScrollViewSearchResults);
        queueView = (LinearLayout)findViewById(R.id.queueView);
        youTubePlayerFragment = (YouTubePlayerFragment)getFragmentManager().findFragmentById(R.id.fragYT);
        viewSwitcherQC = (ViewSwitcher)findViewById(R.id.viewSwitcher);
        QCSwitchBtn = (Button)findViewById(R.id.btnQCSwitch);
        sendChatBtn= (ImageButton)findViewById(R.id.btnSendChat);
        chatWindow = (EditText)findViewById(R.id.chatWindow);
        chatTxt = (EditText)findViewById(R.id.txtChat);
        prevVid = (ImageButton)findViewById(R.id.btnPrevious);
        nextVid = (ImageButton)findViewById(R.id.btnNext);
        pqVid = (ImageButton)findViewById(R.id.btnPState);

        chatMessageList = new LinkedList<>();
        // *** Event Listeners

        // Support for searching from the enter button on soft keyboard
        searchTxt.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH)
                    searchYT(v);
                return false;
            }
        });

        // Support for sending chat message using send button on soft keyboard
        chatTxt.setOnEditorActionListener(new EditText.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEND){
                    sendChat();
                }
                return false;
            }
        });

        // Animation translations for view toggle
        final Animation anim_slide_right = AnimationUtils.loadAnimation(this, R.anim.qc_slide_right);
        final Animation anim_slide_left = AnimationUtils.loadAnimation(this, R.anim.qc_slide_left);
        // TODO: HIDE SEARCH BAR OR SEARCH_EDIT TEXT IN CHAT VIEW
        QCSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("View", QCSwitchBtn.getText().toString());
                if(QCSwitchBtn.getText().toString().equals(getString(R.string.btn_chat_view))){
                    QCSwitchBtn.setText(R.string.btn_queue_view);
                    viewSwitcherQC.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.slide_in_left));
                    viewSwitcherQC.showNext();

                    QCSwitchBtn.startAnimation(anim_slide_left);
                    anim_slide_left.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)QCSwitchBtn.getLayoutParams();
                            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                            QCSwitchBtn.setLayoutParams(params);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }
                else{
                    QCSwitchBtn.setText(R.string.btn_chat_view);
                    viewSwitcherQC.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.slide_out_right));
                    viewSwitcherQC.showPrevious();
                    QCSwitchBtn.startAnimation(anim_slide_right);
                    anim_slide_right.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {

                            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) QCSwitchBtn.getLayoutParams();
                            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
                            QCSwitchBtn.setLayoutParams(params);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }
            }
        });

        sendChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendChat();
            }
        });

        prevVid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevious();
            }
        });

        nextVid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        });

        pqVid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(youtubePlayer!=null) {
                    if (!youtubePlayer.isPlaying()) {
                        youtubePlayer.play();
                    } else {
                        youtubePlayer.pause();
                    }
                }
            }
        });
    }


    /**
     Initialize YouTube Fragment hook it to a YouTubePlayerFragment Object
     Initialize the youtube player with api key
     */
    public void initYTFragment(){

        youTubePlayerFragment.initialize(KEY, new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider,
                                                YouTubePlayer player,
                                                boolean wasRestored) {
                youtubePlayer = player;
                youtubePlayer.setPlayerStateChangeListener(new PlayerPlaybackEventListener());
                youtubePlayer.setShowFullscreenButton(false);
                youtubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.MINIMAL);
                roomJoinSync();
                youtubePlayer.setPlaybackEventListener(new YouTubePlayer.PlaybackEventListener() {
                    @Override
                    public void onPlaying() {
                        pqVid.setBackgroundResource(android.R.drawable.ic_media_pause);
                        if(!qL.isPlaying()) {
                            qL.setPlaying(true);
                            fbRef.child("queueList").setValue(qL);
                         }
                    }

                    @Override
                    public void onPaused() {
                        pqVid.setBackgroundResource(android.R.drawable.ic_media_play);
                        if(qL.isPlaying()) {
                            qL.setPlaying(false);
                            fbRef.child("queueList/playing").setValue(qL.isPlaying());
                        }
                    }

                    @Override
                    public void onStopped() {
                        pqVid.setBackgroundResource(android.R.drawable.ic_media_play);
                    }

                    @Override
                    public void onBuffering(boolean b) {

                    }

                    @Override
                    public void onSeekTo(int i) {

                    }
                });
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider,
                                                YouTubeInitializationResult result) {
            }

        });
    }

    public void roomJoinSync(){
        if(isJoining && qL.getSize()>0) {
            youtubePlayer.cueVideo(qL.getId(qL.getIndex()));
            isJoining=false;
        }
    }

    /**
     * Sends chat message out to propagate to other clients
     * maintains a history of MAX_MESSAGES length
     */
    public void sendChat(){
        String chatText = chatTxt.getText().toString();
        if(chatText.isEmpty())return;

        Message msg = new Message(userName, chatText);

        // check length of messages then push
        if(chatMessageList.size()<=MAX_MESSAGES) {
            chatMessageList.addLast(msg);
            fbChat.setValue(chatMessageList);
        }
        else{
            chatMessageList.addLast(msg);
            chatMessageList.removeFirst();
            fbChat.setValue(chatMessageList);
        }
        chatTxt.setText("");
    }

    public void playNext(){
        if(qL.getSize()-1>qL.getIndex())
            qL.setIndex(qL.getIndex()+1);
        else
            qL.setIndex(0);

        youtubePlayer.cueVideo(qL.getId(qL.getIndex()));
        fbRef.child("queueList").setValue(qL);
    }


    public void playPrevious(){
        if(qL.getIndex()>0)
            qL.setIndex(qL.getIndex()-1);

        youtubePlayer.cueVideo(qL.getId(qL.getIndex()));
        fbRef.child("queueList").setValue(qL);
    }

    // Update firebase with recent changes to the queue
    public void updateFirebase(){
        fbRef.child("queueList").setValue(qL);
    }

    /**
     * Event listener to handle player states and auto-play
     */
    private class PlayerPlaybackEventListener implements YouTubePlayer.PlayerStateChangeListener{
        @Override
        public void onLoading() {
            Log.d("QueueView", "Loading...");
        }

        @Override
        public void onLoaded(String s) {
            //if auto play on
            Log.d("QueueView", "Loaded: " + s);
            youtubePlayer.play();
        }

        @Override
        public void onAdStarted() {

        }

        @Override
        public void onVideoStarted() {

        }

        @Override
        public void onVideoEnded() {
            if(qL.getSize()>qL.getIndex())
                qL.setIndex(qL.getIndex()+1);
            else
                qL.setIndex(0);

            youtubePlayer.cueVideo(qL.getId(qL.getIndex()));
        }

        @Override
        public void onError(YouTubePlayer.ErrorReason errorReason) {

        }
    }

    /**
     * Instantiate containters and call to Async task
     * YTSearch will update ui from onPostExec
     * @param v reference to the Search Button
     */
    public void searchYT(View v){
        searchIds = new ArrayList<>();
        searchThumbUrls = new ArrayList<>();
        searchTitles = new ArrayList<>();
        new YTSearch().execute();
    }

    /**
     *  Update SearchView with latest search results
     */
    public void showSearch(){
        searchView.removeAllViews();
        if(searchThmLoader!=null){
            for(int i = 0; i<searchThmLoader.size(); i++){
                searchThmLoader.get(i).release();
            }
        }
        searchThmView = new ArrayList<>();
        searchThmLoader = new ArrayList<>();
        for(int i = 0; i<searchIds.size(); i++) {
            searchThmView.add(new YouTubeThumbnailView(this));
            searchView.addView(searchThmView.get(i));
            searchThmView.get(i).initialize(KEY, new SearchThumbnailListener());

            // TODO: FIX THE SIZING INCONSISTENCY IN THUMBNAILS
            LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            searchThmView.get(i).setLayoutParams(layoutParams);
        }
    }

    /**
     *  Thumbnail Listener for search view thumbnails
     */
    private final class SearchThumbnailListener implements YouTubeThumbnailView.OnInitializedListener {
        @Override
        public void onInitializationSuccess(YouTubeThumbnailView youTubeThumbnailView, YouTubeThumbnailLoader youTubeThumbnailLoader) {
            int i = searchView.indexOfChild(youTubeThumbnailView);
            searchThmLoader.add(youTubeThumbnailLoader);
            searchThmLoader.get(i).setVideo(searchIds.get(i));
            searchThmLoader.get(i).setOnThumbnailLoadedListener(new ThumbnailListener());
            searchThmView.get(i).setOnClickListener(new SearchClickListener());
        }

        @Override
        public void onInitializationFailure(YouTubeThumbnailView youTubeThumbnailView, YouTubeInitializationResult youTubeInitializationResult) {

        }
    }

    /**
     * OnClick For Thumbnails from Search Results View
     * passing in the index of the result
     */
    private final class SearchClickListener implements View.OnClickListener {
        private int index;

        @Override
        public void onClick(View v) {
            index = searchView.indexOfChild(v);
            Log.d("Search Index: ", String.valueOf(index));
            Log.d("search result: ", searchIds.get(index));
            Log.d("search result: title: ", searchTitles.get(index));
            Log.d("height: ", String.valueOf(searchView.getHeight()));

            // Remove view from search add view to Queue
            qL.addItem(searchIds.get(index), searchThumbUrls.get(index), searchTitles.get(index));
            searchIds.remove(index);
            searchThumbUrls.remove(index);
            searchTitles.remove(index);
            searchView.removeView(v);
            v.setOnClickListener(new QueueClickListener());
            v.setOnLongClickListener(new QueueLongClickListener());
            queueThmLoader.add(searchThmLoader.get(index));
            queueView.addView(v);
            updateFirebase();
        }
    }

    /**
     *  Thumbnail Listener for queue view thumbnails
     */
    private final class QueueThumbnailListener implements YouTubeThumbnailView.OnInitializedListener {
        @Override
        public void onInitializationSuccess(YouTubeThumbnailView youTubeThumbnailView, YouTubeThumbnailLoader youTubeThumbnailLoader) {
            int i = queueView.indexOfChild(youTubeThumbnailView);       // Index of Initialized ThumbnailView
            queueThmLoader.add(youTubeThumbnailLoader);
            queueThmLoader.get(i).setVideo(qL.getVideoIds().get(i));
            queueThmLoader.get(i).setOnThumbnailLoadedListener(new ThumbnailListener());
            queueThmView.get(i).setOnClickListener(new QueueClickListener());
            queueThmView.get(i).setOnLongClickListener(new QueueLongClickListener());
        }

        @Override
        public void onInitializationFailure(YouTubeThumbnailView youTubeThumbnailView, YouTubeInitializationResult youTubeInitializationResult) {

        }
    }

    /**
     *  Listener for playing songs from the Queue
     */
    private final class QueueClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            vidIndex = queueView.indexOfChild(v);
            qL.setIndex(vidIndex);
            fbRef.child("queueList").setValue(qL);
            Log.d("Queue Clicked index: ", String.valueOf(vidIndex));
            youtubePlayer.cueVideo(qL.getVideoIds().get(vidIndex));
        }
    }

    /**
     *  Long Click Listener for a Queue Thumbnail
     *  Displays Context menu with action
     */
    private final class QueueLongClickListener implements View.OnLongClickListener{
        @Override
        public boolean onLongClick(View v) {
            registerForContextMenu(v);
            v.showContextMenu();
//            int vidIndex = queueView.indexOfChild(v);
            return true;    // Don't let the onClickListener get invoked after
        }
    }

    /**
     * Default Context menu used for queue LongClick
     * @param menu a context menu item to use
     * @param v reference to the view registered to this menu
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        int index = queueView.indexOfChild(v);
        menu.setHeaderTitle(qL.getTitles().get(index));
        menu.add(index, v.getId(), 0, R.string.context_remove);
        menu.add(index, v.getId(), 1, R.string.context_play_next);
        menu.add(index, v.getId(), 2, R.string.context_move);
    }

    /**
     * Context Item Selected used for Queue Long Click
     * @param item reference to the menu item that was selected
     * @return if selected or canceled
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.d("CONTEC, ", "Item selected " + item.getTitle().toString());
        Log.d("C Select: ", String.valueOf(item.getGroupId()));
        int index = item.getGroupId();
        int order = item.getOrder();
        switch(order){
            case 0:     // Remove
                removeFromQueue(index);
                break;
            case 1:     // Play Next
                break;
            case 2:     // Move to...
                break;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Update Queue view with latest items
     */
    public void showQueue(){
        queueView.removeAllViews();// make sure queue is emptied and filled
        queueThmView = new ArrayList<>();
        if(queueThmLoader!=null){
            for(int i =0; i<queueThmLoader.size(); i++){
                queueThmLoader.get(i).release();
            }
        }

        queueThmLoader = new ArrayList<>();

        if(qL!=null && qL.getVideoIds()!=null) {
            for (int i = 0; i < qL.getVideoIds().size(); i++) {
                Log.d("Queue Showing: ", qL.getVideoIds().get(i));
                queueThmView.add(new YouTubeThumbnailView(this));   // This presents a possible memory leak
                                                        // Must be cleaned later
                Log.d("Queue View ADD", qL.getTitles().get(i));
                queueView.addView(queueThmView.get(i));
                queueThmView.get(i).initialize(KEY, new QueueThumbnailListener());
            }
        }
    }

    /**
     * Removes item from queue list and updates firebase
     * @param index of item to remove from queue
     */
    public void removeFromQueue(int index){

        queueView.removeViewAt(index);
        qL.removeItem(index);
        if(index<queueThmLoader.size()-1 && queueThmView.size()>0) {
            queueThmLoader.get(index).release();
            queueThmLoader.remove(index);
            queueThmView.remove(index);
        }
        updateFirebase();
    }

    /**
     * An internal listener which listens to thumbnail loading events from the
     * {@link YouTubeThumbnailView}.
     */
    private final class ThumbnailListener implements
            YouTubeThumbnailLoader.OnThumbnailLoadedListener {
        @Override
        public void onThumbnailLoaded(YouTubeThumbnailView thumbnail, String videoId) {
            thumbnail.setMinimumWidth(600);
            thumbnail.setMinimumHeight(900);
            thumbnail.setPadding(5,0,5,0);
        }

        @Override
        public void onThumbnailError(YouTubeThumbnailView thumbnail,
                                     YouTubeThumbnailLoader.ErrorReason reason) {

        }
    }

    private class QueueChangeListener implements ValueEventListener{
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String qHash = "";
                for(String id: qL.getVideoIds())
                    qHash+=id;
                // Set serializable QueueList Object to new queue data
                try {
                    qL = dataSnapshot.child("queueList").getValue(QueueList.class);
                }catch(Exception fe){
                    qL = new QueueList();
                    Log.d("queue: ", "queue is empty.");
                }

                // Update the View IFF there was a change to the
                // queue size i.e. Removed or added
                String difHash = "";
                for(String id: qL.getVideoIds())
                    difHash+=id;

                if(qHash.isEmpty() || !qHash.equals(difHash))
                    showQueue();
            }
            // TODO: Address any possible errors here
            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
    }
    
    private class ChatChangeListener implements ValueEventListener{

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            chatMessageList.clear();
            chatWindow.setText("");
            for(DataSnapshot ds: dataSnapshot.getChildren()) {
                chatMessageList.addLast(ds.getValue(Message.class));
            }
            for(Message m: chatMessageList){
                chatWindow.append(m.getName() + ": " + m.getMessage() + "\n");
            }
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }

    /**
     *  Async Task to perform Network communication on a background task
     *  TODO: Reconfigure Arguments and returns for member methods and class type
     */
    class YTSearch extends AsyncTask<String, Void, Object>{
        String query;
        ContentValues queryValues;
        EditText qTxt;
        final Long NUM_SEARCH_RES = 9L;

        @Override
        protected void onPreExecute(){
            queryValues = new ContentValues();
            qTxt = (EditText)findViewById(R.id.txtSearch);
            queryValues.put("query", qTxt.getText().toString());
        }

        @Override
        protected Object doInBackground(String... params) {
            if(!queryValues.getAsString("query").equals("")) {
                try {
                    ytData = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpRequestInitializer() {
                        public void initialize(HttpRequest request) throws IOException {
                        }
                    }).setApplicationName("queuebud").build();

                    // Build the search request
                    YouTube.Search.List searchList = ytData.search().list("id,snippet");
                    searchList.setKey(KEY);
                    // Get query from ipc ContentValues collection
                    Log.d("Query: ", queryValues.getAsString("query"));
                    searchList.setQ(queryValues.getAsString("query")); //pass this in
                    searchList.setType("video");
                    searchList.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
                    searchList.setMaxResults(NUM_SEARCH_RES);

                    // Execute query
                    SearchListResponse searchResponse = searchList.execute();
                    // Structure results in an iterable list
                    List<SearchResult> searchResultList = searchResponse.getItems();
                    Log.d("BG SearchResults: ", searchResponse.toPrettyString());

                    // Iterate over list build search view
                    int i = 0;
                    while (i<searchResultList.size()){
                        SearchResult vid = searchResultList.get(i);
                        ResourceId rId = vid.getId();
                        if(rId.getKind().equals("youtube#video")){
                            Log.d("VID: ", rId.getVideoId());
                            Thumbnail t = vid.getSnippet().getThumbnails().getDefault();
                            searchIds.add(rId.getVideoId());
                            searchTitles.add(vid.getSnippet().getTitle());
                            searchThumbUrls.add(t.getUrl());
                        }
                        i++;
                        Log.d("I: ", String.valueOf(i));
                    }
                } catch (Exception f) {     // TODO: Restrict/narrow this Exception
                    Log.d("net error", f.getMessage());
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(Object o) {
            if(qTxt!=null)
            {
                // Update Search Queue
                showSearch();
            }
            super.onPostExecute(o);
        }
    }
}


// TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
/*
     * Add login support for facebook/google+
     * Add logout support
     * Further Abstract out the thumbnail loading process for both search and queue views.
     * Add other thumbnail longclick functionality
     * Add current video state sync with firebase done but needs support for ads
     * Possibly keep screen awake perms?
     * Media Volume slider
 */