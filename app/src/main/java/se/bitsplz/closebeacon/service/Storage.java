package se.bitsplz.closebeacon.service;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author jonnakollin
 * @author j0na5L
 */
public final class Storage
{
    public static final String PREF_NAME = "se.bitsplz.closebeacon";
    public static final String PREF_NAME_REMEMBER = "";
    public static final int MODE = Context.MODE_PRIVATE;

    public static void writeToString(Context context, String key, String value)
    {
        getEditor(context).putString(key, value).commit();
    }

    public static String readString(Context context, String key, String value)
    {
        return getPreferences(context).getString(key, value);
    }

    private static SharedPreferences.Editor getEditor(Context context)
    {
        return getPreferences(context).edit();
    }

    private static SharedPreferences getPreferences(Context context)
    {
        return context.getSharedPreferences(PREF_NAME, MODE);
    }
}
