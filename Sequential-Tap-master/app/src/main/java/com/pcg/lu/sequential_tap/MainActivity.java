package com.pcg.lu.sequential_tap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Random;


public class MainActivity extends AppCompatActivity {
    public final static int VIEW_SEQUENTIAL = 0;
    public final static int VIEW_GESTURE = 1;

    public final static int GESTURE_SETUP = 2;
    public final static int GESTURE_LEARN = 3;
    public final static int GESTURE_TEST = 4;

    public final static int SEQUENTIAL_SETUP = 5;
    public final static int SEQUENTIAL_LEARN = 6;
    public final static int SEQUENTIAL_TEST = 7;

    DrawView drawView;
    DrawView drawView2;
    DrawFlash drawFlash;
    DrawFlash drawFlash2;

//    TextView view;
//    Button changeState;
//    Button save;
//    EditText edit;
//    TextView gestureName;
//    Button startTest;

// seq页面组件
    Spinner changeModeSpinner_s;
    List<String> list_s = new ArrayList<String>();
    ArrayAdapter<String> adapter_s;
    TextView modeTip_s;

    TextView setupTip_s;
    Button setupSave_s;
    Button setupPlay_s;
    Button setupLast_s;
    Button setupNext_s;

    TextView learnFunction_s;
    TextView learnResult_s;
    Button learnPlay_s;
    Button learnLast_s;
    Button learnNext_s;
    Switch learnSwitch_s;
    TextView learnSwitchTip_s;

    Switch log_s;

    TextView testTip_s;
    TextView testResult_s;
    Button testStart_s;

    boolean showTipFlash_s = true;


// gesture页面组件
    boolean isGestureInitial = false;

    Spinner changeModeSpinner;
    List<String> list = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    TextView modeTip;

    TextView setupTip;
    Button setupSave;
    Button setupPlay;
    Button setupLast;
    Button setupNext;

    TextView learnFunction;
    TextView learnResult;
    Button learnPlay;
    Button learnLast;
    Button learnNext;

    TextView testTip;
    TextView testResult;
    Button testStart;


    int page = VIEW_SEQUENTIAL;
    int state = GESTURE_SETUP;
    int state_s = SEQUENTIAL_SETUP;


    boolean isTesting = false;
    final int testTime = 50;
    int testNum = 0;
    int rightCaseNum = 0;
    int wrongCaseNum = 0;
    String[] testCase;

    long startTime;
    long endTime;
    long usedTime;

    final int seqSize = 7;
    final int gestureSize = 7;
    int setupNumber = 0;
    int learnNumber = 0;

    boolean isTesting_s = false;
    final int testTime_s = 50;
    int testNum_s = 0;
    int rightCaseNum_s = 0;
    int wrongCaseNum_s = 0;
    String[] testCase_s;

    long startTime_s;
    long endTime_s;
    long usedTime_s;


    TouchEvent[] touchEvent = new TouchEvent[10];
    ArrayList<QTouch> qtouchs = new ArrayList();
    ArrayList<QTouch> qtouchs_copy = new ArrayList();

    ArrayList<Point> qmoves = new ArrayList();
    ArrayList<Candidate> candidates = new ArrayList();
    ArrayList<Candidate_s> candidates_s = new ArrayList();

    QSequential[] qSequentials = new QSequential[seqSize];
    QGesture[] qGestures = new QGesture[gestureSize];

    Timer idleTimer = null;

    SoundUtils soundUtils = new SoundUtils(this, SoundUtils.RING_SOUND);

    // v3 sensor
    final boolean ENABLE_MICROPHONE = false;
    // ui
    TextView uText0, uText1;
    // log
    long sensorStartTime;
    File fileDirectory;
    PrintWriter logger;
    String logBuffer;
    boolean isLogging;

    // microphone
    Microphone microphone;

    // inertial
    SensorManager sensorManager;
    InertialSensor inertialSensor;

    void onCreateLogger() {
        fileDirectory = this.getApplicationContext().getExternalFilesDir(null);
        logBuffer = "";
        isLogging = false;
    }

    void onCreateMicrophone() {
        microphone = new Microphone(this);
        if (ENABLE_MICROPHONE) microphone.start();
    }

    void onCreateInertial() {
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        inertialSensor = new InertialSensor(this);
    }

    void logToFile0(String tag, Object... param) {
        if (!isLogging) return;
        synchronized (logBuffer) {
            logBuffer += tag;
            switch (tag) {
                case "acc":
                case "gyr":
                case "gra":
                    float[] values0 = (float[]) param[0];
                    logBuffer += String.format(" %.5f %.5f %.5f", values0[0], values0[1], values0[2]);
                    break;
                case "meg":float[] values1 = (float[]) param[0];
                    logBuffer += String.format(" %.1f %.1f %.1f", values1[0], values1[1], values1[2]);
                    break;
                case "mic":
                    byte[] values2 = (byte[]) param[0];
                    for (byte value : values2) {
                        logBuffer += " " + value;
                        break;
                    }
                    break;
                case "time":
                    logBuffer += " " + System.currentTimeMillis();
                    break;
            }
            logBuffer += "\n";
            if (logBuffer.length() > 1024) {
                logger.write(logBuffer);
                logger.flush();
                logBuffer = "";
            }
        }
    }

    void logToFile1() {
        if (!isLogging) return;
        logBuffer += "time " + System.currentTimeMillis() + "\n";
        synchronized (inertialSensor.listLinearAccelerometer) {
            for (float[] values : inertialSensor.listLinearAccelerometer) logBuffer += String.format("acc %.5f %.5f %.5f\n", values[0], values[1], values[2]);
            inertialSensor.listLinearAccelerometer.clear();
        }
        synchronized (inertialSensor.listGyroscope) {
            for (float[] values : inertialSensor.listGyroscope) logBuffer += String.format("gyr %.5f %.5f %.5f\n", values[0], values[1], values[2]);
            inertialSensor.listGyroscope.clear();
        }
        synchronized (inertialSensor.listMegneticField) {
            for (float[] values : inertialSensor.listMegneticField) logBuffer += String.format("meg %.2f %.2f %.2f\n", values[0], values[1], values[2]);
            inertialSensor.listMegneticField.clear();
        }
        synchronized (inertialSensor.listGravity) {
            for (float[] values : inertialSensor.listGravity) logBuffer += String.format("gra %.5f %.5f %.5f\n", values[0], values[1], values[2]);
            inertialSensor.listGravity.clear();
        }
        synchronized (microphone.buffer) {
            logBuffer += "mic";
            for (byte value: microphone.buffer) logBuffer += " " + value;
            logBuffer += "\n";
        }
        if (logBuffer.length() > 1024) {
            logger.write(logBuffer);
            logger.flush();
            logBuffer = "";
        }
    }

    void logToFile2() {
        if (!isLogging) return;
        logBuffer += "time " + System.currentTimeMillis() + "\n";
        synchronized (inertialSensor.dataLinearAccelerometer) {
            float[] values = inertialSensor.dataLinearAccelerometer;
            logBuffer += String.format("acc %.5f %.5f %.5f\n", values[0], values[1], values[2]);
        }
        synchronized (inertialSensor.dataGyroscope) {
            float[] values = inertialSensor.dataGyroscope;
            logBuffer += String.format("gyr %.5f %.5f %.5f\n", values[0], values[1], values[2]);
        }
        /*synchronized (inertialSensor.dataMegneticField) {
            float[] values = inertialSensor.dataMegneticField;
            logBuffer += String.format("meg %.2f %.2f %.2f\n", values[0], values[1], values[2]);
        }*/
        synchronized (inertialSensor.dataGravity) {
            float[] values = inertialSensor.dataGravity;
            logBuffer += String.format("gra %.5f %.5f %.5f\n", values[0], values[1], values[2]);
        }
        //Log.d("log", logBuffer.length() + "");
        if (logBuffer.length() > 16384) {
            logger.write(logBuffer);
            logger.flush();
            logBuffer = "";
        }
    }

    void logToFile3(String s) {
        if (!isLogging) return;
        s = System.currentTimeMillis() + " " + s + "\n";
        logBuffer += s;
        Log.d("log", logBuffer.length() + "");
        if (logBuffer.length() > 16384) {
            logger.write(logBuffer);
            logger.flush();
            logBuffer = "";
        }
    }

    void showFrequency() {
        double runTime = (System.currentTimeMillis() - startTime) / 1000.0;
        uText0.setText(String.format("Inertial: %.3f Hz", inertialSensor.counter / runTime));
        uText1.setText(String.format("Microphone: %.3f Hz", microphone.counter / runTime));
    }

    void changeLogStatus(int v) {
        // v == 0 : change
        // v == 1 : off
        // v == 2 : on
        if (v == 0 && isLogging == true || v == 1) {
            if (isLogging) {
                if (logger != null) {
                    logger.write(logBuffer);
                    logger.flush();
                    logger.close();
                }
                logBuffer = "";
                isLogging = false;
                modeTip_s.setText("LOG/OFF");
            }
        } else {
            if (!isLogging) {
                try {
                    String fileName = "log_" + new SimpleDateFormat("yy-MM-dd_HH-mm-ss").format(new Date()) + ".txt";
                    logger = new PrintWriter(new FileOutputStream(fileDirectory + "/" + fileName));
                    logBuffer = "";
                    isLogging = true;
                    modeTip_s.setText("LOG/ON");
                } catch (Exception e) {
                    Log.d("file", e.toString());
                }
            }
        }
    }

    //
//    /**
//     * 开始录音
//     */
//    private void record(){
//        int mResult = -1;
//        AudioRecordFunc mRecord_1 = AudioRecordFunc.getInstance();
//        mResult = mRecord_1.startRecordAndFile();
//    }
//    /**
//     * 停止录音
//     */
//    private void stop(){
//        AudioRecordFunc mRecord_1 = AudioRecordFunc.getInstance();
//        mRecord_1.stopRecordAndFile();
//    }


//    AudioUtil audio = new AudioUtil();

