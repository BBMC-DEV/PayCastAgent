package kr.co.bbmc.paycastagent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

public class AgentActivity extends Activity {
    private AgentExternalVarApp mAgentExterVarApp;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAgentExterVarApp = (AgentExternalVarApp) getApplication();
        startService(new Intent(this, AgentService.class));
        finish();
    }
}
