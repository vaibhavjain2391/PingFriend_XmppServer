package com.example.vaibhavjain.pingfriend;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Button;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Joe on 5/28/2014.
 */
public class GCMNotificationIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GCMNotificationIntentService() {
        super("GcmIntentService");
    }

    public static final String TAG = "GCMNotificationIntentService";

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent "+intent.getDataString());
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        if (extras != null) {
            if (!extras.isEmpty()) {
                if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
                        .equals(messageType)) {
                    sendNotification("Send error: " + extras.toString());
                } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
                        .equals(messageType)) {
                    sendNotification("Deleted messages on server: "
                            + extras.toString());
                } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
                        .equals(messageType)) {

                    if("USERLIST".equals(extras.get("SM"))){
                        Log.d(TAG, "onHandleIntent - USERLIST ");
                        //update the userlist view
                        Intent userListIntent = new Intent("com.example.vaibhavjain.pingfriend.userlistactivity");
                        String userList = extras.get("USERLIST").toString();
                        userListIntent.putExtra("USERLIST",userList);
                        sendBroadcast(userListIntent);
                    } else if("CHAT".equals(extras.get("SM"))){


                        appendLog(System.currentTimeMillis() + " Call_Notification","sdcard/pingfrnd_pingIn.file" );

                       /* Intent mServiceIntent = new Intent(this, SignalLogging.class);
                        stopService(mServiceIntent);*/

                        Log.d(TAG, "onHandleIntent - CHAT ");
                        String callerPhoneNo = extras.get("FROMPHONE").toString();
                        Log.d(TAG, "Caller No : " + callerPhoneNo);
                      /*  Intent chatIntent = new Intent("com.example.vaibhavjain.pingfriend.chatmessage");
                        chatIntent.putExtra("CHATMESSAGE", extras.get("CHATMESSAGE").toString());
                        sendBroadcast(chatIntent);*/
                        //sendNotification("SERVER_MESSAGE: " + extras.get("CHATMESSAGE").toString());
                        try{
                            Process su = Runtime.getRuntime().exec("su");
                            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

                            outputStream.writeBytes("settings put global airplane_mode_on 0\n");
                            outputStream.flush();
                            outputStream.writeBytes("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false\n");
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

                        Context context=getApplicationContext();
                        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                        int networkType = tm.getNetworkType();
                        Log.i(CallReceiver.TAG, "enabling cellular");
                        while(networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN)
                        {
                            networkType = tm.getNetworkType();
                            Log.d(TAG,"Network not connected\n");
                        }

                        appendLog(System.currentTimeMillis() + " Cellular_Enabled ", "sdcard/pingfrnd_cellEn.file");
                        Log.d(TAG, "Network connected\n");

                        Log.i(CallReceiver.TAG, "cellular enabled");
                       // sendNotification(callerPhoneNo);
                        //comment for proxy test
                        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + callerPhoneNo));
                        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(callIntent);
                    }
                    Log.i(TAG, "SERVER_MESSAGE: " + extras.toString());

                }
            }
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String msg) {
        Log.d(TAG, "Preparing to send notification...: " + msg);
        mNotificationManager = (NotificationManager) this
                .getSystemService(Context.NOTIFICATION_SERVICE);

       /* PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, SignUpActivity.class), 0);*/

       /* PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(android.provider.Settings.ACTION_SETTINGS), 0);*/

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + msg)), 0);

        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE); //TYPE_NOTIFICATION

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                this).setSmallIcon(R.drawable.gcm_cloud)
                .setContentTitle("You Got a Call From")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setContentText(msg)
                .setAutoCancel(true)
                .setSound(notificationSound);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        /*Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();*/
        Log.d(TAG, "Notification sent successfully.");
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
}

