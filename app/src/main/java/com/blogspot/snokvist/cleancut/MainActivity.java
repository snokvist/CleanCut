package com.blogspot.snokvist.cleancut;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.CartesianFloat;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.Bmi160Gyro;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.processor.Average;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements ServiceConnection {
    private static final String LOG_TAG = "CleanCut";
    private static final String ACCEL_DATA = "accel_data";
    private static final String GYRO_DATA = "accel_data";
    private static final String ACCEL_GYRO_DATA = "accel_gyro_data";
    private static int gyroCounter = 0;
    private static int accCounter = 0;
    private final ArrayList<String> accAxisData = new ArrayList<>();
    private final ArrayList<String> gyroAxisData = new ArrayList<>();


    private MetaWearBleService.LocalBinder serviceBinder;
    private MetaWearBoard mwBoard;
    private Bmi160Accelerometer accelModule;
    private Bmi160Gyro gyroModule;
    private Debug debugModule;
    //test

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);

        findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    gyroModule = mwBoard.getModule(Bmi160Gyro.class);
                    gyroModule.configure()
                            .setFullScaleRange(Bmi160Gyro.FullScaleRange.FSR_2000)
                            .setOutputDataRate(Bmi160Gyro.OutputDataRate.ODR_50_HZ)
                            .commit();
                    gyroModule.routeData().fromAxes()
                            .stream(GYRO_DATA).commit()
                            .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe(GYRO_DATA, new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message message) {
                                            CartesianFloat axes = message.getData(CartesianFloat.class);
                                            gyroCounter = gyroCounter + 1;
                                            //Log.i(LOG_TAG, "GYRO" + Integer.toString(gyroCounter) + axes.toString());
                                            gyroAxisData.add(axes.toString());
                                        }
                                    });
                                }

                            });
                } catch (UnsupportedModuleException e) {
                    Log.i(LOG_TAG, "Cannot find Gyro module.", e);
                }

                try {
                    accelModule = mwBoard.getModule(Bmi160Accelerometer.class);
                    accelModule.configureAxisSampling()
                            .setFullScaleRange(Bmi160Accelerometer.AccRange.AR_16G)
                            .setOutputDataRate(Bmi160Accelerometer.OutputDataRate.ODR_50_HZ)
                            .commit();

                    accelModule.routeData().fromAxes()
                            .stream(ACCEL_DATA).commit()
                            .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                                @Override
                                public void success(RouteManager result) {
                                    result.subscribe(ACCEL_DATA, new RouteManager.MessageHandler() {
                                        @Override
                                        public void process(Message message) {
                                            CartesianFloat axes = message.getData(CartesianFloat.class);
                                            accCounter = accCounter + 1;
                                            //Log.i(LOG_TAG, "ACCEL" + Integer.toString(accCounter) + axes.toString());
                                            accAxisData.add(axes.toString());
                                        }
                                    });
                                }
                            });
                } catch (UnsupportedModuleException e) {
                    Log.i(LOG_TAG, "Cannot find Accelerometer module.", e);
                }

                accelModule.enableAxisSampling();
                accelModule.start();
                gyroModule.start();
            }
        });

        findViewById(R.id.stopButton).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                accelModule.stop();
                accelModule.disableAxisSampling();
                gyroModule.stop();
                gyroAxisData.clear();
                accAxisData.clear();

            }
        });

        findViewById(R.id.resetButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugModule.resetDevice();
            }
        });


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView gyroText = (TextView) findViewById(R.id.gyroData);
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                if (gyroAxisData.size() >= 1) {
                    gyroText.setText(gyroAxisData.get(gyroAxisData.size() - 1));
                } else {
                    gyroText.setText("NO DATA");
                }


            }
        });


        Thread gyroThread = new Thread() {

            @Override
            public void run() {
                TextView gyroText = (TextView) findViewById(R.id.gyroData);
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView gyroText = (TextView) findViewById(R.id.gyroData);
                                if (gyroAxisData.size() >= 1) {
                                    gyroText.setText("Gyro: " + gyroAxisData.get(gyroAxisData.size() - 1));
                                } else {
                                    //accText.setText("NO DATA");
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Log.i(LOG_TAG, "Error running AccelThread");
                }
            }
        };

        Thread accThread = new Thread() {

            @Override
            public void run() {

                try {
                    while (!isInterrupted()) {

                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView accText = (TextView) findViewById(R.id.accData);
                                    if (gyroAxisData.size() >= 1) {
                                    accText.setText("Accelerometer: " + accAxisData.get(accAxisData.size() - 1));
                                } else {
                                    //accText.setText("NO DATA");
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Log.i(LOG_TAG, "Error running AccelThread");
                }
            }
        };

        gyroThread.start();
        accThread.start();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApplicationContext().unbindService(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceBinder = (MetaWearBleService.LocalBinder) service;
        String mwMacAdress = "C7:74:D3:36:CF:B1";

        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothDevice btDevice = btManager.getAdapter().getRemoteDevice(mwMacAdress);

        mwBoard = serviceBinder.getMetaWearBoard(btDevice);

        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
            @Override
            public void connected() {
                Log.i(LOG_TAG, "Connected.");
            }

            @Override
            public void disconnected() {
                Log.i(LOG_TAG, "Disconnected.");
            }


        });

        try {
            debugModule = mwBoard.getModule(Debug.class);
        } catch (UnsupportedModuleException e) {
            e.printStackTrace();
        }

        mwBoard.connect();

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
