package kr.co.bbmc.paycastagent;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import kr.co.bbmc.selforderutil.AuthKeyFile;
import kr.co.bbmc.selforderutil.NetworkUtil;
import kr.co.bbmc.selforderutil.ProductInfo;
import kr.co.bbmc.selforderutil.PropUtil;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private static String TAG = "MyFirebaseInstanceIDService";
    private boolean LOG  = false;



    @Override

    public void onTokenRefresh() {

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        if(LOG)
            Log.e(TAG, "Refreshed token: " + refreshedToken);

        sendRegistrationToServer(refreshedToken);

    }



    private void sendRegistrationToServer(String token) {
        ProductInfo pInfo = AuthKeyFile.getProductInfo();
        if(pInfo!=null)
        {
            AuthKeyFile.onSetFcmToken(token);
            String serverUrl = AuthKeyFile.getAuthRegFCMTokenServer();
            String queryString = AuthKeyFile.getAuthTokenParam();
            String ssl = PropUtil.configValue(getApplicationContext().getString(kr.co.bbmc.selforderutil.R.string.serverSSLEnabled), getApplicationContext());

            if (Boolean.valueOf(ssl)) {
                serverUrl = serverUrl.replace("http://", "https://");
            }
            //-
            URI url = null;
            try {
                url = new URI(serverUrl + queryString);
//                url = new URI(reqUrl+encodedQueryString);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            String response = NetworkUtil.sendFCMTokenToAuthServer(serverUrl, queryString);
            if((response!=null)&&(!response.isEmpty()))
            {
                if(LOG)
                    Log.d(TAG, "sendRegistrationToServer() response="+ response);
            }
        }

    }
}
