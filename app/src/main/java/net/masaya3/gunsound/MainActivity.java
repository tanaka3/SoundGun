package net.masaya3.gunsound;

import android.bluetooth.BluetoothGattCharacteristic;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.uxxu.konashi.lib.Konashi;
import com.uxxu.konashi.lib.KonashiListener;
import com.uxxu.konashi.lib.KonashiManager;

import net.masaya3.gunsound.data.BulletInfo;
import net.masaya3.gunsound.data.GunInfo;

import org.jdeferred.DoneCallback;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import info.izumin.android.bletia.BletiaException;


public class MainActivity extends AppCompatActivity {

    private BulletListAdapter mAdapter;

    enum Mode{
        Gun,
        Load,
        Reload
    }
    private static int POLLING_TIME = 10000;

    private static final String DATE_PATTERN = "yyyyMMddHHmmss";
    private static final int MAX_BULLET = 20;
    private Mode mGunMode = Mode.Gun;
    private MediaPlayer mReloadSound;
    private List<MediaPlayer> mGunSounds = new ArrayList<MediaPlayer>();
    private MediaRecorder mRecorder;
    private String mSoundFile;
    private List<BulletInfo> mBulletList = new ArrayList<BulletInfo>();
    private GunInfo mGunInfo = new GunInfo();
    private Vibrator mVibrator;
    private KonashiManager mKonashiManager;
    private Thread mThread;
    private PollingTask mPollingTask;
    private HashMap<String, BulletInfo> mBulletHistory = new HashMap<String, BulletInfo>();
    private Handler mHandler = new Handler();
    private boolean mAutoReload = false;
    private boolean mCanRapidFire = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        mKonashiManager = new KonashiManager(getApplicationContext());

