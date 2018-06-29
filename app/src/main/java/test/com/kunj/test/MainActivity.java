package test.com.kunj.test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button start, connect, stop, pause;
    private final String DEVICE_ADDRESS="98:D3:31:80:9E:BE";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    boolean record = true;
    private OutputStream outputStream;
    private InputStream inputStream;
    TextView textView;
    long startTime;
    boolean deviceConnected=false;
    byte buffer[];
    boolean stopThread;
    FileOutputStream stream2, stream;
    FileInputStream inputStream2;
    File file2, file;
    ArrayList<Entry> entries, entriesUP, entriesLO;
    int count;
    ArrayList<String> labels;
    ArrayList<Double> values;
    LineChart lineChart;
    OLSMultipleLinearRegression regression;
    int eqncount;
    double [][] x;
    long timeWhenStopped = 0;
    double [] y;
    double[] beta;
    double sigma, predicted, upper, lower;
    TextView bt, degree;
    Chronometer timeElapsed;
    View chrono, angle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        entries = new ArrayList<>();
        entriesUP = new ArrayList<>();
        entriesLO = new ArrayList<>();
        labels = new ArrayList<>();

        values = new ArrayList<>();
        x = new double[40][20];
        y = new double[40];
        beta = new double[20];

        regression = new OLSMultipleLinearRegression();

        startTime=System.currentTimeMillis();
        final String date = getDate(startTime, "dd_MM_yyyy_hh_mm_ss");

        eqncount = 0;
        count = 1;
        predicted = 0;

        start = (Button) findViewById(R.id.button3);
        pause = (Button) findViewById(R.id.button);
        stop = (Button) findViewById(R.id.button2);
        start.setVisibility(View.INVISIBLE);
        stop.setVisibility(View.INVISIBLE);
        pause.setVisibility(View.INVISIBLE);
        connect = (Button)findViewById(R.id.button4);
        textView = (TextView)findViewById(R.id.textView2);
        bt = (TextView)findViewById(R.id.textView);
        degree = (TextView)findViewById(R.id.textView3);
        textView.setVisibility(View.INVISIBLE);
        chrono = findViewById(R.id.view2);
        chrono.setVisibility(View.INVISIBLE);
        angle = findViewById(R.id.view3);
        angle.setVisibility(View.INVISIBLE);
        degree.setVisibility(View.INVISIBLE);

        timeElapsed = (Chronometer) findViewById(R.id.chronometer);
        timeElapsed.setVisibility(View.INVISIBLE);

        lineChart = (LineChart) findViewById(R.id.chart);
        lineChart.setVisibility(View.INVISIBLE);

        Context context2 = this.getApplicationContext();
        final File path2 = context2.getExternalFilesDir(null);
        String fileName2 = "Cyclos_Sensor_Val.txt";
        file2 = new File(path2, fileName2);

        Context context = this.getApplicationContext();
        File path = context.getExternalFilesDir(null);
        String fileName = "Cyclos_"+date+".txt";
        final File file = new File(path, fileName);

        try {
            stream2 = new FileOutputStream(file2);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            inputStream2 = new FileInputStream(file2);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            stream = new FileOutputStream(file);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BTinit())
                {
                    if(BTconnect())
                    {
                        deviceConnected=true;
                        start.setVisibility(View.VISIBLE);
                        Toast.makeText(getApplicationContext(),"Connected to device",Toast.LENGTH_LONG).show();
                        connect.setVisibility(View.INVISIBLE);
                        bt.setText("The device should remain switched on throughout the ride and must be placed at L1 as instructed\n\n\nAre you ready?");

                    }
                }
            }

        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                start.setVisibility(View.INVISIBLE);
                textView.setVisibility(View.VISIBLE);
                lineChart.setVisibility(View.VISIBLE);
                stop.setVisibility(View.VISIBLE);
                pause.setVisibility(View.VISIBLE);
                bt.setVisibility(View.INVISIBLE);
                timeElapsed.setVisibility(View.VISIBLE);
                chrono.setVisibility(View.VISIBLE);
                angle.setVisibility(View.VISIBLE);
                degree.setVisibility(View.VISIBLE);
                timeElapsed.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener(){
                    @Override
                    public void onChronometerTick(Chronometer cArg) {
                        long time = SystemClock.elapsedRealtime() - cArg.getBase();
                        int h   = (int)(time /3600000);
                        int m = (int)(time - h*3600000)/60000;
                        int s= (int)(time - h*3600000- m*60000)/1000 ;
                        String hh = h < 10 ? "0"+h: h+"";
                        String mm = m < 10 ? "0"+m: m+"";
                        String ss = s < 10 ? "0"+s: s+"";
                        cArg.setText(hh+":"+mm+":"+ss);
                    }
                });


                timeElapsed.setBase(SystemClock.elapsedRealtime());
                timeElapsed.start();
                beginListenForData();

            }

        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pause.getText().equals("Pause") || pause.getText().equals("PAUSE")) {
                    record = false;
                    pause.setText("Resume");
                    timeWhenStopped = timeElapsed.getBase() - SystemClock.elapsedRealtime();
                    timeElapsed.stop();
                } else if(pause.getText().equals("Resume")){
                    pause.setText("Pause");
                    record=true;
                    timeElapsed.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
                    timeElapsed.start();
                }

            }

        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopThread = true;
                lineChart.setVisibleXRange(0,lineChart.getXValCount());
                lineChart.zoomOut();
                try {
                    outputStream.close();
                    inputStream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                deviceConnected=false;

            }

        });
    }

    public boolean BTinit()
    {
        boolean found=false;
        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",Toast.LENGTH_LONG).show();
            finish();
        }
        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Please Pair the Device first",Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(),"Tap on Connect again",Toast.LENGTH_SHORT).show();

        }
        else
        {
            for (BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(DEVICE_ADDRESS))
                {
                    device=iterator;
                    found=true;
                    break;

                }
            }
        }
        return found;
    }

    public boolean BTconnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        return connected;
    }

    void beginListenForData()
    {

        String string = "g";
        string.concat("\n");
        try {
            outputStream.write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string=new String(rawBytes,"UTF-8");
                            handler.post(new Runnable() {
                                public void run()
                                {
                                    try {
                                        stream2.write((string).getBytes());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    BufferedReader br = null;
                                    try {
                                        br = new BufferedReader(new InputStreamReader(new FileInputStream(file2)));
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    String line;
                                    String last = null;
                                    try {
                                        while ((line = br.readLine()) != null) {
                                            last = line;
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    String str = last;
                                    String[] numList = str.split(",");

                                    if(numList[0].length()>2){
                                        textView.setText(numList[0]);
                                        values.add(Double.parseDouble(numList[0]));

                                        if(values.size()>60){
                                            for (int i=0; i < 39; i++){
                                                y[i] = y[i+1];
                                                for (int r =0; r<20; r++){
                                                    x[i][r] =  x[i+1][r];
                                                }
                                            }

                                            for (int i=0; i < 20; i++){
                                                x[39][i] = values.get(values.size()-21+i);
                                                y[39] = values.get(values.size()-1);
                                            }

                                            regression.newSampleData(y, x);
                                            beta = regression.estimateRegressionParameters();
                                            sigma = regression.estimateRegressionStandardError();

                                            for (int i = 0; i<20; i++) {
                                                predicted = predicted + (values.get(values.size()-2-i)*beta[20-i]);
                                            }

                                            predicted += beta[0];

                                            upper = predicted + Math.abs(sigma);
                                            lower = predicted - Math.abs(sigma);

                                            entriesLO.add(new Entry((float) lower,count));
                                            entriesUP.add(new Entry((float) upper,count));

                                            try {
                                                stream.write((numList[0] + "," + String.valueOf(predicted) + String.valueOf(lower) + "," + String.valueOf(upper) + "\n").getBytes());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                            predicted = 0;

                                        } else if (values.size()>20){

                                            for (int i=0; i < 20; i++){
                                                x[eqncount][i] = values.get(values.size()-21+i);
                                                y[eqncount] = values.get(values.size()-1);
                                            }

                                            try {
                                                stream.write((numList[0] + "\n").getBytes());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                            if(values.size()==60){
                                                regression.newSampleData(y, x);
                                                beta = regression.estimateRegressionParameters();
                                                sigma = regression.estimateRegressionStandardError();

                                                for (int i = 0; i<20; i++){
                                                    predicted = predicted + (values.get(values.size()-2-i)*beta[20-i]);
                                                }
                                                predicted += beta[0];
                                                upper = predicted + Math.abs(sigma);
                                                lower = predicted - Math.abs(sigma);

                                                try {
                                                    stream.write((numList[0] + "," + String.valueOf(predicted) + String.valueOf(lower) + "," + String.valueOf(upper) + "\n").getBytes());
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }

                                                entriesLO.add(new Entry((float) lower, count));
                                                entriesUP.add(new Entry((float) upper, count));

                                                predicted = 0;

                                            } else {
                                                eqncount++;
                                            }

                                        }

                                        entries.add(new Entry(Float.parseFloat(numList[0]), count));

                                        ArrayList<LineDataSet> lines = new ArrayList<>();

                                        LineDataSet dataset = new LineDataSet(entries, "Pelvic Tilt");
                                        dataset.setDrawCircleHole(false);
                                        dataset.setDrawValues(false);
                                        dataset.setCircleSize(2);
                                        dataset.setColor(Color.BLUE);
                                        dataset.setCircleColor(Color.BLUE);

                                        LineDataSet datasetLO = new LineDataSet(entriesLO, "Lower Bound");
                                        datasetLO.setDrawCircleHole(false);
                                        datasetLO.setDrawValues(false);
                                        datasetLO.setCircleSize(2);
                                        datasetLO.setColor(Color.RED);
                                        datasetLO.setCircleColor(Color.RED);

                                        LineDataSet datasetUP = new LineDataSet(entriesUP, "Upper Bound");
                                        datasetUP.setDrawCircleHole(false);
                                        datasetUP.setDrawValues(false);
                                        datasetUP.setCircleSize(2);
                                        datasetUP.setColor(Color.GRAY);
                                        datasetUP.setCircleColor(Color.GRAY);

                                        labels.add(String.valueOf(count));

                                        lines.add(dataset);
                                        lines.add(datasetLO);
                                        lines.add(datasetUP);

                                        lineChart.setData(new LineData(labels, lines));
                                        lineChart.notifyDataSetChanged();
                                        lineChart.setVisibleXRange(0,16);
                                        if (lineChart.getXValCount()>20){
                                            lineChart.moveViewToX(lineChart.getXValCount()-20);
                                        } else {
                                            lineChart.zoomOut();
                                        }
                                        lineChart.setDescription("");

                                        count++;
                                    }
                                }
                            });
                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }

    public static String getDate(long milliSeconds, String dateFormat)
    {

        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);


        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}
