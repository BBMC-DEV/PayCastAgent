package kr.co.bbmc.paycastagent;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingEnvPersister {
    //private static final String MMSAPK_PREFERENCESNAME = "co.kr.bbmc.paycastagent_preferences";
    private static SharedPreferences mPrefs;
    private static SharedPreferences.Editor mPrefsEditor;
    private static Context mContext;

    private static final String FCM_MSG_RECEIVED = "pref_key_fcm_message_receive";


    private SettingEnvPersister() {

    }

    public static void initPrefs(Context context) {
        if( mPrefs == null){
            //version 7.1.2 로 변경되면서 퍼미션 error 발생으로 수정됨.
            mPrefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
            //mPrefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_WORLD_READABLE);
            mPrefsEditor = mPrefs.edit();
        }

    }

    /**
     *  Received FCM Message
     * @return
     */
    public static boolean getSettingFcmMsgReceived() {
        // false : not received
        // true : received
        return mPrefs.getBoolean(FCM_MSG_RECEIVED, false);
    }
    /**
     *  Received FCM Message
     * @return
     */
    public static void setSettingFcmMsgReived(boolean fcmMsgReived) {
        // false : not received
        // true : received
        mPrefsEditor = mPrefs.edit();
        mPrefsEditor.putBoolean(FCM_MSG_RECEIVED, fcmMsgReived);
        mPrefsEditor.commit();

    }
}
