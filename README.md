# GTA Türkçe Dublaj (Android)

Kotlin tabanlı Android uygulama kaynak projesi. Hedef cihaz: Android tabletler (özellikle Galaxy Tab S9 FE+).

## Mevcut uygulama davranışı
- Tablet mikrofonundan televizyon hoparlöründeki İngilizce konuşmayı dinler.
- Android `SpeechRecognizer` ile İngilizce konuşma tanır; mümkünse cihaz üstü tanıyıcıyı seçer, yoksa cihazın varsayılan tanıma hizmetine düşer.
- ML Kit on-device Translation ile İngilizce → Türkçe çeviri modeli indirilip cihazda kullanılır.
- Türkçe seslendirmeyi cihazın kurulu Android TTS motoruyla yapar.
- Ön plan mikrofon servisi, durum bildirimi ve otomatik dinleme yeniden başlatma mekanizması içerir.
- Ses dosyası kaydetmez.

## Önemli sınırlar
Bu proje, bulut tabanlı premium STT veya doğal yapay zekâ TTS hizmeti içermez. Android SpeechRecognizer sağlayıcısı ses tanıma sırasında internet kullanabilir ve sesin cihaz dışına gönderilip gönderilmediği seçilen Android sağlayıcısına bağlıdır. ML Kit çeviri modeli indirildikten sonra çeviri cihaz üzerinde yapılır. TTS kalitesi tablette kurulu Türkçe ses motoruna bağlıdır; her cihazda aynı kalite garanti edilemez.

SpeechRecognizer sürekli akış için tasarlanmış gerçek bir streaming API değildir. Servis dinlemeyi yeniden başlatır; kısa kesintiler ve gecikme olabilir. Bu nedenle bu sürüm, talep edilen profesyonel düşük gecikmeli GTA dublajının tamamlanmış/cihazda doğrulanmış sürümü olarak değerlendirilmemelidir.

## Derleme
- Android Studio Koala veya daha yenisi
- JDK 17 veya 21
- Android SDK Platform 35
- Android Gradle Plugin 8.6.1

Projeyi Android Studio ile açın, Gradle senkronizasyonunu tamamlayın ve `Build > Build Bundle(s) / APK(s) > Build APK(s)` seçin. APK genellikle `app/build/outputs/apk/debug/app-debug.apk` konumunda oluşur.

## Kurulum ve kullanım
1. APK'yı Galaxy Tab S9 FE+'ye yükleyin.
2. İlk açılışta mikrofon iznini verin.
3. İnternet açıkken ilk kullanımda İngilizce-Türkçe çeviri modelinin indirilmesini bekleyin.
4. TV sesini tablet mikrofonunun duyabileceği seviyede tutun.
5. DUBLAJI BAŞLAT'a basın; PS5 oyununu başlatın.
6. Durdurmak için DURDUR'a basın veya devam eden bildirimi sistemden kapatın.

Kulaklık kullanılması önerilir; aksi halde tablet hoparlöründen çıkan Türkçe ses tekrar mikrofona girerek yankı oluşturabilir.

## Test durumu
Bu geliştirme ortamında Android SDK/Gradle derleme aracı ve gerçek Galaxy Tab S9 FE+ bulunmadığından APK derlenemedi; uygulama gerçek cihazda test edilmedi. Mikrofon, STT, çeviri, TTS ve arka plan kararlılığı için gerçek cihaz testi gereklidir. Kaynak kodu bu sınırlamalar saklanmadan teslim edilmektedir.
