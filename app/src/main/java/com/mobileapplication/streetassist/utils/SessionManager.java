package com.mobileapplication.streetassist.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager{
    private static final String PREF_NAME = "StreetAssistPrefs";
    private static final String KEY_INTRO_SEEN = "intro_seen";

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    public SessionManager(Context context){
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // check if intro already shown
    public boolean isIntroSeen(){
        return prefs.getBoolean(KEY_INTRO_SEEN, false);
    }

    // mark intro as seen
    public void setIntroSeen(){
        editor.putBoolean(KEY_INTRO_SEEN, true);
        editor.apply();
    }

}
