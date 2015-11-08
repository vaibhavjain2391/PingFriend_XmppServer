package com.example.vaibhavjain.pingfriend;

import android.app.Application;

/**
 * Created by Vaibhav Jain on 10/7/2015.
 */
public class MyApplication extends Application {
    private boolean pingForwarder=true;

    public boolean fwdPing() {
        return pingForwarder;
    }

    public void setPingForwarder(boolean val) {
        this.pingForwarder = val;
    }
 }