    // 工具函数
    // 计算绝对值
    public boolean AbsoluteVal(double vala, double valb, double absolute){
        if((vala - valb > 0) && vala - valb < absolute ){
            return true;
        }
        else if((vala - valb < 0) && vala -valb > -absolute){
            return true;
        }
        else {
            return false;
        }
    }
    // 初始化tap序列 tap功能名称
    public void initialQSequential(){
        final long seqInterval = 300;
        final long synInterval = 100;

        for(int i = 0; i < seqSize; i++){
            qSequentials[i] = new QSequential();
        }

        //打开微信 23
        qSequentials[0].tapName = "打开微信";
        qSequentials[0].qTaps.add(new QTap(1,1,true));
        qSequentials[0].qTaps.add(new QTap(1,2,false));
        qSequentials[0].qTaps.add(new QTap(2,3,true));
        qSequentials[0].qTaps.add(new QTap(2,4,false));

        //打开信息 24
        qSequentials[1].tapName = "打开信息";
        qSequentials[1].qTaps.add(new QTap(1,1,true));
        qSequentials[1].qTaps.add(new QTap(1,2,false));
        qSequentials[1].qTaps.add(new QTap(3,3,true));
        qSequentials[1].qTaps.add(new QTap(3,4,false));

        //打开电话 234
        qSequentials[2].tapName = "打开电话";
        qSequentials[2].qTaps.add(new QTap(1,1,true));
        qSequentials[2].qTaps.add(new QTap(1,2,false));
        qSequentials[2].qTaps.add(new QTap(2,3,true));
        qSequentials[2].qTaps.add(new QTap(2,4,false));
        qSequentials[2].qTaps.add(new QTap(3,3,true));
        qSequentials[2].qTaps.add(new QTap(3,4,false));

        //打开微博 32
        qSequentials[3].tapName = "打开微博";
        qSequentials[3].qTaps.add(new QTap(2,1,true));
        qSequentials[3].qTaps.add(new QTap(2,2,false));
        qSequentials[3].qTaps.add(new QTap(1,3,true));
        qSequentials[3].qTaps.add(new QTap(1,4,false));

        //打开视频播放器 324
        qSequentials[4].tapName = "打开视频播放器";
        qSequentials[4].qTaps.add(new QTap(2,1,true));
        qSequentials[4].qTaps.add(new QTap(2,2,false));
        qSequentials[4].qTaps.add(new QTap(1,3,true));
        qSequentials[4].qTaps.add(new QTap(1,4,false));
        qSequentials[4].qTaps.add(new QTap(3,3,true));
        qSequentials[4].qTaps.add(new QTap(3,4,false));

        //打开音乐播放器 432
        qSequentials[5].tapName = "打开音乐播放器";
        qSequentials[5].qTaps.add(new QTap(3,1,true));
        qSequentials[5].qTaps.add(new QTap(3,2,false));
        qSequentials[5].qTaps.add(new QTap(2,3,true));
        qSequentials[5].qTaps.add(new QTap(2,4,false));
        qSequentials[5].qTaps.add(new QTap(1,5,true));
        qSequentials[5].qTaps.add(new QTap(1,6,false));

        //打开支付宝 42
        qSequentials[6].tapName = "打开支付宝";
        qSequentials[6].qTaps.add(new QTap(3,1,true));
        qSequentials[6].qTaps.add(new QTap(3,2,false));
        qSequentials[6].qTaps.add(new QTap(1,3,true));
        qSequentials[6].qTaps.add(new QTap(1,4,false));

        for(int i = 0; i < seqSize; i++){
            for(int j = 0; j < qSequentials[i].qTaps.size() - 1; j++){
                if(qSequentials[i].qTaps.get(j).order == qSequentials[i].qTaps.get(j+1).order){
                    qSequentials[i].runTime = qSequentials[i].runTime + synInterval;
                }
                else {
                    qSequentials[i].runTime = qSequentials[i].runTime + seqInterval;
                }
            }
            qSequentials[i].runTime = qSequentials[i].runTime + 1000;
        }
    }
    // 初始化gesture序列 gesture功能名称
    public void initialQGesture(){
        for(int i = 0; i < seqSize; i++){
            qGestures[i] = new QGesture();
        }
        // 打开微信 W
        qGestures[0].gestureName = "打开微信";
        qGestures[0].qPoints.add(new Point(300,400));
        qGestures[0].qPoints.add(new Point(350,550));
        qGestures[0].qPoints.add(new Point(400,700));
        qGestures[0].qPoints.add(new Point(450,550));
        qGestures[0].qPoints.add(new Point(500,400));
        qGestures[0].qPoints.add(new Point(550,550));
        qGestures[0].qPoints.add(new Point(600,700));
        qGestures[0].qPoints.add(new Point(650,550));
        qGestures[0].qPoints.add(new Point(700,400));

        // 打开信息 X
        qGestures[1].gestureName = "打开信息";
        qGestures[1].qPoints.add(new Point(400,400));
        qGestures[1].qPoints.add(new Point(500,550));
        qGestures[1].qPoints.add(new Point(600,700));
        qGestures[1].qPoints.add(new Point(600,550));
        qGestures[1].qPoints.add(new Point(600,400));
        qGestures[1].qPoints.add(new Point(500,550));
        qGestures[1].qPoints.add(new Point(400,700));

        // 打开电话 D
        qGestures[2].gestureName = "打开电话";
        qGestures[2].qPoints.add(new Point(400,700));
        qGestures[2].qPoints.add(new Point(400,550));
        qGestures[2].qPoints.add(new Point(400,400));

        qGestures[2].qPoints.add(new Point(460,440));
        qGestures[2].qPoints.add(new Point(520,480));
        qGestures[2].qPoints.add(new Point(550,550));
        qGestures[2].qPoints.add(new Point(520,620));
        qGestures[2].qPoints.add(new Point(460,660));
        qGestures[2].qPoints.add(new Point(400,700));

        // 打开微博 椭圆
        qGestures[3].gestureName = "打开微博";
        qGestures[3].qPoints.add(new Point(550,400));
        qGestures[3].qPoints.add(new Point(475,420));
        qGestures[3].qPoints.add(new Point(400,475));
        qGestures[3].qPoints.add(new Point(475,530));
        qGestures[3].qPoints.add(new Point(550,550));
        qGestures[3].qPoints.add(new Point(625,530));
        qGestures[3].qPoints.add(new Point(700,475));
        qGestures[3].qPoints.add(new Point(625,420));
        qGestures[3].qPoints.add(new Point(550,400));

        // 打开音乐播放器 音符
        qGestures[4].gestureName = "打开音乐播放器";
        qGestures[4].qPoints.add(new Point(650,400));
        qGestures[4].qPoints.add(new Point(600,400));
        qGestures[4].qPoints.add(new Point(550,400));
        qGestures[4].qPoints.add(new Point(550,450));
        qGestures[4].qPoints.add(new Point(550,500));
        qGestures[4].qPoints.add(new Point(550,550));
        qGestures[4].qPoints.add(new Point(550,600));
        qGestures[4].qPoints.add(new Point(550,650));
        qGestures[4].qPoints.add(new Point(550,700));

        qGestures[4].qPoints.add(new Point(525,690));
        qGestures[4].qPoints.add(new Point(500,650));
        qGestures[4].qPoints.add(new Point(525,610));
        qGestures[4].qPoints.add(new Point(550,600));


        // 打开视频播放器 三角
        qGestures[5].gestureName = "打开视频播放器";
        qGestures[5].qPoints.add(new Point(450,400));
        qGestures[5].qPoints.add(new Point(450,550));
        qGestures[5].qPoints.add(new Point(450,700));
        qGestures[5].qPoints.add(new Point(550,625));
        qGestures[5].qPoints.add(new Point(650,550));
        qGestures[5].qPoints.add(new Point(550,475));
        qGestures[5].qPoints.add(new Point(450,400));

        // 打开支付宝 S
        qGestures[6].gestureName = "打开支付宝";
        qGestures[6].qPoints.add(new Point(650,475));
        qGestures[6].qPoints.add(new Point(600,420));

        qGestures[6].qPoints.add(new Point(550,400));
        qGestures[6].qPoints.add(new Point(500,420));

        qGestures[6].qPoints.add(new Point(450,475));
        qGestures[6].qPoints.add(new Point(500,530));

        qGestures[6].qPoints.add(new Point(550,550));

        qGestures[6].qPoints.add(new Point(600,570));
        qGestures[6].qPoints.add(new Point(650,625));

        qGestures[6].qPoints.add(new Point(600,680));
        qGestures[6].qPoints.add(new Point(550,700));

        qGestures[6].qPoints.add(new Point(500,680));
        qGestures[6].qPoints.add(new Point(450,625));


        for(int i = 0; i < gestureSize; i++){
            qGestures[i].runTime = qGestures[i].qPoints.size() * 200 + 1000;
        }
    }
    // 初始化gesture页面组件
    public void initialGesturePage(){
        changeModeSpinner = (Spinner)findViewById(R.id.change_mode);
        modeTip = (TextView)findViewById(R.id.mode_tip);

        setupTip = (TextView)findViewById(R.id.setup_tip);
        setupSave = (Button)findViewById(R.id.setup_save);
        setupLast = (Button)findViewById(R.id.setup_last);
        setupPlay = (Button)findViewById(R.id.setup_play);
        setupNext = (Button)findViewById(R.id.setup_next);

        learnFunction = (TextView)findViewById(R.id.learn_function);
        learnResult = (TextView)findViewById(R.id.learn_result);
        learnLast = (Button)findViewById(R.id.learn_last);
        learnPlay = (Button)findViewById(R.id.learn_play);
        learnNext = (Button)findViewById(R.id.learn_next);

        testTip = (TextView)findViewById(R.id.test_tip);
        testResult = (TextView)findViewById(R.id.test_result);
        testStart = (Button)findViewById(R.id.test_start);

        list.add("初始设置");
        list.add("学习模式");
        list.add("测试模式");
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        changeModeSpinner.setAdapter(adapter);

        changeModeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                /* 将所选mySpinner 的值带入myTextView 中 */
                switch (arg2){
                    case 0:
                        state = GESTURE_SETUP;
                        break;
                    case 1:
                        if(candidates.size() != gestureSize){
                            int minus = gestureSize - candidates.size();
                            int vacant[] = new int[minus];
                            int flag = 0;
                            for(int i = 0; i < gestureSize; i++){
                                if(qGestures[i].setup == false){
                                    vacant[flag] = i;
                                    flag ++;
                                }
                            }
                            AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
                            nameNull.setTitle("失败" ) ;
                            String tip = "仍有"+ minus + "个手势未设置，分别为";
                            for(int i = 0; i < minus; i++){
                                tip = tip + vacant[i] + ",";
                            }
                            nameNull.setMessage(tip) ;
                            nameNull.setPositiveButton("ok" ,  null );
                            nameNull.show();
                            state = GESTURE_SETUP;
                        }
                        else{
                            state = GESTURE_LEARN;
                        }
                        break;
                    case 2:
                        if(candidates.size() != gestureSize){
                            int minus = gestureSize - candidates.size();
                            int vacant[] = new int[minus];
                            int flag = 0;
                            for(int i = 0; i < gestureSize; i++){
                                if(qGestures[i].setup == false){
                                    vacant[flag] = i;
                                    flag ++;
                                }
                            }
                            AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
                            nameNull.setTitle("失败" ) ;
                            String tip = "仍有"+ minus + "个手势未设置，分别为";
                            for(int i = 0; i < minus; i++){
                                tip = tip + vacant[i] + ",";
                            }
                            nameNull.setMessage(tip) ;
                            nameNull.setPositiveButton("ok" ,  null );
                            nameNull.show();
                            state = GESTURE_SETUP;
                        }
                        else{
                            state = GESTURE_TEST;
                        }
                        break;
                }
                StateOnChange();
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
    }

