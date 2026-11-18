package com.gtaturkcedublaj;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Locale;

public class DubService extends Service implements TextToSpeech.OnInitListener {
    public static final String ACTION_UPDATE="com.gtaturkcedublaj.UPDATE";
    private static final String CHANNEL="gta_dub_channel";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private Translator translator;
    private TextToSpeech tts;
    private boolean running=false, translating=false, ttsReady=false, modelReady=false;
    private final ArrayDeque<String> translationQueue=new ArrayDeque<>();
    private String lastSpoken="";
    private long lastSpeechAt=0, lastFinalAt=0;

    @Override public void onCreate(){super.onCreate();createChannel();tts=new TextToSpeech(this,this);
        TranslatorOptions options=new TranslatorOptions.Builder().setSourceLanguage("en").setTargetLanguage("tr").build();translator= Translation.getClient(options);
        translator.downloadModelIfNeeded().addOnSuccessListener(v->{modelReady=true;send("Çeviri modeli hazır · Dinleme başlatılıyor",null,null,-1);pumpTranslations();}).addOnFailureListener(e->send("Çeviri modeli indirilemedi: internet bağlantını kontrol et",null,null,-1));
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(!running){running=true;startForeground(7,notification("Mikrofon dinlemede"));startRecognition();}
        return START_STICKY;
    }
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"GTA Türkçe Dublaj",NotificationManager.IMPORTANCE_LOW);c.setDescription("Oyun sesi çevirisi sırasında mikrofon durumu");getSystemService(NotificationManager.class).createNotificationChannel(c);}}
    private Notification notification(String text){return new NotificationCompat.Builder(this,CHANNEL).setContentTitle("GTA Türkçe Dublaj").setContentText(text).setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).build();}
    private void startRecognition(){
        if(!running)return;
        try{
            if(!SpeechRecognizer.isRecognitionAvailable(this)){send("Hata: Android konuşma tanıma hizmeti bulunamadı",null,null,-1);scheduleRestart(3000);return;}
            if(recognizer!=null){try{recognizer.destroy();}catch(Exception ignored){}}
            if(Build.VERSION.SDK_INT>=31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            else recognizer=SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener(){
                @Override public void onReadyForSpeech(Bundle p){send("● Dinleniyor · Mikrofon açık",null,null,-1);}
                @Override public void onBeginningOfSpeech(){lastSpeechAt=System.currentTimeMillis();}
                @Override public void onRmsChanged(float r){}
                @Override public void onBufferReceived(byte[] b){}
                @Override public void onEndOfSpeech(){send("Konuşma işleniyor…",null,null,-1);}
                @Override public void onError(int e){if(running){send(errorText(e),null,null,-1);scheduleRestart(e==SpeechRecognizer.ERROR_RECOGNIZER_BUSY?1200:700);}}
                @Override public void onResults(Bundle b){ArrayList<String> a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())process(a.get(0));scheduleRestart(450);}
                @Override public void onPartialResults(Bundle b){ArrayList<String>a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())send("● Konuşma algılanıyor",a.get(0),null,-1);}
                @Override public void onEvent(int t,Bundle p){}
            });
            Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en-US");i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,650);i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,400);recognizer.startListening(i);
        }catch(Exception e){send("Konuşma tanıma başlatılamadı: "+e.getMessage(),null,null,-1);scheduleRestart(2000);}
    }
    private void scheduleRestart(long delay){handler.removeCallbacks(restart);handler.postDelayed(restart,delay);}
    private final Runnable restart=()->{if(running)startRecognition();};
    private void process(String raw){
        if(raw==null)return;String text=raw.trim().replaceAll("\\s+"," ");if(text.length()<2)return;
        String normalized=text.toLowerCase(Locale.US);long now=System.currentTimeMillis();if(normalized.equals(lastSpoken)&&now-lastFinalAt<8000)return;if(normalized.length()<3)return;lastSpoken=normalized;lastFinalAt=now;
        send("Çeviri kuyruğuna eklendi",text,null,-1);translationQueue.addLast(text);pumpTranslations();
    }
    private void pumpTranslations(){
        if(!modelReady||translating||translationQueue.isEmpty()||!running)return;
        translating=true;String text=translationQueue.removeFirst();long started=System.currentTimeMillis();
        translator.translate(text).addOnSuccessListener(tr->{translating=false;send("● Çeviri hazır",text,tr,System.currentTimeMillis()-started);speak(tr);pumpTranslations();}).addOnFailureListener(e->{translating=false;send("Çeviri başarısız: "+e.getLocalizedMessage(),text,null,-1);pumpTranslations();});
    }
    private void speak(String text){if(!ttsReady||text==null||text.trim().isEmpty())return;
        float rate=getSharedPreferences("settings",MODE_PRIVATE).getFloat("speed",1.0f);float vol=getSharedPreferences("settings",MODE_PRIVATE).getFloat("volume",1.0f);
        tts.setSpeechRate(Math.max(0.5f,Math.min(2.0f,rate)));Bundle params=new Bundle();params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,Math.max(0f,Math.min(1f,vol)));tts.speak(text,TextToSpeech.QUEUE_FLUSH,params,"gta-"+System.currentTimeMillis());
    }
    @Override public void onInit(int status){if(status==TextToSpeech.SUCCESS){int r=tts.setLanguage(new Locale("tr","TR"));ttsReady=r!=TextToSpeech.LANG_MISSING_DATA&&r!=TextToSpeech.LANG_NOT_SUPPORTED;send(ttsReady?"Türkçe ses hazır":"Türkçe TTS sesi cihazda bulunamadı",null,null,-1);}}
    private String errorText(int e){switch(e){case SpeechRecognizer.ERROR_AUDIO:return "Mikrofon ses hatası";case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:return "Mikrofon izni verilmedi";case SpeechRecognizer.ERROR_NETWORK:case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:return "Konuşma tanıma için internet/hizmet hatası";case SpeechRecognizer.ERROR_NO_MATCH:return "Konuşma anlaşılamadı · yeniden dinleniyor";case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:return "Tanıma meşgul · yeniden deneniyor";case SpeechRecognizer.ERROR_SERVER:return "Konuşma tanıma sunucusu yanıt vermedi";default:return "Dinleme yeniden başlatılıyor (kod "+e+")";}}
    private void send(String s,String en,String tr,long latency){Intent i=new Intent(ACTION_UPDATE);i.setPackage(getPackageName());i.putExtra("status",s);if(en!=null)i.putExtra("english",en);if(tr!=null)i.putExtra("turkish",tr);if(latency>=0)i.putExtra("latency",latency);sendBroadcast(i);}
    @Override public void onDestroy(){running=false;translationQueue.clear();handler.removeCallbacksAndMessages(null);if(recognizer!=null){try{recognizer.cancel();recognizer.destroy();}catch(Exception ignored){}}if(translator!=null)translator.close();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
    @Nullable @Override public IBinder onBind(Intent i){return null;}
}
