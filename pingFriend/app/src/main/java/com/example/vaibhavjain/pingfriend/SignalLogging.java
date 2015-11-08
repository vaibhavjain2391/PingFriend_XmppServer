package com.example.vaibhavjain.pingfriend;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by ningoo on 7/9/15.
 */
public class SignalLogging extends Service {
    String LOG_TAG = "SignalLogger";

    final static String STOP_LOGGING = "com.example.vaibhavjain.pingfriend.STOP_LOGGING";

    private boolean WRITE_LOGCAT = true;
    private boolean WRITE_LOGFILE = true;

    private TelephonyManager tm;
    private BufferedWriter writer;
    private PhoneStateListener phoneStateListener;
    private BroadcastReceiver wifiReceiver;
    private BroadcastReceiver stopReceiver;
    static int prevRssi=-1;
    public SignalLogging() {
    }

    private void log(String msg) {
        if(WRITE_LOGCAT) {
            Log.i(LOG_TAG, msg);
        }
        if(WRITE_LOGFILE) {
            try {
                writer.write(msg + '\n');
            } catch (IOException e) {
                Log.e(LOG_TAG, "error in write msg: " + msg);
            }
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        //String fileName = "signal.log";
        setupSignalListeners();
        return START_STICKY;
    }

    public void setupSignalListeners() {
        if(WRITE_LOGFILE) {
            initLogWriter();
        }
        //setupCellularListener();
        setupWifiListener();
        setupStopListener();
        Log.i(LOG_TAG, "SignalLogging setup done");
    }

    private void initLogWriter() {
        String logFileName = "signal_log";
        File logFileDir = getFilesDir();
        logFileDir.mkdirs();
        File logFile = new File(logFileDir, logFileName);
        try {
            writer = new BufferedWriter(new FileWriter(logFile));
            //FileOutputStream stream = openFileOutput(logFileName, Context.MODE_PRIVATE);
            //writer = new OutputStreamWriter(stream);
        } catch(IOException e) {
            Log.e(LOG_TAG, "error in open file " + logFileName);
        }
        // make the logFile world readable
        if(!logFile.setReadable(true, false)) {
            Log.e(LOG_TAG, "set log readable fails");
        }
        else {
            Log.d(LOG_TAG, "set log readable OK");
        }
    }

    private void setupCellularListener() {
        tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength ss) {
                if (ss != null) {
                    String sss[] = ss.toString().split(" ");
                    boolean gsm_valid = (ss.getGsmSignalStrength() != 99);
                    int gsm_dbm = ss.getGsmSignalStrength() * 2 - 113;
                    //int cdma_dbm = ss.getCdmaDbm();
                    //int evdo_dbm = ss.getEvdoDbm();
                    String msg = null;
                    if (sss.length >= 10) {
                        msg = Long.toString(System.currentTimeMillis()) + " lte " + sss[9];
                        /*if(gsm_valid) {
                            //msg = Long.toString(System.currentTimeMillis()) + " lte " + sss[8] + " " + sss[9] + " " + sss[10];
                            msg = Long.toString(System.currentTimeMillis()) + " lte " + sss[9];
                        }
                        else {
                            msg = Long.toString(System.currentTimeMillis()) + " gsm " + Integer.toString(gsm_dbm) + " lte " + sss[8] + " " + sss[9] + " " + sss[10];
                        }*/
                    } else {
                        msg = Long.toString(System.currentTimeMillis()) + " gsm " + Integer.toString(gsm_dbm);
                    }
                    if(msg != null) {
                        log(msg);
                    }
                }
            }
        };
        tm.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        Log.d(LOG_TAG, "setupCellularListener done");
    }

    private void setupWifiListener() {
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        // log the initial wifi signal
        String msg = Long.toString(System.currentTimeMillis()) + " wifi " + Integer.toString(wm.getConnectionInfo().getRssi());
        // register wifi signal listener
        wifiReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1){
                int newRssi = arg1.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
                String msg = Long.toString(System.currentTimeMillis()) + " wifi " + Integer.toString(newRssi);

                if(newRssi >= -65 && (prevRssi<-65 || prevRssi==-1))
                {
                    disableCellular();
                    prevRssi = newRssi;
                }
                else if(newRssi < -85 && (prevRssi >= -85 || prevRssi==-1))
                {
                    enableCellular();
                    prevRssi = newRssi;
                }
                log(msg);
            }
        };
        this.registerReceiver(this.wifiReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
        Log.d(LOG_TAG, "setupWifiListener done");
    }

    private void setupStopListener() {
        stopReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1){
                stopSelf();
            }
        };
        this.registerReceiver(this.stopReceiver, new IntentFilter(this.STOP_LOGGING));

    }

    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void onDestroy() {
        //tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        this.unregisterReceiver(this.wifiReceiver);
        this.unregisterReceiver(this.stopReceiver);
        try {
            writer.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "error in close log file");
        }
        Log.i(LOG_TAG, "SignalLogging destroyed");
    }

    public boolean disableCellular(){

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
            appendLog(System.currentTimeMillis() + " Cellular disabled ");
        }catch(IOException e){
            //throw new Exception(e);
            Log.d(LOG_TAG,"Error in executing shell command\n");
        }catch(InterruptedException e){
            //throw new Exception(e);
            Log.d(LOG_TAG,"Error in executing shell command\n");
        }
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);


        //startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
        return true;

    }

    public boolean enableCellular(){
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
            appendLog(System.currentTimeMillis() + " Cellular enabled ");
        }catch(IOException e){
            //throw new Exception(e);
            Log.d(LOG_TAG,"Error in executing shell command\n");
        }catch(InterruptedException e){
            //throw new Exception(e);
            Log.d(LOG_TAG,"Error in executing shell command\n");
        }
       /* WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);*/
        //startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
        return true;
    }

    public void appendLog(String text)
    {
        File logFile = new File("sdcard/wifi2notifyCellularlog.file");
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