        //Reload
        FloatingActionButton reload = (FloatingActionButton) findViewById(R.id.reload);
        reload.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        /*
                        if(mKonashiManager != null){
                            if(!mKonashiManager.isConnected()){
                                mKonashiManager.find(MainActivity.this);
                                return false;
                            }
                        }
                        */
                        openMic();
                        break;
                    case MotionEvent.ACTION_UP:
                        closeMic();
                        break;
                }

                return true;
            }
        });

        //shooting
        FloatingActionButton shoot = (FloatingActionButton) findViewById(R.id.shoot);
        shoot.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                shooting();
            }
        });

        //reloading
        FloatingActionButton reload_bullet = (FloatingActionButton) findViewById(R.id.gun_reload);
        reload_bullet.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                reloading();
            }
        });


        mReloadSound = MediaPlayer.create(getApplicationContext(), R.raw.reload);
        mReloadSound.setVolume(0.3f, 0.3f);
        mAdapter = new BulletListAdapter(getApplicationContext(), mBulletList);

        ListView listView = (ListView)findViewById(R.id.listView);
        listView.setAdapter(mAdapter);
    }

    private Runnable mPollingRunnable = new Runnable() {
        @Override
        public void run() {
            if(mPollingTask != null && mPollingTask.isProcessing()){
                mHandler.postDelayed(mPollingRunnable, POLLING_TIME/5);
            }
            else{
                mPollingTask = new PollingTask(getApplicationContext(), mBulletHistory);
                mPollingTask.setOnCompletionListener(new PollingTask.OnCompletionListener() {
                    @Override
                    public void onCompletion(List<BulletInfo> result) {

                        if(result.size() == 0){
                            return;
                        }

                        for(BulletInfo info : result){
                            if(!info.success){
                                continue;
                            }

                            mBulletList.add(info);
                        }
                        mAdapter.notifyDataSetChanged();
                        mReloadSound.start();
                    }
                });
                mPollingTask.start();
                mHandler.postDelayed(mPollingRunnable, POLLING_TIME);
            }

        }
    };

    private void openMic(){
        if(mGunMode != Mode.Gun) {
            return;
        }

        if(mBulletList.size() >= MAX_BULLET){
            return;
        }

        mGunMode = Mode.Load;

        final RelativeLayout layout = (RelativeLayout)findViewById(R.id.mic_layout);
        layout.setVisibility(View.VISIBLE);

        Animation in_animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_in);
        in_animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                try{
                    if(mRecorder != null){
                        mRecorder.stop();
                        mRecorder.reset();   //オブジェクトのリセット
                        mRecorder.release(); //Recorderオブジェクトの解放
                        mRecorder = null;
                    }
                }
                catch (Exception e){
                }

                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setAudioSamplingRate(48000);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

                Calendar now = Calendar.getInstance();
                SimpleDateFormat dateformat = new SimpleDateFormat(DATE_PATTERN);

                String sound_file = String.format("/%s.mp4", dateformat.format(now.getTime()));

                //保存先
                mSoundFile = getCacheDir() + sound_file;
                mRecorder.setOutputFile(mSoundFile);

                //録音準備＆録音開始
                try {
                    mRecorder.prepare();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mRecorder.start();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        layout.startAnimation(in_animation);
    }

    private void closeMic(){

        if(mGunMode != Mode.Load) {
            return;
        }

        final RelativeLayout layout = (RelativeLayout)findViewById(R.id.mic_layout);

        try {
            //録音を停止する
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;

            //弾を追加する
            BulletInfo info = new BulletInfo(mSoundFile);
            mBulletList.add(info);
            mAdapter.notifyDataSetChanged();

            //リロード音再生
            mReloadSound.start();

        }
        //録音に失敗した場合は、追加を行わない
        catch (Exception e){
        }

        Animation out_animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bottom_out);
        out_animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                layout.setVisibility(View.GONE);
                mGunMode = Mode.Gun;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        layout.startAnimation(out_animation);
    }

    private void reloading(){

        if(mGunMode != Mode.Gun){
            return;
        }

        if(mBulletList.size() == 0){
            return;
        }

        mGunMode = Mode.Reload;


        for(MediaPlayer player : mGunSounds) {
            player.stop();
            player.release();
        }
        mGunSounds.clear();

        try {
            Log.e("log-reload", mBulletList.get(0).soundFile);
            this.deleteFile(mBulletList.get(0).soundFile);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        mBulletList.remove(0);
        mAdapter.notifyDataSetChanged();

        mReloadSound.start();

        mGunMode = Mode.Gun;
    }

    private void shooting(){

        if(mGunMode != Mode.Gun){
            return;
        }

        if(mBulletList.size() == 0){
            return;
        }


        //再生中の場合は何もしない
        if (mGunSounds.size() > 0 && !mCanRapidFire) {
            boolean play = false;

            for(MediaPlayer player : mGunSounds) {
                play |= player.isPlaying();
            }

            if(play) {
                return;
            }
        }

        //現在の音を削除する
        for(MediaPlayer player : mGunSounds) {
            player.stop();
            player.release();
        }
        mGunSounds.clear();

        if(mGunInfo.one_shoot) {
            MediaPlayer media = createMediaPlayer(mBulletList.get(0));
            if(media != null){
                mGunSounds.add(media);
            }
        }

        if(mGunInfo.two_shoot) {
            if(mBulletList.size() > 1) {
                MediaPlayer media = createMediaPlayer(mBulletList.get(1));
                if (media != null) {
                    mGunSounds.add(media);
                }
            }
        }

        if(mGunInfo.three_shoot) {
            if(mBulletList.size() > 2) {
                MediaPlayer media = createMediaPlayer(mBulletList.get(2));
                if (media != null) {
                    mGunSounds.add(media);
                }
            }
        }

        if(mGunInfo.four_shoot) {
            if(mBulletList.size() > 3) {
                MediaPlayer media = createMediaPlayer(mBulletList.get(3));
                if (media != null) {
                    mGunSounds.add(media);
                }
            }
        }

        long duration = 0;
        for(MediaPlayer gunsound : mGunSounds) {
            gunsound.start();
            duration = Math.max(duration, gunsound.getDuration()/2);
        }
        mVibrator.vibrate(duration);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mKonashiManager != null) {
            mKonashiManager.addListener(mKonashiListener);

            if (!mKonashiManager.isConnected()) {
                mKonashiManager.find(this);
            }
        }
        mHandler.postDelayed(mPollingRunnable, POLLING_TIME);

    }

    @Override
    protected void onPause() {
        if(mKonashiManager != null) {
            mKonashiManager.removeListener(mKonashiListener);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(mKonashiManager.isConnected()){
                        mKonashiManager.reset()
                                .then(new DoneCallback<BluetoothGattCharacteristic>() {
                                    @Override
                                    public void onDone(BluetoothGattCharacteristic result) {
                                        mKonashiManager.disconnect();
                                    }
                                });
                    }
                }
            }).start();
        }
        mHandler.removeCallbacks(mPollingRunnable);

        super.onPause();

    }

    @Override
    protected void onDestroy() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mKonashiManager.isConnected()){
                    mKonashiManager.reset()
                            .then(new DoneCallback<BluetoothGattCharacteristic>() {
                                @Override
                                public void onDone(BluetoothGattCharacteristic result) {
                                    mKonashiManager.disconnect();
                                }
                            });
                }
            }
        }).start();
        super.onDestroy();
    }


    private MediaPlayer createMediaPlayer(BulletInfo info) {
        MediaPlayer gunSound = new MediaPlayer();

        gunSound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {

                if(mAutoReload){

                    boolean playing = false;
                    for(MediaPlayer player : mGunSounds){
                        if(player == mediaPlayer) {
                            continue;
                        }
                        playing |= player.isPlaying();
                    }

                    if(!playing) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                reloading();
                            }
                        });
                    }
                }
            }
        });
        try {
            gunSound.setDataSource(info.soundFile);
            gunSound.prepare();

            Log.e("log-shoot", info.soundFile);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return gunSound;
    }

    private final KonashiListener mKonashiListener = new KonashiListener() {
        @Override
        public void onConnect(KonashiManager manager) {
            mKonashiManager.analogRead(Konashi.AIO0).then(mAioCallback);
            //
            //mKonashiManager.pinPullupAll(255);
            mKonashiManager.pinModeAll(0);

            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(mThread != null){

                        if(mKonashiManager.isConnected()){

                            //Log.e("demo1", mKonashiManager.digitalRead(1) == 1 ? "ON":"OFF");
                            //Log.e("demo2", mKonashiManager.digitalRead(2) == 1 ? "ON":"OFF");
                            //Log.e("demo3", mKonashiManager.digitalRead(3) == 1 ? "ON":"OFF");

                            mGunInfo.auto = mKonashiManager.digitalRead(1) == 1;
                            mGunInfo.two_shoot = mKonashiManager.digitalRead(2) == 1;
                            mGunInfo.three_shoot = mKonashiManager.digitalRead(3) == 1;
                            mGunInfo.four_shoot = mKonashiManager.digitalRead(4) == 1;

                            //トリガー
                            if(mKonashiManager.digitalRead(0) == 1){

                                if(mGunInfo.triggerType == GunInfo.Type.OFF || mGunInfo.auto)
                                {
                                    shooting();
                                    mGunInfo.triggerType = GunInfo.Type.ON;
                                }
                            }
                            else{
                                mGunInfo.triggerType = GunInfo.Type.OFF;
                            }
                        }

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            mThread.start();

        }

        private final DoneCallback<Integer> mAioCallback = new DoneCallback<Integer>() {
            @Override
            public void onDone(Integer result) {
                if (!mKonashiManager.isConnected()) {
                    return;
                }


                Log.e("ponp", String.format("a:%d", result));

                if(result > 900 && mGunInfo.reloadType == GunInfo.Type.OFF){
                    mGunInfo.reloadType = GunInfo.Type.ON;
                }
                else if(result < 180 && mGunInfo.reloadType == GunInfo.Type.ON){
                    reloading();
                    mGunInfo.reloadType = GunInfo.Type.OFF;
                }
                mKonashiManager.analogRead(Konashi.AIO0).then(mAioCallback);

            }
        };

        @Override
        public void onDisconnect(KonashiManager manager) {
            mThread = null;
        }

        @Override
        public void onError(KonashiManager manager, BletiaException e) {

        }

        @Override
        public void onUpdatePioOutput(KonashiManager manager, int value) {
        }

        @Override
        public void onUpdateUartRx(KonashiManager manager, byte[] value) {

        }

        @Override
        public void onUpdateSpiMiso(KonashiManager manager, byte[] value) {

        }

        @Override
        public void onUpdateBatteryLevel(KonashiManager manager, int level) {

        }
    };
}