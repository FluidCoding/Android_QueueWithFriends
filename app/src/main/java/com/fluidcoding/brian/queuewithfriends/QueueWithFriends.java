package com.fluidcoding.brian.queuewithfriends;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Map;

public class QueueWithFriends extends AppCompatActivity {

    private CheckBox checkNewUser;  // Checked for New User
    private EditText txtRepeatPass; // Repeat Password Input
    private EditText txtPass;       // Password Input
    private EditText txtEmail;      // Email Input
    private Button btnSubmit;       // Login/Register Form Submit
    private Firebase userFBRef;     // Firenase reference to user accounts
    SharedPreferences loginAuth;    // login token/name valid for 24 hours
    private Firebase.AuthStateListener mAuthStateListener;
    Intent selectRoom;      // Room Select Activity
    private String uName;   // Username for cache
    //Switched back to strings for slight performance gain   Set<String> authSet;
// ^    ArrayList<String> authList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qwf_login);
        // Instances of next intent and auth cache
        selectRoom = new Intent(this, RoomListView.class);
        loginAuth = getSharedPreferences("auth", 0);
        uName=loginAuth.getString("uName","nologin");

        initFB();
        initUI();
    }

    public void initUI(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Hook Components
        checkNewUser = (CheckBox)findViewById(R.id.checkNewUser);
        checkNewUser.setOnCheckedChangeListener(new LoginOrRegisterChanged());
        txtEmail = (EditText)findViewById(R.id.txtEmail);
        txtPass = (EditText)findViewById(R.id.txtPassword);
        txtRepeatPass = (EditText)findViewById(R.id.txtPasswordRepeat);
        btnSubmit = (Button)findViewById(R.id.btnLogin);
        // Remember username by default
        if(!uName.equals("nologin"))    txtEmail.setText(uName);
    }

    public void initFB(){
        // Objects for firebase and authentication caching
        Firebase.setAndroidContext(this);
        userFBRef = new Firebase("https://queueusers.firebaseio.com/");

        // Switching back to just single key values for less overhead
        //authSet = loginAuth.getStringSet("AuthSet", new HashSet<String>());
        // Check Login Token
        final String token = loginAuth.getString("token", "nologin");
        final String uID = loginAuth.getString("uID", "nologin");

        Log.d("Login Auth: ", token);
        Log.d("User Auth: ", uID);
        // Test the token
        if(!token.equals("nologin")){
            userFBRef.authWithCustomToken(token, new Firebase.AuthResultHandler() {
                // Continue to room list
                @Override
                public void onAuthenticated(AuthData authData) {
                    Log.d("Auth: ", "Authenticated Success");
                    startActivity(selectRoom);
                }
                // Show login
                @Override
                public void onAuthenticationError(FirebaseError firebaseError) {
                    Log.d("Auth Error:", firebaseError.getMessage());
                }
            });
        }
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
     * Attempt to log user in or register
     * Show snackbar on error
     * @param v the view in static context that triggered this call
     */
    public void login(final View v){
        if(checkNewUser.isChecked()){
            userFBRef.authWithPassword(txtEmail.getText().toString(), txtPass.getText().toString(),
                    new Firebase.AuthResultHandler() {
                        @Override
                        public void onAuthenticated(AuthData authData) {
                            Log.d("Login Success: ", "User ID: " + authData.getUid() + ", Provider: " + authData.getProvider());
                            // Cache login auth data
                            loginAuth.edit().putString("token", authData.getToken()).commit();
                            loginAuth.edit().putString("uID", authData.getUid()).commit();
                            loginAuth.edit().putLong("expires", authData.getExpires()).commit();
                            loginAuth.edit().putString("uName", txtEmail.getText().toString()).commit();
                            startActivity(selectRoom);
                        }

                        @Override
                        public void onAuthenticationError(FirebaseError firebaseError) {
                            // there was an error
                            Log.d("Login ERROR: ", firebaseError.getDetails());
                            Snackbar.make(v, firebaseError.getMessage(), Snackbar.LENGTH_INDEFINITE).show();
                        }
                    });
        }
        else if(validForm()){   // Pre Check input for syntax
            userFBRef.createUser(txtEmail.getText().toString(), txtPass.getText().toString(),
                    new Firebase.ValueResultHandler<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    Snackbar.make(v, "Account Created Please Login", Snackbar.LENGTH_INDEFINITE).show();
                    txtRepeatPass.setVisibility(View.GONE);
                    btnSubmit.setText(R.string.submit_button_default);
                    checkNewUser.setChecked(true);
                    Log.d("Register Success: ", "Successfully created user account with uid: " + result.get("uid"));
                }
                @Override
                public void onError(FirebaseError firebaseError) {
                    Log.d("Register ERROR: ", firebaseError.getMessage());
                    Snackbar.make(v, firebaseError.getMessage(), Snackbar.LENGTH_INDEFINITE).show();
                }
            });
        }
        else{
            Snackbar.make(v, "Invalid Account Data", Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    /**
     * Validate form data for account login/register
     * @return if form data is relatively valid
     */
    public Boolean validForm(){
        if(txtPass.getText().length()<4)
            return false;
        else if(txtPass.getText().toString().equals(txtRepeatPass.getText().toString()))
            return true;
        else
            return false;
    }

    /**
     * Listener for new account checkbox - checked changed.
     */
    public class LoginOrRegisterChanged implements CompoundButton.OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked) {
                txtRepeatPass.setVisibility(View.GONE);
                btnSubmit.setText(R.string.submit_button_default);
            }
            else{
                txtRepeatPass.setVisibility(View.VISIBLE);
                btnSubmit.setText(R.string.submit_button_register);
            }
        }
    }
}