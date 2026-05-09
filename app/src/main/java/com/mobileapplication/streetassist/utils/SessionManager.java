package com.mobileapplication.streetassist.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager{
    private static final String PREF_NAME = "StreetAssistPrefs";
    private static final String KEY_INTRO_SEEN = "intro_seen";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_SAVED_EMAIL = "saved_email";

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

    // Save remember me preference
    public void setRememberMe(boolean isChecked, String email) {
        editor.putBoolean(KEY_REMEMBER_ME, isChecked);
        if (isChecked) {
            editor.putString(KEY_SAVED_EMAIL, email);
        } else {
            editor.remove(KEY_SAVED_EMAIL);
        }
        editor.apply();
    }

    public boolean isRememberMeChecked() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }

    public String getSavedEmail() {
        return prefs.getString(KEY_SAVED_EMAIL, "");
    }
}
