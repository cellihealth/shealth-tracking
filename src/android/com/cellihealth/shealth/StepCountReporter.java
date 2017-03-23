package com.cellihealth.shealth;

import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataObserver;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataResolver.Filter;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadRequest;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadResult;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthDevice;
import com.samsung.android.sdk.healthdata.HealthResultHolder;
import com.samsung.android.sdk.healthdata.HealthDeviceManager;


import android.database.Cursor;
import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StepCountReporter {
    private static String LOG_TAG = "StepCountReporter";
    private final HealthDataStore mStore;
    public Shealth sHealth = null;

    public StepCountReporter(HealthDataStore store, Shealth shealth) {
        mStore = store;
        sHealth = shealth;
    }

    public void start() {
        // Register an observer to listen changes of step count and get today step count
        // HealthDataObserver.addObserver(mStore, HealthConstants.StepCount.HEALTH_DATA_TYPE, mObserver);
        readStepCount();
    }

    public void startReadStepCount(String startDate, String endDate) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range from start time of today to the current time
        long startTime = setToMiliseconds(startDate);
        long endTime = setToMiliseconds(endDate) +  ( 2 * 24 * 60 * 60 * 1000 );
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.StepCount.START_TIME, startTime),
                Filter.lessThanEquals(HealthConstants.StepCount.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.StepCount.COUNT,
                        HealthConstants.StepCount.START_TIME,
                        HealthConstants.StepCount.END_TIME,
                        HealthConstants.StepCount.TIME_OFFSET,
                        HealthConstants.StepCount.DEVICE_UUID
                })
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(mListener);

            Log.e(LOG_TAG, "Getting step count success.");
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getClass().getName() + " - " +  e.getMessage());
            Log.e(LOG_TAG, "Getting step count fails.");
            sHealth.globalCallbackContext.error("Getting step count fails.");
        }
    }

    // Read the today's step count on demand
    private void readStepCount() {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range from start time of today to the current time
        long startTime = getStartTimeOfToday() - ( 7 * 24 * 60 * 60 * 1000 );
        long endTime = System.currentTimeMillis();
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.StepCount.START_TIME, startTime),
                                   Filter.lessThanEquals(HealthConstants.StepCount.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                                                        .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                                                        .setProperties(new String[]{
                                                                HealthConstants.StepCount.COUNT,
                                                                HealthConstants.StepCount.START_TIME,
                                                                HealthConstants.StepCount.END_TIME,
                                                                HealthConstants.StepCount.TIME_OFFSET,
                                                                HealthConstants.StepCount.DEVICE_UUID
                                                        })
                                                        .setFilter(filter)
                                                        .build();

        try {
            resolver.read(request).setResultListener(mListener);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.e(LOG_TAG, "Getting step count fails.");
            sHealth.globalCallbackContext.error("Getting step count fails.");
        }
    }

    private long getStartTimeOfToday() {
        Calendar today = Calendar.getInstance();

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return today.getTimeInMillis();
    }

    private final HealthResultHolder.ResultListener<ReadResult> mListener = new HealthResultHolder.ResultListener<ReadResult>() {
        @Override
        public void onResult(ReadResult result) {
            String resultSet = "[";

            Cursor c = null;

            HealthDeviceManager deviceManager = new HealthDeviceManager(mStore);

            try {
                c = result.getResultCursor();

                Log.d(LOG_TAG, "Column Names" + Arrays.toString(c.getColumnNames()));

                if (c != null) {
                    String deviceName = "";
                    String deviceManufacturer = "";
                    String deviceModel = "";
                    String groupName="";
                    Integer deviceGroup;
                    byte[] dataText = null;

                    while (c.moveToNext()) {

                        deviceName = deviceManager.getDeviceByUuid(c.getString(c.getColumnIndex(HealthConstants.StepCount.DEVICE_UUID))).getCustomName();
                        deviceManufacturer = deviceManager.getDeviceByUuid(c.getString(c.getColumnIndex(HealthConstants.StepCount.DEVICE_UUID))).getManufacturer();
                        deviceModel = deviceManager.getDeviceByUuid(c.getString(c.getColumnIndex(HealthConstants.StepCount.DEVICE_UUID))).getModel();
                        deviceGroup = deviceManager.getDeviceByUuid(c.getString(c.getColumnIndex(HealthConstants.StepCount.DEVICE_UUID))).getGroup();

                        if (deviceName == null) {
                            deviceName = "";
                        }

                        if (deviceManufacturer == null) {
                            deviceManufacturer = "";
                        }

                        if (deviceModel == null) {
                            deviceModel = "";
                        }
                        switch(deviceGroup){
                            case HealthDevice.GROUP_MOBILE:
                                groupName = "mobileDevice";
                                break;
                            case HealthDevice.GROUP_EXTERNAL:
                                groupName = "peripheral";
                                break;
                            case HealthDevice.GROUP_COMPANION:
                                groupName = "wearable";
                                break;
                            case HealthDevice.GROUP_UNKNOWN:
                                groupName = "unknown";
                                break;
                        }

                        resultSet += "{ offset: " +
                                c.getString(c.getColumnIndex(HealthConstants.StepCount.TIME_OFFSET)) + ", start:" +
                                c.getString(c.getColumnIndex(HealthConstants.StepCount.START_TIME)) + ", end:" +
                                c.getString(c.getColumnIndex(HealthConstants.StepCount.END_TIME)) + ", step:" +
                                c.getInt(c.getColumnIndex(HealthConstants.StepCount.COUNT)) + ", deviceName:\"" +
                                deviceName + "\", deviceManufacturer:\"" + deviceManufacturer + "\", deviceModel:\"" +
                                deviceModel + "\", deviceGroup: \""+ groupName + "\" }, ";
                    }

                } else {
                    Log.d(LOG_TAG, "The cursor is null.");
                }
            }
            catch(Exception e) {
                Log.e(LOG_TAG, e.getClass().getName() + " - " + e.getMessage());
            }
            finally {
                if (c != null) {
                    c.close();
                }
            }
            resultSet += "]";

            Log.d(LOG_TAG, "Steps Result: " + resultSet);
            try{
                JSONArray p = new JSONArray(resultSet);
                sHealth.drawStepCount(p);
            }catch (JSONException e){
                LOG.d(LOG_TAG, "JSONException on resultSet.");
            }
        }
    };

    private final HealthDataObserver mObserver = new HealthDataObserver(null) {

        // Update the step count when a change event is received
        @Override
        public void onChange(String dataTypeName) {
            Log.d(LOG_TAG, "Observer receives a data changed event");
            readStepCount();
        }
    };

    private long setToMiliseconds(String str) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        long miliseconds = 0;

        try {
            Date date = format.parse(str);
            miliseconds = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return miliseconds;
    }


}
