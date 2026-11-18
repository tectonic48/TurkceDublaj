package com.gtaturkcedublaj;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQ = 41;
    private TextView status, english, turkish, latency, speedLabel, volumeLabel;
    private SeekBar speed, volume;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (!DubService.ACTION_UPDATE.equals(i.getAction())) return;
            String s = i.getStringExtra("status"); if (s != null) status.setText(s);
            String en = i.getStringExtra("english"); if (en != null) english.setText(en);
            String tr = i.getStringExtra("turkish"); if (tr != null) turkish.setText(tr);
            long ms = i.getLongExtra("latency", -1); if (ms >= 0) latency.setText("Son işlem süresi: " + ms + " ms (tanıma + çeviri başlangıcı)");
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b); setContentView(R.layout.activity_main);
        status=findViewById(R.id.status); english=findViewById(R.id.english); turkish=findViewById(R.id.turkish); latency=findViewById(R.id.latency);
        speed=findViewById(R.id.speed); volume=findViewById(R.id.volume); speedLabel=findViewById(R.id.speedLabel); volumeLabel=findViewById(R.id.volumeLabel);
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ public void onProgressChanged(SeekBar b,int p,boolean u){float v=0.5f+p*0.1f; speedLabel.setText(String.format(java.util.Locale.US,"Konuşma hızı: %.1fx",v));getSharedPreferences("settings",MODE_PRIVATE).edit().putFloat("speed",v).apply();} public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){} });
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ public void onProgressChanged(SeekBar b,int p,boolean u){volumeLabel.setText("Ses seviyesi: "+p+"%");getSharedPreferences("settings",MODE_PRIVATE).edit().putFloat("volume",p/100f).apply();} public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){} });
        speed.setProgress(5); volume.setProgress(100);
        findViewById(R.id.start).setOnClickListener(v -> startDub());
        findViewById(R.id.stop).setOnClickListener(v -> { stopService(new Intent(this,DubService.class)); status.setText("● Durduruldu · Mikrofon kapalı"); });
        IntentFilter f=new IntentFilter(DubService.ACTION_UPDATE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED); else registerReceiver(receiver,f);
    }
    private void startDub(){
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},REQ);return;}
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ+1);
        Intent i=new Intent(this,DubService.class);
        if(Build.VERSION.SDK_INT>=26) ContextCompat.startForegroundService(this,i); else startService(i);
        status.setText("● Başlatılıyor · Mikrofon etkinleşecek");
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ && g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED)startDub();else if(r==REQ)Toast.makeText(this,"Mikrofon izni olmadan dublaj yapılamaz.",Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){try{unregisterReceiver(receiver);}catch(Exception ignored){}super.onDestroy();}
}
