package com.mobileapplication.streetassist.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager{
    private static final String PREF_NAME = "StreetAssistPrefs";
    private static final String KEY_INTRO_SEEN = "intro_seen";
    private static final String KEY_REMEMBER_ME_RESIDENT = "remember_me_resident";
    private static final String KEY_SAVED_EMAIL_RESIDENT = "saved_email_resident";
    private static final String KEY_REMEMBER_ME_ADMIN = "remember_me_admin";
    private static final String KEY_SAVED_EMAIL_ADMIN = "saved_email_admin";

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
    public void setRememberMe(String role, boolean isChecked, String email) {
        String keyRemember = "admin".equalsIgnoreCase(role) ? KEY_REMEMBER_ME_ADMIN : KEY_REMEMBER_ME_RESIDENT;
        String keyEmail = "admin".equalsIgnoreCase(role) ? KEY_SAVED_EMAIL_ADMIN : KEY_SAVED_EMAIL_RESIDENT;
        
        editor.putBoolean(keyRemember, isChecked);
        if (isChecked) {
            editor.putString(keyEmail, email);
        } else {
            editor.remove(keyEmail);
        }
        editor.apply();
    }

    public boolean isRememberMeChecked(String role) {
        String key = "admin".equalsIgnoreCase(role) ? KEY_REMEMBER_ME_ADMIN : KEY_REMEMBER_ME_RESIDENT;
        return prefs.getBoolean(key, false);
    }

    public String getSavedEmail(String role) {
        String key = "admin".equalsIgnoreCase(role) ? KEY_SAVED_EMAIL_ADMIN : KEY_SAVED_EMAIL_RESIDENT;
        return prefs.getString(key, "");
    }
}
