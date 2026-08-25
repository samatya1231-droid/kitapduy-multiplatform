# Karşılama Ekranında Misafir Girişi Sorununun Giderilmesi

Bu plan, `WelcomeScreen` üzerindeki "Keşfetmeye Başla" butonunun çalışmama sorununu gidermeyi amaçlamaktadır. Mevcut durumda manuel olarak rastgele e-posta oluşturulmaya çalışılmaktadır, bu yöntem Supabase politikaları nedeniyle başarısız olmaktadır. Bunun yerine Supabase'in yerel anonim giriş özelliği kullanılacaktır.

## Yapılacak Değişiklikler

### [Supabase Entegrasyonu]

#### [MODIFY] [SupabaseAuthRepository.kt](file:///C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/data/repository/SupabaseAuthRepository.kt)
- `signInAnonymously` fonksiyonu Supabase'in native `auth.signInAnonymously()` metodunu kullanacak şekilde güncellenecek.
- Giriş sonrası `users` tablosuna misafir profilinin eklenmesi garanti altına alınacak.

### [Kullanıcı Arayüzü]

#### [MODIFY] [WelcomeScreen.kt](file:///C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/ui/screens/WelcomeScreen.kt)
- Giriş işlemi başarısız olduğunda kullanıcıya bildirim verilmesi sağlanacak.
- Gereksiz e-posta simülasyon mantığı temizlenecek.

## Doğrulama Planı

### Manuel Doğrulama
- Uygulama başlatılacak.
- Karşılama ekranında "Misafir Olarak Keşfet" seçilecek.
- İsim girilecek veya boş bırakılacak.
- "Keşfetmeye Başla" butonuna basılarak ana ekrana yönlendirme yapıldığı doğrulanacak.
- Hata durumunda (örn. internet yoksa) kullanıcıya bilgi verildiği kontrol edilecek.