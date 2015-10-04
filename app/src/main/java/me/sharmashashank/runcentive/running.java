package me.sharmashashank.runcentive;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.security.Security;
import java.util.Calendar;

import android.os.Handler;

public class running extends Activity implements LocationListener {

    boolean shouldStop = false;
    int timeStep = 5000;
    LocationManager mLocationManager;
    private final static String TAG = running.class.getName();
    Handler mHandler;
    TextView timerDisplay;
    TextView totalDistanceView;
    final long startTimeTimer = System.currentTimeMillis();
    boolean stillRunning=true;
    Location lastLocation;
    Double totalDistance=0.0;
    double kilos = 70;
    double moneyForNow = 0;
    double calorieFactor = 0.5;
    Handler colorChangeHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = null;


        mHandler = new Handler(Looper.getMainLooper());
        try {

            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,0,0,mLocationListener);
        }
        catch(SecurityException ex){
            ex.printStackTrace();
        }
        //startClock(this);
        Log.d(TAG, "Inside onCreate");
        mHandler= new Handler(Looper.getMainLooper());
        timerDisplay = (TextView) findViewById(R.id.timerTextView);
        totalDistanceView = (TextView) findViewById(R.id.totalDistanceTextView);
        Runnable runnable = new Runnable(){
          public void run(){
              timerDisplay = (TextView) findViewById(R.id.timerTextView);
              while(stillRunning) {
                  long timePassed = System.currentTimeMillis() - startTimeTimer;
                  int seconds = (int) (timePassed / 1000) %60;
                 int minutes = (int) (timePassed / 1000) / 60;
                  Log.d("HELLO", "" + seconds + " " + minutes);
                  setText(minutes,seconds);
                  try {
                      Thread.sleep(500);
                  }
                  catch(InterruptedException ex){
                      Log.d("NOOO","I was interrupted");
                  }
              }
          }
        };
        Thread thread = new Thread(runnable);
        thread.start();
       // startClock(this);
    }
    public void setText(final int minutes, final int seconds){
        mHandler.post(new Runnable() {
            public void run() {
                String min=Integer.toString(minutes);
                String sec=Integer.toString(seconds);
                if (minutes<10){
                    min="0"+min;
                }
                if (seconds<10){
                    sec="0"+sec;
                }
                timerDisplay.setText(min + ":" + sec);
                totalDistanceView.setText(((Integer)totalDistance.intValue()).toString() + " m");
            }
        });
    }

    public void stopTheRun(View view){
        stillRunning = false;
        try {
            mLocationManager.removeUpdates(mLocationListener);
        }
        catch(SecurityException ex){

        }
        NessieWrapper wrapper=NessieWrapper.getInstance();
        wrapper.transferToChecking(moneyForNow);
        Intent intent = new Intent(this, ScoreScreen.class);
        intent.putExtra("Money", moneyForNow);
        startActivity(intent);
    }


    private  LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {

                Log.d("LocationListener", "NUJN");
                double    longitude = location.getLongitude();
                double    latitude = location.getLatitude();
            if (lastLocation!=null){
                double timedelta = System.currentTimeMillis()-startTimeTimer;
                if (location.distanceTo(lastLocation) >= 4 || timedelta<5000 ) {
                    totalDistance += location.distanceTo(lastLocation);
                    CaloriesBurned mCalories = new CaloriesBurned(kilos, (totalDistance / (timedelta * 1000)), (timedelta / 1000));
                    double calories = mCalories.calcCalories();
                    Double calories2 = (double) ((int) (calories * 100)) / 100;
                    TextView mView = (TextView) findViewById(R.id.caloriesburned);
                    mView.setText(calories2.toString() + " calories");
                    moneyForNow = calories2 * calorieFactor;
                    final TextView MoneyView = (TextView) findViewById(R.id.MoneyEarned);
                    MoneyView.setText("$ " + String.format("%.2f", moneyForNow));
                    MoneyView.setTextColor(Color.parseColor("#006400"));
                    colorChangeHandler = new Handler();

                    Runnable r = new Runnable(){
                        public void run(){
                            MoneyView.setTextColor(Color.BLACK);
                        }
                    };

                    colorChangeHandler.postDelayed(r, 2000);

                }

            }
            lastLocation=location;

            Toast.makeText(
                    getApplicationContext(), latitude +" "+ longitude,
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_running, menu);
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
    public void onLocationChanged(Location location) {
        Log.d(TAG,"Location is null!!");
        Toast.makeText(getApplicationContext(),"Location changed" ,
                Toast.LENGTH_LONG).show();
        if (location != null) {
            Log.d(TAG, location.getLatitude() + " and " + location.getLongitude());
            Toast.makeText(getApplicationContext(),location.getLatitude() + " and " + location.getLongitude() ,
                    Toast.LENGTH_LONG).show();
        }
        if (shouldStop){
            try {
                mLocationManager.removeUpdates(this);
            }
            catch(SecurityException ex){
                Log.d(TAG,"Could not properly stop location manager");
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