    // 工具类
    private static Handler handler=new Handler();

    public class SetEnableRunnable implements Runnable {
        int mode;
        long enableTime;

        public SetEnableRunnable(int mode, long enableTime){
            this.enableTime= enableTime;
            this.mode = mode;
        }

        public void run() {
            if(mode == SEQUENTIAL_SETUP){
                handler.post(new Runnable(){
                    public void run(){
                        setupSave_s.setEnabled(false);
                        setupNext_s.setEnabled(false);
                        setupPlay_s.setEnabled(false);
                        setupLast_s.setEnabled(false);
                        changeModeSpinner_s.setEnabled(false);
                    }
                });
                try {
                    Thread.sleep(enableTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable(){
                    public void run(){
                        setupSave_s.setEnabled(true);
                        setupNext_s.setEnabled(true);
                        setupPlay_s.setEnabled(true);
                        setupLast_s.setEnabled(true);
                        changeModeSpinner_s.setEnabled(true);
                    }
                });
            }
            else if(mode == SEQUENTIAL_LEARN){
                handler.post(new Runnable(){
                    public void run(){
                        learnNext_s.setEnabled(false);
                        learnPlay_s.setEnabled(false);
                        learnLast_s.setEnabled(false);
                        changeModeSpinner_s.setEnabled(false);
                    }
                });
                try {
                    Thread.sleep(enableTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable(){
                    public void run(){
                        learnNext_s.setEnabled(true);
                        learnPlay_s.setEnabled(true);
                        learnLast_s.setEnabled(true);
                        changeModeSpinner_s.setEnabled(true);
                    }
                });
            }
            else if(mode == GESTURE_SETUP){
                handler.post(new Runnable(){
                    public void run(){
                        setupSave.setEnabled(false);
                        setupNext.setEnabled(false);
                        setupPlay.setEnabled(false);
                        setupLast.setEnabled(false);
                        changeModeSpinner.setEnabled(false);
                    }
                });
                try {
                    Thread.sleep(enableTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable(){
                    public void run(){
                        setupSave.setEnabled(true);
                        setupNext.setEnabled(true);
                        setupPlay.setEnabled(true);
                        setupLast.setEnabled(true);
                        changeModeSpinner.setEnabled(true);
                    }
                });
            }
            else if(mode == GESTURE_LEARN){
                handler.post(new Runnable(){
                    public void run(){
                        learnNext.setEnabled(false);
                        learnPlay.setEnabled(false);
                        learnLast.setEnabled(false);
                        changeModeSpinner.setEnabled(false);
                    }
                });
                try {
                    Thread.sleep(enableTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable(){
                    public void run(){
                        learnNext.setEnabled(true);
                        learnPlay.setEnabled(true);
                        learnLast.setEnabled(true);
                        changeModeSpinner.setEnabled(true);
                    }
                });
            }

        }
    };


    //初始函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onChangeView(VIEW_SEQUENTIAL);
        initialFindViews();

        onCreateLogger();
        onCreateMicrophone();
        onCreateInertial();

        sensorStartTime = System.currentTimeMillis();
    }

    // 初始页面
    public void initialFindViews(){
        changeModeSpinner_s = (Spinner)findViewById(R.id.change_mode_s);
        modeTip_s = (TextView)findViewById(R.id.mode_tip_s);

        setupTip_s = (TextView)findViewById(R.id.setup_tip_s);
        setupSave_s = (Button)findViewById(R.id.setup_save_s);
        setupLast_s = (Button)findViewById(R.id.setup_last_s);
        setupPlay_s = (Button)findViewById(R.id.setup_play_s);
        setupNext_s = (Button)findViewById(R.id.setup_next_s);

        learnFunction_s = (TextView)findViewById(R.id.learn_function_s);
        learnResult_s = (TextView)findViewById(R.id.learn_result_s);
        learnLast_s = (Button)findViewById(R.id.learn_last_s);
        learnPlay_s = (Button)findViewById(R.id.learn_play_s);
        learnNext_s = (Button)findViewById(R.id.learn_next_s);
        learnSwitch_s = (Switch)findViewById(R.id.learn_switch_s);
        learnSwitchTip_s = (TextView)findViewById(R.id.learn_switch_tip_s);

        uText0 = (TextView)findViewById(R.id.uText0);
        uText1 = (TextView)findViewById(R.id.uText1);
        log_s = (Switch)findViewById(R.id.log_s);

        log_s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    modeTip_s.setText("开启");
                    changeLogStatus(2);
                    microphone.start();
//                    audio.startRecord();
//                    record();
                }else {
                    modeTip_s.setText("关闭");
                    changeLogStatus(1);
                    microphone.isRun = false;
//                    audio.stopRecord();
//                    stop();
                }
            }
        });


        testTip_s = (TextView)findViewById(R.id.test_tip_s);
        testResult_s = (TextView)findViewById(R.id.test_result_s);
        testStart_s = (Button)findViewById(R.id.test_start_s);


        learnFunction_s.setVisibility(View.GONE);
        learnResult_s.setVisibility(View.GONE);
        learnLast_s.setVisibility(View.GONE);
        learnPlay_s.setVisibility(View.GONE);
        learnNext_s.setVisibility(View.GONE);
        learnSwitch_s.setVisibility(View.GONE);
        learnSwitchTip_s.setVisibility(View.GONE);
        testTip_s.setVisibility(View.GONE);
        testResult_s.setVisibility(View.GONE);
        testStart_s.setVisibility(View.GONE);

        initialQSequential();
        initialQGesture();

