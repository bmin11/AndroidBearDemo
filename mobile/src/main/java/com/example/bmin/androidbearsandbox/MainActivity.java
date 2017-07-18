package com.example.bmin.androidbearsandbox;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends FragmentActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        View.OnClickListener,
        DataApi.DataListener {


    public static final String MOBILE_MAIN = "MobileMain"; //for logging purpose
    private static final String COUNT_KEY = "com.example.key.count";

    private Node mNode; //Used for storing all detected wearable devices

    private Button btConnectionCheck;
    private TextView tvConnectionResult;

    private GoogleApiClient mGoogleApiClient; //Manages communication between app and wearable

    private int counter = 0;
    private TextView tvCountResult;
    private Button btCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvConnectionResult = (TextView) findViewById(R.id.tvConnectionResult);
        btConnectionCheck = (Button) findViewById(R.id.btConnectionCheck);
        btConnectionCheck.setOnClickListener(this);

        // Create and specify to use wearable api and add listeners
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        tvCountResult = (TextView) findViewById(R.id.tvCountResult);
        btCounter = (Button) findViewById(R.id.btCounter);
        btCounter.setOnClickListener(this);

    }

    // Attempt connection on start of the application
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    // Remove the dataapi listener and close connection on pause
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    // Attempt connection on resume of the application
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    // Remove the dataapi listener and close connection on stop
    protected void onStop() {
        super.onStop();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    /*
     If the connection was successful, detect all devices it is connected
     and print out the near by device's name
      */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient)
                .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(@NonNull NodeApi.GetConnectedNodesResult nodes) {
                        for (Node node : nodes.getNodes()) {
                            if (node != null && node.isNearby()) {
                                mNode = node;
                                Log.d(MOBILE_MAIN, getString(R.string.connection_label) + mNode.getDisplayName());
                                tvConnectionResult.setText(getString(R.string.connection_label) + mNode.getDisplayName());
                            }
                        }
                        if (mNode == null) {
                            Log.d(MOBILE_MAIN, getString(R.string.connection_fail));
                            tvConnectionResult.setText(getString(R.string.connection_fail));
                        }
                    }
                });
        // Add a listener for detecting any data changes on the datamap
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            /*
            When the button for connection check is pushed, detect all devices it is connected
            and print out the near by device's name
             */
            case R.id.btConnectionCheck:
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient)
                        .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                            @Override
                            public void onResult(@NonNull NodeApi.GetConnectedNodesResult nodes) {
                                for (Node node : nodes.getNodes()) {
                                    if (node != null && node.isNearby()) {
                                        mNode = node;
                                        Log.d(MOBILE_MAIN, getString(R.string.connection_label) + mNode.getDisplayName());
                                        tvConnectionResult.setText(getString(R.string.connection_label) + mNode.getDisplayName());
                                    }
                                }
                                if (mNode == null) {
                                    Log.d(MOBILE_MAIN, getString(R.string.connection_fail));
                                    tvConnectionResult.setText(getString(R.string.connection_fail));
                                }
                            }
                        });
                break;
            /*
            When the button for increment counter is pushed, increment the counter varaible by 1
            and create a datamap request, update the counter variable on the request,
            and finally, put the request on pending.
             */
            case R.id.btCounter:
                counter++;
                PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/count"); //datamap operates on uri format
                putDataMapReq.getDataMap().putInt(COUNT_KEY, counter);
                // Push request on pending
                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                PendingResult<DataApi.DataItemResult> pendingResult =
                        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
                // Listen if the pending request is resolved
                pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                        if(dataItemResult.getStatus().isSuccess()) {
                            Log.d(MOBILE_MAIN, getString(R.string.dataitem_label) + dataItemResult.getDataItem().getUri());
                        }
                    }
                });
                break;
        }
    }

    /*
    Whenever a data on the datamap has been changed, grab the updated data
    and print out the data on the screen
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/count") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    tvCountResult.setText(String.valueOf(dataMap.getInt(COUNT_KEY)));
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }
}
