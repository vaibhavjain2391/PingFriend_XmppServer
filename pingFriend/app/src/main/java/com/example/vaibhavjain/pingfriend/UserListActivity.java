package com.example.vaibhavjain.pingfriend;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class UserListActivity extends ListActivity {
    private static final String TAG = "UserListActivity";
    TextView content;
    Button refreshButton;
    private Intent intent;
    MessageSender messageSender;
    GoogleCloudMessaging gcm;
    String myPhoneNumber;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        content = (TextView)findViewById(R.id.output);
        content.setText("Select user to notify:");
        refreshButton = (Button)findViewById(R.id.refreshButton);
        intent = new Intent(this, GCMNotificationIntentService.class);
        registerReceiver(broadcastReceiver, new IntentFilter("com.example.vaibhavjain.pingfriend.userlistactivity"));
        messageSender = new MessageSender();
        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
        TelephonyManager tMgr = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        myPhoneNumber = tMgr.getLine1Number();
        //Log.d(TAG, "My Phone No: " + myPhoneNumber);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // get user list
                Bundle dataBundle = new Bundle();
                dataBundle.putString("ACTION", "USERLIST");
                messageSender.sendMessage(dataBundle, gcm);
            }
        });

    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent.getStringExtra("USERLIST"));
            updateUI(intent.getStringExtra("USERLIST"));
        }
    };

    private void updateUI(String userList) {
        //get userlist from the intents and update the list

        String[] userListArr = userList.split(":");

        Log.d(TAG, "userListArr: " + userListArr.length + " tostr " + userListArr.toString());

        //remove empty strings :-)
        List<String> list = new ArrayList<String>();
        for(String s : userListArr) {
            if(s != null && s.length() > 0) {
                list.add(s);
            }
        }
        userListArr = list.toArray(new String[list.size()]);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, userListArr);
        setListAdapter(adapter);

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        super.onListItemClick(l, v, position, id);

        // ListView Clicked item index
        int itemPosition     = position;

        // ListView Clicked item value
        String  itemValue    = (String) l.getItemAtPosition(position);
        content.setText("User selected: " +itemValue);


       /* Intent i = new Intent(getApplicationContext(),
                ChatActivity.class);
        i.putExtra("TOUSER",itemValue);
        startActivity(i);
        finish();*/

        GoogleCloudMessaging gcm= GoogleCloudMessaging.getInstance(getApplicationContext());
        //sending gcm message to the paired device
        Bundle dataBundle = new Bundle();
        dataBundle.putString("ACTION", "CHAT");
        dataBundle.putString("TOUSER", itemValue);
        dataBundle.putString("CHATMESSAGE", "Enable Celular");
        dataBundle.putString("FROMPHONE", myPhoneNumber);

        Log.i(CallReceiver.TAG, "Send chat message");
        appendLog(System.currentTimeMillis() + " Sending_Ping", "sdcard/pingfrnd_pingSent.file");
        messageSender.sendMessage(dataBundle, gcm);


    }

    public void appendLog(String text, String filename)
    {
        File logFile = new File("sdcard/filename");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean disableCellular(View v){

        //Settings.Global.putInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);

        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes("settings put global airplane_mode_on 1\n");
            outputStream.flush();
            outputStream.writeBytes("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true\n");
            outputStream.flush();
            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
        }catch(IOException e){
            //throw new Exception(e);
            Log.d(TAG,"Error in executing shell command\n");
        }catch(InterruptedException e){
            //throw new Exception(e);
            Log.d(TAG,"Error in executing shell command\n");
        }

        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        //startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
        return true;

    }

    public boolean autoDisEnCellular(View v)
    {
        Intent mServiceIntent = new Intent(this, SignalLogging.class);
        Button tv = (Button) findViewById(R.id.autoButton);
        //tv.setText("Disable Auto Mode");
        if((tv.getText()).equals("Enable Auto Mode"))
        {
            startService(mServiceIntent);
            tv.setText("Disable Auto Mode");
        }
        else {
            stopService(mServiceIntent);
            tv.setText("Enable Auto Mode");
        }

        return true;
    }



}