        learnSwitch_s.setChecked(true);
        learnSwitch_s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //控制开关字体颜色
                if (b) {
                    System.out.println("heeeere this is b");
                    showTipFlash_s = true;
                }else {
                    System.out.println("heeeere this is !!!!!b");
                    showTipFlash_s = false;
                }
            }
        });


        list_s.add("初始设置");
        list_s.add("学习模式");
        list_s.add("测试模式");
        adapter_s = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list_s);
        adapter_s.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        changeModeSpinner_s.setAdapter(adapter_s);

        changeModeSpinner_s.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                /* 将所选mySpinner 的值带入myTextView 中 */
                switch (arg2){
                    case 0:
                        state_s = SEQUENTIAL_SETUP;
                        break;
                    case 1:
//                        if(candidates_s.size() != seqSize){
//                            int minus = seqSize - candidates_s.size();
//                            int vacant[] = new int[minus];
//                            int flag = 0;
//                            for(int i = 0; i < seqSize; i++){
//                                if(qSequentials[i].setup == false){
//                                    vacant[flag] = i;
//                                    flag ++;
//                                }
//                            }
//                            AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
//                            nameNull.setTitle("失败" ) ;
//                            String tip = "仍有"+ minus + "个手势未设置，分别为";
//                            for(int i = 0; i < minus; i++){
//                                tip = tip + vacant[i] + ",";
//                            }
//                            nameNull.setMessage(tip) ;
//                            nameNull.setPositiveButton("ok" ,  null );
//                            nameNull.show();
//                            state_s = SEQUENTIAL_SETUP;
//                        }
//                        else{
//                            state_s = SEQUENTIAL_LEARN;
//                        }
                        state_s = SEQUENTIAL_LEARN;
                        break;
                    case 2:
                        if(candidates_s.size() != seqSize){
                            int minus = seqSize - candidates_s.size();
                            int vacant[] = new int[minus];
                            int flag = 0;
                            for(int i = 0; i < seqSize; i++){
                                if(qSequentials[i].setup == false){
                                    vacant[flag] = i;
                                    flag ++;
                                }
                            }
                            AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
                            nameNull.setTitle("失败" ) ;
                            String tip = "仍有"+ minus + "个手势未设置，分别为";
                            for(int i = 0; i < minus; i++){
                                tip = tip + vacant[i] + ",";
                            }
                            nameNull.setMessage(tip) ;
                            nameNull.setPositiveButton("ok" ,  null );
                            nameNull.show();
                            state_s = SEQUENTIAL_SETUP;
                        }
                        else{
                            state_s = SEQUENTIAL_TEST;
                        }
                        break;
                }
                StateOnChange_s();
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        soundUtils.putSound(0, R.raw.audio0);
        soundUtils.putSound(1, R.raw.audio1);
        soundUtils.putSound(2, R.raw.audio2);
        soundUtils.putSound(3, R.raw.audio3);
        soundUtils.putSound(4, R.raw.audio4);
        soundUtils.putSound(5, R.raw.audio5);
        soundUtils.putSound(6, R.raw.audio6);
        soundUtils.putSound(7, R.raw.audio7);
        soundUtils.putSound(8, R.raw.audio8);
        soundUtils.putSound(9, R.raw.audio9);
    }

    // 两种模式切换
    void onChangeView(int target) {
        page = target;
        switch (page) {
            case VIEW_SEQUENTIAL:
                setContentView(R.layout.sequential_tap);
                ConstraintLayout layout = findViewById(R.id.main_layout);
                drawView = new DrawView(this);
                layout.addView(drawView);
                drawFlash = new DrawFlash(this);
                layout.addView(drawFlash);
                break;
            case VIEW_GESTURE:
                setContentView(R.layout.gesture);
                ConstraintLayout layout_gesture = findViewById(R.id.gesture_layout);
                drawView2 = new DrawView(this);
                layout_gesture.addView(drawView2);
                drawFlash2 = new DrawFlash(this);
                layout_gesture.addView(drawFlash2);
                break;
        }
    }

    public void buttonPageOnChange(View v) {
        switch (page) {
            case VIEW_SEQUENTIAL:
                onChangeView(VIEW_GESTURE);
                if(!isGestureInitial){
                    isGestureInitial = true;
                    initialGesturePage();
                }

                changeModeSpinner = (Spinner)findViewById(R.id.change_mode);
                modeTip = (TextView)findViewById(R.id.mode_tip);

                setupTip = (TextView)findViewById(R.id.setup_tip);
                setupSave = (Button)findViewById(R.id.setup_save);
                setupLast = (Button)findViewById(R.id.setup_last);
                setupPlay = (Button)findViewById(R.id.setup_play);
                setupNext = (Button)findViewById(R.id.setup_next);

                learnFunction = (TextView)findViewById(R.id.learn_function);
                learnResult = (TextView)findViewById(R.id.learn_result);
                learnLast = (Button)findViewById(R.id.learn_last);
                learnPlay = (Button)findViewById(R.id.learn_play);
                learnNext = (Button)findViewById(R.id.learn_next);

                testTip = (TextView)findViewById(R.id.test_tip);
                testResult = (TextView)findViewById(R.id.test_result);
                testStart = (Button)findViewById(R.id.test_start);

                adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                changeModeSpinner.setAdapter(adapter);

                changeModeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                                               int arg2, long arg3) {
                        /* 将所选mySpinner 的值带入myTextView 中 */
                        switch (arg2){
                            case 0:
                                state = GESTURE_SETUP;
                                break;
                            case 1:
                                if(candidates.size() != gestureSize){
                                    int minus = gestureSize - candidates.size();
                                    int vacant[] = new int[minus];
                                    int flag = 0;
                                    for(int i = 0; i < gestureSize; i++){
                                        if(qGestures[i].setup == false){
                                            vacant[flag] = i;
                                            flag ++;
                                        }
                                    }
                                    AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
                                    nameNull.setTitle("失败" ) ;
                                    String tip = "仍有"+ minus + "个手势未设置，分别为";
                                    for(int i = 0; i < minus; i++){
                                        tip = tip + vacant[i] + ",";
                                    }
                                    nameNull.setMessage(tip) ;
                                    nameNull.setPositiveButton("ok" ,  null );
                                    nameNull.show();
                                    state = GESTURE_SETUP;
                                }
                                else{
                                    state = GESTURE_LEARN;
                                }
                                break;
                            case 2:
                                if(candidates.size() != gestureSize){
                                    int minus = gestureSize - candidates.size();
                                    int vacant[] = new int[minus];
                                    int flag = 0;
                                    for(int i = 0; i < gestureSize; i++){
                                        if(qGestures[i].setup == false){
                                            vacant[flag] = i;
                                            flag ++;
                                        }
                                    }
                                    AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
                                    nameNull.setTitle("失败" ) ;
                                    String tip = "仍有"+ minus + "个手势未设置，分别为";
                                    for(int i = 0; i < minus; i++){
                                        tip = tip + vacant[i] + ",";
                                    }
                                    nameNull.setMessage(tip) ;
                                    nameNull.setPositiveButton("ok" ,  null );
                                    nameNull.show();
                                    state = GESTURE_SETUP;
                                }
                                else{
                                    state = GESTURE_TEST;
                                }
                                break;
                        }
                        StateOnChange();
                    }

                    public void onNothingSelected(AdapterView<?> arg0) {

                    }
                });

                changeModeSpinner.setVisibility(View.VISIBLE);
                modeTip.setVisibility(View.VISIBLE);
                setupTip.setVisibility(View.VISIBLE);
                setupSave.setVisibility(View.VISIBLE);
                setupLast.setVisibility(View.VISIBLE);
                setupPlay.setVisibility(View.VISIBLE);
                setupNext.setVisibility(View.VISIBLE);


                learnFunction.setVisibility(View.GONE);
                learnResult.setVisibility(View.GONE);
                learnLast.setVisibility(View.GONE);
                learnPlay.setVisibility(View.GONE);
                learnNext.setVisibility(View.GONE);
                testTip.setVisibility(View.GONE);
                testResult.setVisibility(View.GONE);
                testStart.setVisibility(View.GONE);

                state = GESTURE_SETUP;

                setupNumber = 0;
                learnNumber = 0;
                break;
            case VIEW_GESTURE:
                onChangeView(VIEW_SEQUENTIAL);
                // 切换至seq

                changeModeSpinner_s = (Spinner)findViewById(R.id.change_mode_s);
                modeTip_s = (TextView)findViewById(R.id.mode_tip_s);

                setupTip_s = (TextView)findViewById(R.id.setup_tip_s);
                setupSave_s = (Button)findViewById(R.id.setup_save_s);
                setupLast_s = (Button)findViewById(R.id.setup_last_s);
                setupPlay_s = (Button)findViewById(R.id.setup_play_s);
                setupNext_s = (Button)findViewById(R.id.setup_next_s);

                learnFunction_s = (TextView)findViewById(R.id.learn_function_s);
                learnResult_s = (TextView)findViewById(R.id.learn_result_s);
                learnLast_s = (Button)findViewById(R.id.learn_last_s);
                learnPlay_s = (Button)findViewById(R.id.learn_play_s);
                learnNext_s = (Button)findViewById(R.id.learn_next_s);
                learnSwitch_s = (Switch)findViewById(R.id.learn_switch_s);
                learnSwitchTip_s = (TextView)findViewById(R.id.learn_switch_tip_s);

                uText0 = (TextView)findViewById(R.id.uText0);
                uText1 = (TextView)findViewById(R.id.uText1);
                log_s = (Switch)findViewById(R.id.log_s);

                log_s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked){
                            modeTip_s.setText("开启");
                            changeLogStatus(2);
                            microphone.start();
                        }else {
                            modeTip_s.setText("关闭");
                            changeLogStatus(1);
                            microphone.interrupt();
                        }
                    }
                });

                testTip_s = (TextView)findViewById(R.id.test_tip_s);
                testResult_s = (TextView)findViewById(R.id.test_result_s);
                testStart_s = (Button)findViewById(R.id.test_start_s);


                adapter_s = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list_s);
                adapter_s.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                changeModeSpinner_s.setAdapter(adapter_s);

                changeModeSpinner_s.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                                               int arg2, long arg3) {
                        /* 将所选mySpinner 的值带入myTextView 中 */
                        switch (arg2){
                            case 0:
                                state_s = SEQUENTIAL_SETUP;
                                break;
                            case 1:
                                if(candidates_s.size() != seqSize){
                                    int minus = seqSize - candidates_s.size();
                                    int vacant[] = new int[minus];
                                    int flag = 0;
                                    for(int i = 0; i < seqSize; i++){
                                        if(qSequentials[i].setup == false){
                                            vacant[flag] = i;
                                            flag ++;
                                        }
                                    }
                                    AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
                                    nameNull.setTitle("失败" ) ;
                                    String tip = "仍有"+ minus + "个手势未设置，分别为";
                                    for(int i = 0; i < minus; i++){
                                        tip = tip + vacant[i] + ",";
                                    }
                                    nameNull.setMessage(tip) ;
                                    nameNull.setPositiveButton("ok" ,  null );
                                    nameNull.show();
                                    state_s = SEQUENTIAL_SETUP;
                                }
                                else{
                                    state_s = SEQUENTIAL_LEARN;
                                }
                                break;
                            case 2:
                                if(candidates_s.size() != seqSize){
                                    int minus = seqSize - candidates_s.size();
                                    int vacant[] = new int[minus];
                                    int flag = 0;
                                    for(int i = 0; i < seqSize; i++){
                                        if(qSequentials[i].setup == false){
                                            vacant[flag] = i;
                                            flag ++;
                                        }
                                    }
                                    AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
                                    nameNull.setTitle("失败" ) ;
                                    String tip = "仍有"+ minus + "个手势未设置，分别为";
                                    for(int i = 0; i < minus; i++){
                                        tip = tip + vacant[i] + ",";
                                    }
                                    nameNull.setMessage(tip) ;
                                    nameNull.setPositiveButton("ok" ,  null );
                                    nameNull.show();
                                    state_s = SEQUENTIAL_SETUP;
                                }
                                else{
                                    state_s = SEQUENTIAL_TEST;
                                }
                                break;
                        }
                        StateOnChange_s();
                    }

                    public void onNothingSelected(AdapterView<?> arg0) {

                    }
                });

                // 默认组件及初始组件visible
                changeModeSpinner_s.setVisibility(View.VISIBLE);
                modeTip_s.setVisibility(View.VISIBLE);
                setupTip_s.setVisibility(View.VISIBLE);
                setupSave_s.setVisibility(View.VISIBLE);
                setupLast_s.setVisibility(View.VISIBLE);
                setupPlay_s.setVisibility(View.VISIBLE);
                setupNext_s.setVisibility(View.VISIBLE);

                // 其他模式组件gone
                learnFunction_s.setVisibility(View.GONE);
                learnResult_s.setVisibility(View.GONE);
                learnLast_s.setVisibility(View.GONE);
                learnPlay_s.setVisibility(View.GONE);
                learnNext_s.setVisibility(View.GONE);
                learnSwitch_s.setVisibility(View.GONE);
                learnSwitchTip_s.setVisibility(View.GONE);
                testTip_s.setVisibility(View.GONE);
                testResult_s.setVisibility(View.GONE);
                testStart_s.setVisibility(View.GONE);

                state_s = SEQUENTIAL_SETUP;

                setupNumber = 0;
                learnNumber = 0;
                break;
        }
    }



    // 识别触摸事件
    public boolean onTouchEvent(MotionEvent event) {
        //return super.onTouchEvent(event);

        int n = event.getPointerCount();
        int index = event.getActionIndex();
        int pointerID = event.getPointerId(index);
        int x = (int)event.getX(index);
        int y = (int)event.getY(index);

        switch (page) {
            case VIEW_SEQUENTIAL:
                switch (state_s){
                    case SEQUENTIAL_SETUP:
                    case SEQUENTIAL_LEARN:
                    case SEQUENTIAL_TEST:
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_POINTER_DOWN:
                                touchEvent[pointerID] = new TouchEvent(this, x, y);
                                if (idleTimer != null) {
                                    idleTimer.cancel();
                                    idleTimer = null;
                                }
                                break;

                            case MotionEvent.ACTION_MOVE:
                                touchEvent[pointerID].move(x, y);
                                break;

                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_POINTER_UP:
                                TouchEvent touch = touchEvent[pointerID];
                                touch.up(x, y);
                                qtouchs.add(new QTouch((touch.x + touch.downX) / 2, (touch.y + touch.downY) / 2, touch.downTime, touch.currentTime));
                                if (n - 1 == 0) {
                                    idleTimer = new Timer();
                                    idleTimer.schedule(new IdleTimerTask(), 200);
                                }
                                touchEvent[pointerID] = null;
                                break;
                        }
                        break;
                }
                break;
            case VIEW_GESTURE:
                switch (state) {
                    case GESTURE_SETUP:
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                qmoves.clear();
                                qmoves.add(new Point(x, y));
                                break;
                            case MotionEvent.ACTION_MOVE:
                                qmoves.add(new Point(x, y));
                                break;
                            case MotionEvent.ACTION_UP:
                                qmoves.add(new Point(x, y));
                                qmoves = SetUpCandi(qmoves);
                                break;
                        }
                        break;
                    case GESTURE_LEARN:
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                qmoves.clear();
                                qmoves.add(new Point(x, y));
                                break;
                            case MotionEvent.ACTION_MOVE:
                                qmoves.add(new Point(x, y));
                                break;
                            case MotionEvent.ACTION_UP:
                                qmoves.add(new Point(x, y));
                                String a = CompareCandi(qmoves);
                                if(a == qGestures[learnNumber].gestureName){
                                    learnResult.setText("敲击正确");
                                }
                                else {
                                    learnResult.setText("敲击错误");
                                }
                                break;
                        }
                        break;
                    case GESTURE_TEST:
                        if(isTesting){
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    qmoves.clear();
                                    qmoves.add(new Point(x, y));
                                    break;
                                case MotionEvent.ACTION_MOVE:
                                    qmoves.add(new Point(x, y));
                                    break;
                                case MotionEvent.ACTION_UP:
                                    qmoves.add(new Point(x, y));
                                    String a = CompareCandi(qmoves);
                                    if(a == testCase[testNum]){
                                        rightCaseNum = rightCaseNum + 1;
                                        testResult.setText("绘制正确");
                                    }
                                    else {
                                        wrongCaseNum = wrongCaseNum + 1;
                                        testResult.setText("绘制错误");
                                    }
                                    testNum = testNum + 1;
                                    if(testNum == testTime){
                                        endTime = System.currentTimeMillis();
                                        usedTime = endTime-startTime;
                                        isTesting = false;
                                        testNum = 0;
                                        testStart.setEnabled(true);
                                        AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
                                        nameNull.setTitle("测试已完成" ) ;
                                        nameNull.setMessage("用时"+usedTime+"ms\n"+"正确率"+(double)rightCaseNum/(rightCaseNum+wrongCaseNum) ) ;
                                        rightCaseNum = 0;
                                        wrongCaseNum = 0;
                                        nameNull.setPositiveButton("ok" ,  null );
                                        nameNull.show();
                                    }
                                    else{
                                        int tmp = testNum;
                                        int playNumber[] = new int[2];
                                        if(tmp < 10){
                                            soundUtils.playSound(tmp,0);
                                        }
                                        else{
                                            playNumber[1] = tmp % 10;
                                            playNumber[0] = tmp / 10;
                                            soundUtils.playSound(playNumber[0],0);
                                            soundUtils.playSound(playNumber[1],0);
                                        }
                                        testTip.setText("请绘制 "+testCase[testNum]);
                                    }
                                    qmoves.clear();
                                    break;
                            }
                        }
                }
                break;
        }
        return super.onTouchEvent(event);
    }



    // seq相关函数

    // 切换模式
    public void StateOnChange_s(){
        switch (state_s) {
            case SEQUENTIAL_SETUP:
                setupTip_s.setVisibility(View.VISIBLE);
                setupSave_s.setVisibility(View.VISIBLE);
                setupLast_s.setVisibility(View.VISIBLE);
                setupPlay_s.setVisibility(View.VISIBLE);
                setupNext_s.setVisibility(View.VISIBLE);
                learnFunction_s.setVisibility(View.GONE);
                learnResult_s.setVisibility(View.GONE);
                learnLast_s.setVisibility(View.GONE);
                learnPlay_s.setVisibility(View.GONE);
                learnNext_s.setVisibility(View.GONE);
                learnSwitch_s.setVisibility(View.GONE);
                learnSwitchTip_s.setVisibility(View.GONE);
                testTip_s.setVisibility(View.GONE);
                testResult_s.setVisibility(View.GONE);
                testStart_s.setVisibility(View.GONE);

                SetEnableRunnable runnableSetup = new SetEnableRunnable(SEQUENTIAL_SETUP, qSequentials[setupNumber].runTime);
                Thread threadSetup = new Thread(runnableSetup);
                threadSetup.start();

                drawView.drawNothing();
                drawFlash.drawTapFlash(setupNumber);

                modeTip_s.setText("请对第"+ setupNumber + "个功能进行初始设置");
                setupTip_s.setText(qSequentials[setupNumber].tapName);
                break;

            case SEQUENTIAL_LEARN:
                setupTip_s.setVisibility(View.GONE);
                setupSave_s.setVisibility(View.GONE);
                setupLast_s.setVisibility(View.GONE);
                setupPlay_s.setVisibility(View.GONE);
                setupNext_s.setVisibility(View.GONE);
                learnFunction_s.setVisibility(View.VISIBLE);
                learnResult_s.setVisibility(View.VISIBLE);
                learnLast_s.setVisibility(View.VISIBLE);
                learnPlay_s.setVisibility(View.VISIBLE);
                learnNext_s.setVisibility(View.VISIBLE);
                learnSwitch_s.setVisibility(View.VISIBLE);
                learnSwitchTip_s.setVisibility(View.VISIBLE);
                testTip_s.setVisibility(View.GONE);
                testResult_s.setVisibility(View.GONE);
                testStart_s.setVisibility(View.GONE);

                drawView.drawNothing();
                if(showTipFlash_s){
                    drawFlash.drawTapFlash(learnNumber);
                    SetEnableRunnable runnableLearn = new SetEnableRunnable(SEQUENTIAL_LEARN, qSequentials[learnNumber].runTime);
                    Thread threadLearn = new Thread(runnableLearn);
                    threadLearn.start();
                }
                else {
                    drawFlash.drawNothing();
                }

                modeTip_s.setText("请巩固学习第"+ learnNumber + "个功能");
                learnFunction_s.setText(qSequentials[learnNumber].tapName);
                break;
            case SEQUENTIAL_TEST:
                setupTip_s.setVisibility(View.GONE);
                setupSave_s.setVisibility(View.GONE);
                setupLast_s.setVisibility(View.GONE);
                setupPlay_s.setVisibility(View.GONE);
                setupNext_s.setVisibility(View.GONE);
                learnFunction_s.setVisibility(View.GONE);
                learnResult_s.setVisibility(View.GONE);
                learnLast_s.setVisibility(View.GONE);
                learnPlay_s.setVisibility(View.GONE);
                learnNext_s.setVisibility(View.GONE);
                learnSwitch_s.setVisibility(View.GONE);
                learnSwitchTip_s.setVisibility(View.GONE);
                testTip_s.setVisibility(View.VISIBLE);
                testResult_s.setVisibility(View.VISIBLE);
                testStart_s.setVisibility(View.VISIBLE);

                drawView.drawNothing();
                drawFlash.drawNothing();
                modeTip_s.setText("请开始测试，测试共" + testTime_s + "次");
                break;
        }
    }

    public void NextClicked_s(View v){
        writeTxtToFile("next is clicked", "/storage/emulated/0/Android/data/com.pcg.lu.sequential_tap/files/Download", "test.txt");
        if(state_s == SEQUENTIAL_SETUP){
            qtouchs.clear();
            drawView.drawNothing();
            setupNumber = setupNumber + 1;
            if(setupNumber >= seqSize){
                setupNumber = 0;
            }
            SetEnableRunnable runnable = new SetEnableRunnable(SEQUENTIAL_SETUP, qSequentials[setupNumber].runTime);
            Thread thread = new Thread(runnable);
            thread.start();
            drawFlash.drawNothing();
            drawFlash.drawTapFlash(setupNumber);

            modeTip_s.setText("请对第"+ setupNumber + "个功能进行初始设置");
            setupTip_s.setText(qSequentials[setupNumber].tapName);
        }
        else if(state_s == SEQUENTIAL_LEARN){
            qtouchs.clear();
            drawView.drawNothing();
            learnNumber = learnNumber + 1;
            if(learnNumber >= seqSize){
                learnNumber = 0;
            }

            drawFlash.drawNothing();
            if(showTipFlash_s){
                drawFlash.drawTapFlash(learnNumber);
                SetEnableRunnable runnable = new SetEnableRunnable(SEQUENTIAL_LEARN, qSequentials[learnNumber].runTime);
                Thread thread = new Thread(runnable);
                thread.start();
            }
            else {
                drawFlash.drawNothing();
            }

            modeTip_s.setText("请巩固学习第"+ learnNumber + "个功能");
            learnFunction_s.setText(qSequentials[learnNumber].tapName);
        }


    }

    public void PlayClicked_s(View v){
        if(state_s == SEQUENTIAL_SETUP){
            SetEnableRunnable runnable = new SetEnableRunnable(SEQUENTIAL_SETUP, qSequentials[setupNumber].runTime);
            Thread thread = new Thread(runnable);
            thread.start();
            drawFlash.drawNothing();
            drawFlash.drawTapFlash(setupNumber);
        }
        else if(state_s == SEQUENTIAL_LEARN){

            drawFlash.drawNothing();
            if(showTipFlash_s){
                drawFlash.drawTapFlash(learnNumber);
                SetEnableRunnable runnable = new SetEnableRunnable(SEQUENTIAL_LEARN, qSequentials[learnNumber].runTime);
                Thread thread = new Thread(runnable);
                thread.start();
            }
            else {
                drawFlash.drawNothing();
            }
        }
    }

    public void LastClicked_s(View v){
        if(state_s == SEQUENTIAL_SETUP){
            qtouchs.clear();
            drawView.drawNothing();
            setupNumber = setupNumber - 1;
            if(setupNumber < 0){
                setupNumber = seqSize - 1;
            }
            SetEnableRunnable runnable = new SetEnableRunnable(SEQUENTIAL_SETUP, qSequentials[setupNumber].runTime);
            Thread thread = new Thread(runnable);
            thread.start();
            drawFlash.drawNothing();
            drawFlash.drawTapFlash(setupNumber);

            modeTip_s.setText("请对第"+ setupNumber + "个功能进行初始设置");
            setupTip_s.setText(qSequentials[setupNumber].tapName);
        }
        else if(state_s == SEQUENTIAL_LEARN){
            qtouchs.clear();
            drawView.drawNothing();
            learnNumber = learnNumber - 1;
            if(learnNumber < 0){
                learnNumber = seqSize - 1;
            }

            drawFlash.drawNothing();
            if(showTipFlash_s){
                drawFlash.drawTapFlash(learnNumber);
                SetEnableRunnable runnable = new SetEnableRunnable(SEQUENTIAL_LEARN, qSequentials[learnNumber].runTime);
                Thread thread = new Thread(runnable);
                thread.start();
            }
            else {
                drawFlash.drawNothing();
            }

            modeTip_s.setText("请巩固学习第"+ learnNumber + "个功能");
            learnFunction_s.setText(qSequentials[learnNumber].tapName);
        }
    }

    // 敲击计时
    class IdleTimerTask extends TimerTask {
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    QTouch.sequentialize(qtouchs);
                    drawView.drawIntervel(qtouchs);
                    qtouchs_copy.clear();
                    for(int i = 0; i < qtouchs.size(); i++){
                        QTouch p = new QTouch(qtouchs.get(i).x, qtouchs.get(i).y, qtouchs.get(i).t0, qtouchs.get(i).t1);
                        p.l = qtouchs.get(i).l;
                        p.r = qtouchs.get(i).r;
                        p.order = qtouchs.get(i).order;
                        p.index = qtouchs.get(i).index;
                        qtouchs_copy.add(p);
                    }
                    if(page == VIEW_SEQUENTIAL && state_s == SEQUENTIAL_TEST && isTesting_s){
                        String a = CompareCandi_s();
                        if(a == testCase_s[testNum_s]){
                            rightCaseNum_s = rightCaseNum_s + 1;
                            testResult_s.setText("敲击正确");
                        }
                        else {
                            wrongCaseNum_s = wrongCaseNum_s + 1;
                            testResult_s.setText("敲击错误");
                        }
                        testNum_s = testNum_s + 1;
                        if(testNum_s == testTime_s){
                            endTime_s = System.currentTimeMillis();
                            usedTime_s = endTime_s-startTime_s;
                            isTesting_s = false;
                            testNum_s = 0;
                            testStart_s.setEnabled(true);
                            AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
                            nameNull.setTitle("测试已完成" ) ;
                            nameNull.setMessage("用时"+usedTime_s+"ms\n"+"正确率"+(double)rightCaseNum_s/(rightCaseNum_s+wrongCaseNum_s) ) ;
                            rightCaseNum_s = 0;
                            wrongCaseNum_s = 0;
                            nameNull.setPositiveButton("ok" ,  null );
                            nameNull.show();
                        }
                        else{
                            int tmp = testNum_s;
                            int playNumber[] = new int[2];
                            if(tmp < 10){
                                soundUtils.playSound(tmp,0);
                            }
                            else{
                                playNumber[1] = tmp % 10;
                                playNumber[0] = tmp / 10;
                                soundUtils.playSound(playNumber[0],0);
                                soundUtils.playSound(playNumber[1],0);
                            }
                            testTip_s.setText("请敲击 "+testCase_s[testNum_s]);
                        }
                    }
                    else if(page == VIEW_SEQUENTIAL && state_s == SEQUENTIAL_LEARN){
                        String a = CompareCandi_s();
                        if(a == qSequentials[learnNumber].tapName){
                            learnResult_s.setText("敲击正确");
                        }
                        else {
                            learnResult_s.setText("敲击错误");
                        }
                    }
                    qtouchs = new ArrayList();
                }
            });
        }
    }

    // 消除敲击先后差异
    public ArrayList<QTouch> clearDiffer(ArrayList<QTouch> qtouches){
        Collections.sort(qtouchs_copy, new Comparator<QTouch>() {
            public int compare(QTouch lhs, QTouch rhs) {
                if ( lhs.t0 > rhs.t0 ) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        for(int i = 0; i < qtouches.size()-1; i++){
            if(AbsoluteVal(qtouches.get(i).t0,qtouches.get(i+1).t0,20)) {
                if(qtouches.get(i).order < qtouches.get(i+1).order){
                    qtouches.get(i+1).order = qtouches.get(i).order;
                }
                if(qtouches.get(i).order > qtouches.get(i+1).order){
                    qtouches.get(i).order = qtouches.get(i+1).order;
                }
            }
        }
        for(int i = qtouches.size()-1 ; i > 0; i--){
            if(AbsoluteVal(qtouches.get(i).t0,qtouches.get(i-1).t0,20)){
                if(qtouches.get(i).order < qtouches.get(i-1).order){
                    qtouches.get(i-1).order = qtouches.get(i).order;
                }
                if(qtouches.get(i).order > qtouches.get(i-1).order){
                    qtouches.get(i).order = qtouches.get(i-1).order;
                }
            }
        }
        Collections.sort(qtouchs_copy, new Comparator<QTouch>() {
            public int compare(QTouch lhs, QTouch rhs) {
                if ( lhs.index > rhs.index ) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        return qtouches;
    }

    // 保存敲击
    public void SaveClicked_s(View v){
        final String name = qSequentials[setupNumber].tapName;

        if(qSequentials[setupNumber].setup){
            // 已经设置过该seq
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            //    设置Title的内容
            builder.setTitle("警告");
            //    设置Content来显示一个信息
            builder.setMessage("您已设置过该快捷操作，是否清除并重新设置？");
            //    设置一个PositiveButton
            builder.setPositiveButton("是", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Toast.makeText(MainActivity.this, "已清除", Toast.LENGTH_SHORT).show();
                    int delNum = 0;
                    for (int i = 0; i < candidates_s.size(); i++){
                        if(name == candidates_s.get(i).name){
                            delNum = i;
                            break;
                        }
                    }
                    candidates_s.remove(delNum);

                    ArrayList<QTouch> tmp = new ArrayList();
                    for(int i = 0; i < qtouchs_copy.size(); i++){
                        qtouchs_copy = clearDiffer(qtouchs_copy);
                        QTouch p = new QTouch(qtouchs_copy.get(i).x, qtouchs_copy.get(i).y, qtouchs_copy.get(i).t0, qtouchs_copy.get(i).t1);
                        p.l = qtouchs_copy.get(i).l;
                        p.r = qtouchs_copy.get(i).r;
                        p.order = qtouchs_copy.get(i).order;
                        p.index = qtouchs_copy.get(i).index;
                        tmp.add(p);
                    }
                    Candidate_s myCandi = new Candidate_s(name, tmp);
                    candidates_s.add(myCandi);
                    qSequentials[setupNumber].setup = true;
                }
            });
            //    设置一个NegativeButton
            builder.setNegativeButton("否", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Toast.makeText(MainActivity.this, "未清除", Toast.LENGTH_SHORT).show();
                }
            });
            //    显示出该对话框
            builder.show();
        }
        else{
            // 还未设置过该seq
            ArrayList<QTouch> tmp = new ArrayList();
            for(int i = 0; i < qtouchs_copy.size(); i++){
                qtouchs_copy = clearDiffer(qtouchs_copy);
                QTouch p = new QTouch(qtouchs_copy.get(i).x, qtouchs_copy.get(i).y, qtouchs_copy.get(i).t0, qtouchs_copy.get(i).t1);
                p.l = qtouchs_copy.get(i).l;
                p.r = qtouchs_copy.get(i).r;
                p.order = qtouchs_copy.get(i).order;
                p.index = qtouchs_copy.get(i).index;
                tmp.add(p);
            }
            Candidate_s myCandi = new Candidate_s(name, tmp);
            candidates_s.add(myCandi);
            qSequentials[setupNumber].setup = true;

            AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
            nameNull.setTitle("成功" ) ;
            nameNull.setMessage("已成功保存") ;
            nameNull.setPositiveButton("ok" ,  null );
            nameNull.show();
        }
    }

    // 对比候选项
    public String CompareCandi_s(){
        boolean isFound = false;
        qtouchs_copy = clearDiffer(qtouchs_copy);

        for(int i = 0; i < candidates_s.size(); i++){
            for(int j = 0; j < candidates_s.get(i).touchs.size(); j++){
                if(isFound == true){
                    if(j == 0){
                        learnResult_s.setText(candidates_s.get(i-1).name);
                        return candidates_s.get(i-1).name;
                    }
                    else{
                        learnResult_s.setText(candidates_s.get(i).name);
                        return candidates_s.get(i).name;
                    }

                }
                isFound = true;
                if(candidates_s.get(i).touchs.get(j).size() == qtouchs_copy.size()){
                    for(int k = 0; k < qtouchs_copy.size()-1; k++){
                        if(candidates_s.get(i).touchs.get(j).get(k).index == qtouchs_copy.get(k).index
                                && candidates_s.get(i).touchs.get(j).get(k).order == qtouchs_copy.get(k).order
                                && AbsoluteVal(
                                        (Math.pow(candidates_s.get(i).touchs.get(j).get(k).x - candidates_s.get(i).touchs.get(j).get(k+1).x, 2)
                                                + Math.pow(candidates_s.get(i).touchs.get(j).get(k).y - candidates_s.get(i).touchs.get(j).get(k+1).y, 2)),
                                        (Math.pow(qtouchs_copy.get(k).x - qtouchs_copy.get(k+1).x, 2)
                                                + Math.pow(qtouchs_copy.get(k).y - qtouchs_copy.get(k+1).y, 2)),
                                100000) ){
                            continue;
                        }
                        else {
                            isFound = false;
                            break;
                        }
                    }
                    if(candidates_s.get(i).touchs.get(j).get(qtouchs_copy.size()-1).index == qtouchs_copy.get(qtouchs_copy.size()-1).index
                            && candidates_s.get(i).touchs.get(j).get(qtouchs_copy.size()-1).order == qtouchs_copy.get(qtouchs_copy.size()-1).order){
                        // 最后的点的index和order
                    }
                    else{
                        isFound = false;
                        continue;
                    }
                }
                else {
                    isFound = false;
                    continue;
                }
            }
        }
        if(isFound == true){
            learnResult_s.setText(candidates_s.get(candidates_s.size()-1).name);
            return candidates_s.get(candidates_s.size()-1).name;
        }

        learnResult_s.setText("not Found");
        return null;
    }

    // 测试开始函数
    public void TestStart_s(View v){
        int caseSize = candidates_s.size();
        if(caseSize == 0){
            AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
            nameNull.setTitle("错误" ) ;
            nameNull.setMessage("预设为空") ;
            nameNull.setPositiveButton("ok" ,  null );
            nameNull.show();
        }
        else{
            testCase_s = new String[testTime_s];
            Random r = new Random();
            for(int i = 0; i < testTime_s; i++){
                testCase_s[i] = candidates_s.get(r.nextInt(caseSize)).name;
            }
            System.out.println("Tip 待测试sequential序列：");
            for(int i = 0; i < testTime_s; i++){
                System.out.println("Tip "+ i + ": "+ testCase_s[i]);
            }

            startTime_s = System.currentTimeMillis();
//            saveInternal(Long.toString(startTime_s), "this is a test");
            soundUtils.playSound(0,0);
            testTip_s.setText("请敲击 "+testCase_s[0]);
            isTesting_s = true;
            testStart_s.setEnabled(false);
        }


    }



    // gesture相关函数

    // 切换模式
    public void StateOnChange(){
        switch (state) {
            case GESTURE_SETUP:
                setupTip.setVisibility(View.VISIBLE);
                setupSave.setVisibility(View.VISIBLE);
                setupLast.setVisibility(View.VISIBLE);
                setupPlay.setVisibility(View.VISIBLE);
                setupNext.setVisibility(View.VISIBLE);
                learnFunction.setVisibility(View.GONE);
                learnResult.setVisibility(View.GONE);
                learnLast.setVisibility(View.GONE);
                learnPlay.setVisibility(View.GONE);
                learnNext.setVisibility(View.GONE);
                testTip.setVisibility(View.GONE);
                testResult.setVisibility(View.GONE);
                testStart.setVisibility(View.GONE);

                SetEnableRunnable runnableSetup = new SetEnableRunnable(GESTURE_SETUP, qGestures[setupNumber].runTime);
                Thread threadSetup = new Thread(runnableSetup);
                threadSetup.start();

                drawView2.drawNothing();
                drawFlash2.drawShapeFlash(setupNumber);

                modeTip.setText("请对第"+ setupNumber + "个功能进行初始设置");
                setupTip.setText(qGestures[setupNumber].gestureName);
                break;

            case GESTURE_LEARN:
                setupTip.setVisibility(View.GONE);
                setupSave.setVisibility(View.GONE);
                setupLast.setVisibility(View.GONE);
                setupPlay.setVisibility(View.GONE);
                setupNext.setVisibility(View.GONE);
                learnFunction.setVisibility(View.VISIBLE);
                learnResult.setVisibility(View.VISIBLE);
                learnLast.setVisibility(View.VISIBLE);
                learnPlay.setVisibility(View.VISIBLE);
                learnNext.setVisibility(View.VISIBLE);
                testTip.setVisibility(View.GONE);
                testResult.setVisibility(View.GONE);
                testStart.setVisibility(View.GONE);

                SetEnableRunnable runnableLearn = new SetEnableRunnable(GESTURE_LEARN, qGestures[learnNumber].runTime);
                Thread threadLearn = new Thread(runnableLearn);
                threadLearn.start();

                drawView2.drawNothing();
                drawFlash2.drawShapeFlash(learnNumber);

                modeTip.setText("请巩固学习第"+ learnNumber + "个功能");
                learnFunction.setText(qGestures[learnNumber].gestureName);
                break;

            case GESTURE_TEST:
                setupTip.setVisibility(View.GONE);
                setupSave.setVisibility(View.GONE);
                setupLast.setVisibility(View.GONE);
                setupPlay.setVisibility(View.GONE);
                setupNext.setVisibility(View.GONE);
                learnFunction.setVisibility(View.GONE);
                learnResult.setVisibility(View.GONE);
                learnLast.setVisibility(View.GONE);
                learnPlay.setVisibility(View.GONE);
                learnNext.setVisibility(View.GONE);
                testTip.setVisibility(View.VISIBLE);
                testResult.setVisibility(View.VISIBLE);
                testStart.setVisibility(View.VISIBLE);

                drawView2.drawNothing();
                drawFlash2.drawNothing();
                modeTip.setText("请开始测试，测试共" + testTime + "次");
                break;
        }
    }

    public void NextClicked(View v){
        if(state == GESTURE_SETUP){
            setupNumber = setupNumber + 1;
            if(setupNumber >= gestureSize){
                setupNumber = 0;
            }
            SetEnableRunnable runnable = new SetEnableRunnable(GESTURE_SETUP, qGestures[setupNumber].runTime);
            Thread thread = new Thread(runnable);
            thread.start();
            drawFlash2.drawNothing();
            drawFlash2.drawShapeFlash(setupNumber);

            modeTip.setText("请对第"+ setupNumber + "个功能进行初始设置");
            setupTip.setText(qGestures[setupNumber].gestureName);
        }
        else if(state == GESTURE_LEARN){
            learnNumber = learnNumber + 1;
            if(learnNumber >= gestureSize){
                learnNumber = 0;
            }
            SetEnableRunnable runnable = new SetEnableRunnable(GESTURE_LEARN, qGestures[learnNumber].runTime);
            Thread thread = new Thread(runnable);
            thread.start();
            drawFlash2.drawNothing();
            drawFlash2.drawShapeFlash(learnNumber);

            modeTip.setText("请巩固学习第"+ learnNumber + "个功能");
            learnFunction.setText(qGestures[learnNumber].gestureName);
        }
        qmoves.clear();
        drawView2.drawShape(qmoves);
    }

    public void PlayClicked(View v){
        if(state == GESTURE_SETUP){
            SetEnableRunnable runnable = new SetEnableRunnable(GESTURE_SETUP, qGestures[setupNumber].runTime);
            Thread thread = new Thread(runnable);
            thread.start();
            drawFlash2.drawNothing();
            drawFlash2.drawShapeFlash(setupNumber);
        }
        else if(state == GESTURE_LEARN){
            SetEnableRunnable runnable = new SetEnableRunnable(GESTURE_LEARN, qGestures[learnNumber].runTime);
            Thread thread = new Thread(runnable);
            thread.start();
            drawFlash2.drawNothing();
            drawFlash2.drawShapeFlash(learnNumber);
        }
        qmoves.clear();
        drawView2.drawShape(qmoves);
    }

    public void LastClicked(View v){
        if(state == GESTURE_SETUP){
            setupNumber = setupNumber - 1;
            if(setupNumber < 0){
                setupNumber = gestureSize - 1;
            }
            SetEnableRunnable runnable = new SetEnableRunnable(GESTURE_SETUP, qGestures[setupNumber].runTime);
            Thread thread = new Thread(runnable);
            thread.start();
            drawFlash2.drawNothing();
            drawFlash2.drawShapeFlash(setupNumber);

            modeTip.setText("请对第"+ setupNumber + "个功能进行初始设置");
            setupTip.setText(qGestures[setupNumber].gestureName);
        }
        else if(state == GESTURE_LEARN){
            learnNumber = learnNumber - 1;
            if(learnNumber < 0){
                learnNumber = gestureSize - 1;
            }
            SetEnableRunnable runnable = new SetEnableRunnable(GESTURE_LEARN, qGestures[learnNumber].runTime);
            Thread thread = new Thread(runnable);
            thread.start();
            drawFlash2.drawNothing();
            drawFlash2.drawShapeFlash(learnNumber);

            modeTip.setText("请巩固学习第"+ learnNumber + "个功能");
            learnFunction.setText(qGestures[learnNumber].gestureName);
        }
        qmoves.clear();
        drawView2.drawShape(qmoves);
    }

    // 保存手势
    public void SaveClicked(View v){
        final String name = qGestures[setupNumber].gestureName;

        if(qGestures[setupNumber].setup){
            // 已经设置过该gesture
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            //    设置Title的内容
            builder.setTitle("警告");
            //    设置Content来显示一个信息
            builder.setMessage("您已设置过该快捷操作，是否清除并重新设置？");
            //    设置一个PositiveButton
            builder.setPositiveButton("是", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Toast.makeText(MainActivity.this, "已清除", Toast.LENGTH_SHORT).show();
                    int delNum = 0;
                    for (int i = 0; i < candidates.size(); i++){
                        if(name == candidates.get(i).name){
                            delNum = i;
                            break;
                        }
                    }
                    candidates.remove(delNum);

                    ArrayList<Point> tmp = new ArrayList();
                    for(int i = 0; i < qmoves.size(); i++){
                        Point p = new Point(qmoves.get(i).x, qmoves.get(i).y);
                        tmp.add(p);
                    }
                    Candidate myCandi = new Candidate(name, tmp);
                    candidates.add(myCandi);
                    qmoves.clear();
                    drawView2.drawShape(qmoves);
                    qGestures[setupNumber].setup = true;
                }
            });
            //    设置一个NegativeButton
            builder.setNegativeButton("否", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    Toast.makeText(MainActivity.this, "未清除", Toast.LENGTH_SHORT).show();
                }
            });
            //    显示出该对话框
            builder.show();
        }
        else{
            // 还未设置过该seq
            ArrayList<Point> tmp = new ArrayList();
            for(int i = 0; i < qmoves.size(); i++){
                Point p = new Point(qmoves.get(i).x, qmoves.get(i).y);
                tmp.add(p);
            }
            Candidate myCandi = new Candidate(name, tmp);
            candidates.add(myCandi);
            qmoves.clear();
            drawView2.drawShape(qmoves);

            qGestures[setupNumber].setup = true;

            AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
            nameNull.setTitle("成功" ) ;
            nameNull.setMessage("已成功保存") ;
            nameNull.setPositiveButton("ok" ,  null );
            nameNull.show();
        }
    }


    // 初始设置
    public ArrayList<Point> SetUpCandi(ArrayList<Point>points){

        ArrayList<Point> newPoints = new ArrayList();

        if(PathLength(points) < 200.0)
        {
            AlertDialog.Builder shortLine  = new AlertDialog.Builder(MainActivity.this);
            shortLine.setTitle("提示" ) ;
            shortLine.setMessage("路径太短不可以作为手势" ) ;
            shortLine.setPositiveButton("是" ,  null );
            shortLine.show();
            return newPoints;
        }


        newPoints = Resample(points, 64);
        drawView2.drawShape(newPoints);
        newPoints = RotateToZero(newPoints);
        newPoints = ScaleToSquare(newPoints, 400);
        newPoints = TranslateToOrigin(newPoints);
        return newPoints;
    }

    // 第一步 重新采样
    public ArrayList<Point> Resample(ArrayList<Point> points, int n){
        ArrayList<Point> newPoints = new ArrayList();

        double I = PathLength(points) / (n - 1);
        double D = 0.0;

        newPoints.add(points.get(0));
        double d = 0.0;

        for(int i = 1; i < points.size()-1; i++){
            Point first = points.get(i-1);
            Point next = points.get(i);
            d = Distance(first, next);
            Point q = new Point(0,0);
            if((D + d) >= I){
                q.x = (int)(first.x + ((I - D)/d) * (next.x - first.x));
                q.y = (int)(first.y + ((I - D)/d) * (next.y - first.y));
                newPoints.add(q);
                points.add(i, q);
                D = 0.0;
            }
            else{
                D = D + d;
            }
        }

        while(newPoints.size() < n){
            Point q = newPoints.get(newPoints.size() - 1);
            newPoints.add(q);
        }
        while(newPoints.size() > n){
            newPoints.remove(newPoints.size() - 1);
        }
        return newPoints;
    }
    public double PathLength(ArrayList<Point> points){
        double distance = 0.0;
        for(int i = 0; i < points.size() - 1; i++){
            Point first = points.get(i);
            Point next = points.get(i+1);
            distance = distance + Distance(first, next);
        }
        return distance;
    }
    public double Distance(Point a, Point b){
        double dis = Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
        return dis;
    }

    // 第二步 旋转
    public ArrayList<Point> RotateToZero(ArrayList<Point> points) {

        ArrayList<Point> newPoints = new ArrayList();
        Point centre = Centroid(points);
        double tmp = 3.14159266;
        double angle = Math.atan2((points.get(0).y - centre.y),(points.get(0).x - centre.x));

        if(-tmp < angle && angle <= -(tmp * 0.75)){
            angle = angle + tmp;
        }
        else if(-(tmp * 0.75) < angle && angle <= -(tmp * 0.25)){
            angle = angle + tmp * 0.5;
        }
        else if(-(tmp * 0.25) < angle && angle <= tmp * 0.25){

        }
        else if(tmp * 0.25 < angle && angle <= tmp * 0.75){
            angle = angle - tmp * 0.5;
        }
        else{
            angle = angle - tmp;
        }
        newPoints = RotateBy(points, -angle);
        return newPoints;
    }
    public Point Centroid(ArrayList<Point> points){
        Point c = new Point(0, 0);
        double x = 0.0;
        double y = 0.0;
        for(int i = 0; i < points.size(); i++){
            x = x + points.get(i).x;
            y = y + points.get(i).y;
        }
        c.x = (int)(x / points.size());
        c.y = (int)(y / points.size());
        return c;
    }
    public ArrayList<Point> RotateBy(ArrayList<Point> points, double angle){
        ArrayList<Point> newPoints = new ArrayList();
        Point centre = Centroid(points);
        for(int i = 0; i < points.size(); i++){
            Point q = new Point(0, 0);
            Point p = points.get(i);
            q.x = (int)((p.x - centre.x) * Math.cos(angle) - (p.y - centre.y) * Math.sin(angle) + centre.x);
            q.y = (int)((p.x - centre.x) * Math.sin(angle) + (p.y - centre.y) * Math.cos(angle) + centre.y);
            newPoints.add(q);
        }
        return newPoints;
    }

    // 第三步 缩放
    public ArrayList<Point> ScaleToSquare(ArrayList<Point> points, double size){
        ArrayList<Point> newPoints = new ArrayList();
        Dimension B = BoundingBox(points);

        double AspectRatio = 0.0;
        if(B.x == 0 || B.y == 0){
            AspectRatio = 0;
        }
        else if(B.x > B.y){
            AspectRatio = B.x/B.y;
        }
        else {
            AspectRatio = B.y/B.x;
        }

        if(AspectRatio < 15){
            for(int i = 0; i < points.size(); i++){
                Point q = new Point(0,0);
                Point p = points.get(i);
                q.x = (int)(p.x * (size / B.x));
                q.y = (int)(p.y * (size / B.y));
                newPoints.add(q);
            }
        }
        else{
            for(int i = 0; i < points.size(); i++){
                Point q = new Point(0,0);
                Point p = points.get(i);
                if(B.x > B.y){
                    q.x = (int)(p.x * (size / B.x));
                    q.y = (int)(p.y);
                }
                else{
                    q.y = (int)(p.y * (size / B.y));
                    q.x = (int)(p.x);
                }

                newPoints.add(q);
            }
        }

        return newPoints;
    }
    public Dimension BoundingBox( ArrayList<Point> points){
        Dimension B = new Dimension(0, 0);
        int minx = 100000000;
        int miny = 100000000;
        int maxx = -1;
        int maxy = -1;
        for(int i = 0; i < points.size(); i++){
            Point tmp = points.get(i);
            if(tmp.x < minx){
                minx = tmp.x;
            }
            if(tmp.x > maxx){
                maxx = tmp.x;
            }
            if(tmp.y < miny){
                miny = tmp.y;
            }
            if(tmp.y > maxy){
                maxy = tmp.y;
            }
        }
        B.x = maxx - minx;
        B.y = maxy - miny;
        return B;
    }

    // 第四步 旋转
    public ArrayList<Point> TranslateToOrigin(ArrayList<Point> points){
        ArrayList<Point> newPoints = new ArrayList();
        Point centre = Centroid(points);

        for(int i = 0; i < points.size(); i++){
            Point q = new Point(0, 0);
            Point p = points.get(i);
            q.x = p.x - centre.x;
            q.y = p.y - centre.y;
            newPoints.add(q);
        }
        return newPoints;
    }


    // 对比候选项
    public RecognizeResult Recognize(ArrayList<Point> points, ArrayList<Candidate> templates){
        RecognizeResult returnResult = new RecognizeResult();
        double b = 1000000;

//        drawView2.drawShape(points);
        for(int i = 0; i < templates.size(); i++){
            for(int j = 0; j < templates.get(i).points.size(); j++){
                double d = DistanceAtBestAngle(points, i, j,-0.785, 0.785, 0.0349);
                if(d < b){
                    b = d;
                    returnResult.template = templates.get(i);
                }
            }
        }

        // size:400
        double score = 1 - b/0.5 * Math.sqrt(Math.pow(400, 2) + Math.pow(400, 2));
        returnResult.score = score;
        return returnResult;
    }
    public double DistanceAtBestAngle(ArrayList<Point> points, int T, int num, double thetaA, double thetaB, double delta){

        double parameterF =  0.5 * (-1 + Math.sqrt(5));
        double x1 = parameterF * thetaA + (1 - parameterF) * thetaB;
        double f1 = DistanceAtAngle(points, T, num, x1);
        double x2 = (1 - parameterF) * thetaA + parameterF * thetaB;
        double f2 = DistanceAtAngle(points, T, num, x2);

        while ((thetaA - thetaB) > delta || (thetaB - thetaA) > delta){
            if(f1 < f2){
                thetaB = x2;
                x2 = x1;
                f2 = f1;
                x1 = parameterF * thetaA + (1 - parameterF) * thetaB;
                f1 = DistanceAtAngle(points, T, num, x1);
            }
            else {
                thetaA = x1;
                x1 = x2;
                f1 = f2;
                x2 = (1 - parameterF) * thetaA + parameterF * thetaB;
                f2 = DistanceAtAngle(points, T, num, x2);
            }
        }
        if(f1 < f2){
            return f1;
        }
        else {
            return f2;
        }
    }
    public double DistanceAtAngle(ArrayList<Point> points, int T, int num, double theta){
        ArrayList<Point> newPoints = RotateBy(points, theta);

        double d = PathDistance(newPoints, T, num);
        newPoints = RotateBy(points, -theta);
        return d;
    }
    public double PathDistance(ArrayList<Point> a, int T, int num){
        double d = 0;
        for(int i = 0; i < a.size(); i++){
            d = d + Distance(a.get(i), candidates.get(T).points.get(num).get(i));
        }
        return d/a.size();
    }
    public String CompareCandi(ArrayList<Point>points){
        if(candidates.size() == 0){
            AlertDialog.Builder shortLine  = new AlertDialog.Builder(MainActivity.this);
            shortLine.setTitle("提示" ) ;
            shortLine.setMessage("您还没有预设手势" ) ;
            shortLine.setPositiveButton("是" ,  null );
            shortLine.show();
            return "";
        }

        if(PathLength(points) < 100.0)
        {
            AlertDialog.Builder shortLine  = new AlertDialog.Builder(MainActivity.this);
            shortLine.setTitle("提示" ) ;
            shortLine.setMessage("路径太短无法匹配" ) ;
            shortLine.setPositiveButton("是" ,  null );
            shortLine.show();
            return "";
        }

        String a = "";
        ArrayList<Point> newPoints = new ArrayList();
        newPoints = Resample(points, 64);
        drawView2.drawShape(newPoints);
        newPoints = RotateToZero(newPoints);
        newPoints = ScaleToSquare(newPoints, 400);
        newPoints = TranslateToOrigin(newPoints);
        RecognizeResult result = Recognize(newPoints, candidates);

        return result.template.name;
    }

    // 测试开始函数
    public void TestStart(View v){
        int caseSize = candidates.size();
        if(caseSize == 0){
            AlertDialog.Builder nameNull  = new AlertDialog.Builder(MainActivity.this);
            nameNull.setTitle("错误" ) ;
            nameNull.setMessage("预设的手势为空") ;
            nameNull.setPositiveButton("ok" ,  null );
            nameNull.show();
        }
        else{
            testCase = new String[testTime];
            Random r = new Random();
            for(int i = 0; i < testTime; i++){
                testCase[i] = candidates.get(r.nextInt(caseSize)).name;
            }

            System.out.println("Tip 待测试gesture序列：");
            for(int i = 0; i < testTime; i++){
                System.out.println("Tip "+ i + ": "+ testCase[i]);
            }
            startTime = System.currentTimeMillis();

            soundUtils.playSound(0,0);
            testTip.setText("请绘制 "+testCase[0]);
            isTesting = true;
            testStart.setEnabled(false);
        }
    }

//    public void saveInternal(String filename, String data) {
//        FileOutputStream outputStream;
//        try {
//            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
//            outputStream.write(data.getBytes("utf-8"));
//            outputStream.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void writeTxtToFile(String strcontent, String filePath, String fileName) {
        //生成文件夹之后，再生成文件，不然会出错
        makeFilePath(filePath, fileName);

        String strFilePath = filePath + fileName;
        // 每次写入时，都换行写
        String strContent = strcontent + "\r\n";
        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                Log.d("TestFile", "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
        }
    }

    // 生成文件
    public File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    // 生成文件夹
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
    }

    //删除指定txt文件   通过路径
    public void deleteFile(String filePath, String fileName) {
        File f = new File(filePath + fileName);  // 输入要删除的文件位置
        if (f.exists()) {
            f.delete();
        }

    }

}
