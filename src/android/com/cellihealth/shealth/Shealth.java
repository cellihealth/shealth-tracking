package com.cellihealth.shealth;

import org.apache.cordova.CordovaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import com.cellihealth.cellihealth.MainActivity;

import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataService;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionResult;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType;
import com.samsung.android.sdk.healthdata.HealthResultHolder;


/**
 * This class echoes a string called from JavaScript.
 */
public class Shealth extends CordovaPlugin {
    public Set<PermissionKey> mKeySet;
    private HealthDataStore mStore;
    private HealthConnectionErrorResult mConnError;
    private StepCountReporter mReporter;
    public CallbackContext globalCallbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("steps")) {
            // this.steps();
            String startDate = args.getJSONObject(0).getString("startDate");
            String endDate   = args.getJSONObject(0).getString("endDate");

            this.stepsSHealth(startDate, endDate);

            globalCallbackContext = callbackContext;
            return true;
        }else if(action.equals("connect")){
            this.connect();
            globalCallbackContext = callbackContext;
            return true;
        }
        return false;
    }
    
    private void connect() {
        mKeySet = new HashSet<PermissionKey>();
        mKeySet.add(new PermissionKey(HealthConstants.StepCount.HEALTH_DATA_TYPE, PermissionType.READ));
        HealthDataService healthDataService = new HealthDataService();
        try {
            healthDataService.initialize(MainActivity.mInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Create a HealthDataStore instance and set its listener
        mStore = new HealthDataStore(MainActivity.mInstance, mConnectionListener);
        // Request the connection to the health data store
        mStore.connectService();
    }
    

    @Override
    public void onDestroy() {
        mStore.disconnectService();
        super.onDestroy();
    }


    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {

        @Override
        public void onConnected() {
            Log.d(MainActivity.APP_TAG, "Health data service is connected.");
            HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
            mReporter = new StepCountReporter(mStore, Shealth.this);

            try {
                // Check whether the permissions that this application needs are acquired
                Map<PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(mKeySet);
                
                if (resultMap.containsValue(Boolean.FALSE)) {
                    // Request the permission for reading step counts if it is not acquired
                    pmsManager.requestPermissions(mKeySet, MainActivity.mInstance).setResultListener(mPermissionListener);
                } else {
                    // Get the current step count and display it
                    Log.d(MainActivity.APP_TAG, "COUNT THE STEPS!");
                    mReporter.start();
                }
            } catch (Exception e) {
                Log.e(MainActivity.APP_TAG, e.getClass().getName() + " - " + e.getMessage());
                Log.e(MainActivity.APP_TAG, "Permission setting fails.");
                globalCallbackContext.error("Permission setting fails.");
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(MainActivity.APP_TAG, "Health data service is not available.");
            globalCallbackContext.error("Health data service is not available.");
        }

        @Override
        public void onDisconnected() {
            Log.d(MainActivity.APP_TAG, "Health data service is disconnected.");
            globalCallbackContext.error("Health data service is disconnected.");
        }
    };
    
    private final HealthResultHolder.ResultListener<PermissionResult> mPermissionListener =
            new HealthResultHolder.ResultListener<PermissionResult>() {

        @Override
        public void onResult(PermissionResult result) {
            Log.d(MainActivity.APP_TAG, "Permission callback is received.");
            Map<PermissionKey, Boolean> resultMap = result.getResultMap();

            if (resultMap.containsValue(Boolean.FALSE)) {
                Log.e(MainActivity.APP_TAG, "NOT CONNECTED YET");
                showPermissionAlarmDialog();
            } else {
                Log.d(MainActivity.APP_TAG, "COUNT THE STEPS!");
                mReporter.start();
            }
        }
    };
    
    private void showPermissionAlarmDialog() {
    	globalCallbackContext.error("All permissions should be acquired");
//        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.mInstance);
//        alert.setTitle("Notice");
//        alert.setMessage("All permissions should be acquired");
//        alert.setPositiveButton("OK", null);
//        alert.show();
    }

    private void showConnectionFailureDialog(HealthConnectionErrorResult error) {

        //AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.mInstance);
        mConnError = error;
        String message = "Connection with S Health is not available";

        if (mConnError.hasResolution()) {
            switch(error.getErrorCode()) {
            case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                message = "Please install S Health";
                break;
            case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                message = "Please upgrade S Health";
                break;
            case HealthConnectionErrorResult.PLATFORM_DISABLED:
                message = "Please enable S Health";
                break;
            case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                message = "Please agree with S Health policy";
                break;
            default:
                message = "Please make S Health available";
                break;
            }
        }
        globalCallbackContext.error(message);

//        alert.setMessage(message);
//
//        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int id) {
//                if (mConnError.hasResolution()) {
//                    mConnError.resolve(MainActivity.mInstance);
//                }
//            }
//        });
//
//        if (error.hasResolution()) {
//            alert.setNegativeButton("Cancel", null);
//        }
//
//        alert.show();
    }

    
    private void steps() {
    	Log.d(MainActivity.APP_TAG, "steps");
    	mReporter.start();
    }

    private void stepsSHealth(String startDate, String endDate) {
        Log.d(MainActivity.APP_TAG, "steps");
        mReporter.startReadStepCount(startDate, endDate);
     }

    public void drawStepCount(JSONArray count){
    	Log.d(MainActivity.APP_TAG, "drawStepCount: " + count);
    	globalCallbackContext.success(count);
    }


}
