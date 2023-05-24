package kr.co.bbmc.paycastagent;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

public class ScAlarmReceiver extends BroadcastReceiver {
    public static int REQUEST_CODE = 12345;
    private static String TAG = "ScAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (context.getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName()))
                return ;
        }

        Intent i = new Intent(context, AgentService.class);
        context.startService(i);
        Log.d(TAG, "ScAlarmReceiver() call");
    }
}
