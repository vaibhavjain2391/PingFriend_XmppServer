package com.example.vaibhavjain.pingfriend;

import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.android.internal.telephony.ITelephony;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * Created by abhilash on 20/09/15.
 */
public class CallReceiver extends PhonecallReceiver {
    MessageSender messageSender;
    GoogleCloudMessaging gcm;
    static String TAG = "chup";

    @Override
    protected void onIncomingCallStarted(Context ctx, String number, Date start) {
        ITelephony telephonyService;
        Log.i(TAG, "Incoming call started");
        Log.i(TAG, "From Phone Number: " + number);
        appendLog(System.currentTimeMillis() + " Incoming_call_from_number:" + number, "sdcard/pingfrnd_CallIn.file");
        //boolean sendPing = ((MyApplication)ctx).fwdPing();
       /* boolean sendPing = true;
        Log.i(TAG, "Forward call ? " + sendPing);
        messageSender = new MessageSender();
        gcm = GoogleCloudMessaging.getInstance(ctx);
        if(sendPing == true)
        {
            Bundle dataBundle = new Bundle();
            dataBundle.putString("ACTION", "CHAT");
            dataBundle.putString("TOUSER", "mona"); //hardcoding
            dataBundle.putString("CHATMESSAGE", "Enable Cellular");
            dataBundle.putString("FROMPHONE", number);
            Log.i(TAG, "Sending notification");
            messageSender.sendMessage(dataBundle, gcm);
        }

        TelephonyManager telephony = (TelephonyManager)
                ctx.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class c = Class.forName(telephony.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            telephonyService = (ITelephony) m.invoke(telephony);
            //telephonyService.silenceRinger();
            telephonyService.endCall();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
        Log.i(TAG, "Outgoing call started");
       // appendLog(System.currentTimeMillis() + " Outgoing call started ");
        appendLog(System.currentTimeMillis() + " Outgoing_Call_started", "sdcard/pingfrnd_CallOut.file");
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
        Log.i(TAG, "Incoming call ended");
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
        Log.i(TAG, "Outgoing call ended");
       // appendLog(System.currentTimeMillis() + " Outgoing call ended ");
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start) {
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
