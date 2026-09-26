package com.semtinazizi.kitapduy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

enum class LegalDocument(val title: String) {
    PRIVACY("Gizlilik Politikası"), TERMS("Kullanım Koşulları"), COMMUNITY("Topluluk Kuralları")
}

object LegalContent {
    const val supportEmail = "samatyayazilim@gmail.com"
    val privacy = """
        KitapDuy — iOS Gizlilik Politikası
        Güncelleme: 25 Eylül 2026

        İletişim: samatyayazilim@gmail.com

        Hesap ve profil
        E-posta ile kayıt olduğunuzda e-posta adresiniz ve seçtiğiniz profil bilgileri hesap oluşturma, giriş ve profil özellikleri için işlenir. Google veya Apple ile girişte, izin verdiğiniz hesap kimliği ve sağlayıcının paylaştığı ad/e-posta bilgileri alınır. Apple'ın e-postayı gizleme seçeneği kullanılabilir. Misafir girişinde de hizmetin çalışması için bir hesap kimliği oluşturulur.

        Paylaşımlar ve kullanım
        Yayınladığınız gönderi, yorum, görsel, anket ve herkese açık sohbet mesajları diğer kullanıcılara görünebilir. Özel sohbet içerikleri ilgili katılımcılara yöneliktir. Profil, kitaplık, takip, dinleme ilerlemesi ve tercihler, ilgili uygulama özelliklerini sağlamak için kullanılır. Şikâyet ve engelleme kayıtları topluluk güvenliği için işlenir. Paylaşımlarınıza hassas kişisel bilgi eklemeyin.

        Hizmet sağlayıcılar
        Hesap ve veritabanı işlemlerinde Supabase, içerik ve dosya altyapısında Cloudflare kullanılır. Google/Apple ile giriş tercih edildiğinde ilgili sağlayıcının hizmeti açılır. Abonelik işlemlerini Apple yürütür; ödeme kartı bilgilerinizi KitapDuy almaz. Uygulama, Apple'ın doğruladığı abonelik durumunu Premium erişimi sağlamak için kullanır. Dış içerik bağlantıları açıldığında ilgili sitenin kendi gizlilik koşulları geçerlidir.

        Bildirimler
        Bildirim hizmetinde OneSignal kullanılır. Uygulama, oturumdaki hesap kimliğini kullanıcıya özel bildirimler için OneSignal'a iletir; bildirim aboneliği için cihaz/uygulama bilgileri ve bildirim belirteci de işlenir. Hesaptan çıkınca bu cihazın OneSignal hesap bağlantısı kaldırılır. Bildirimleri uygulamanın Ayarlar > Bildirimler bölümünden veya iOS Ayarlar uygulamasından yönetebilirsiniz.

        iOS reklamları
        Reklam sunumu için Google AdMob ve izin tercihleri için Google User Messaging Platform kullanılır. Reklam hizmeti ağ bilgileri (örneğin IP adresi), cihaz/uygulama bilgileri, reklam etkileşimleri ve tanılama verilerini işleyebilir. Bu sürüm kişiselleştirilmemiş reklam ister ve IDFA erişim izni istemez. Gereken bölgelerde izin formu gösterilir; kullanılabilir olduğunda Ayarlar > Reklam gizliliği tercihleri bölümünden seçiminizi değiştirebilirsiniz.

        Cihaz izinleri
        Fotoğraf veya kamera erişimi, ilgili özelliği kullanmayı seçtiğinizde ve cihazda izin verdiğiniz ölçüde kullanılır. Bu izinleri iOS Ayarlar uygulamasından değiştirebilirsiniz. İndirilen içerikler ve bazı tercihler cihazda tutulur.

        Hesap silme ve talepler
        Profil > Ayarlar > Hesabı Sil bölümünden hesabınızın silinmesini başlatabilirsiniz. İşlem tamamlandığında uygulama bunu açıkça bildirir; hata durumunda hesabınız silinmiş gibi gösterilmez. Hesabın silinmesi App Store aboneliğini iptal etmez; aboneliğinizi ayrıca Apple hesap ayarlarından yönetmelisiniz. Veri erişimi, düzeltme veya silme talepleri için destek adresine yazabilirsiniz.

        Saklama
        Hesap ve içerik verileri, hizmetin sunulması ve kullanıcı taleplerinin işlenmesi için tutulur. Yasal olarak saklanması gereken kayıtlar, güvenlik kayıtları ve hizmet sağlayıcıların yedekleme süreçleri ayrı saklama sürelerine tabi olabilir. Başka kullanıcıların daha önce aldığı kopyalar KitapDuy tarafından geri çağrılamaz.
    """.trimIndent()

    val terms = """
        KitapDuy — Kullanım Koşulları
        Güncelleme: 6 Eylül 2026

        KitapDuy kitap, sesli içerik ve topluluk özellikleri sunar. Destek: samatyayazilim@gmail.com.

        Yalnızca paylaşma hakkına sahip olduğunuz içerikleri yükleyin. Başkasının telif, gizlilik veya kişilik haklarını ihlal eden içerikler paylaşmayın. Tehdit, taciz, nefret söylemi, dolandırıcılık, spam ve hukuka aykırı içerik yasaktır. Şikâyet edilen veya kuralları ihlal eden içerikler incelenebilir, kaldırılabilir ve ilgili hesaplara kısıtlama uygulanabilir.

        Abonelikler
        Satın almadan önce ürün adı, dönem ve mağazanın güncel fiyatı gösterilir. Ödemeler App Store üzerinden gerçekleşir. Otomatik yenilenen abonelikler iptal edilene kadar yenilenir. İptal, iade ve abonelik yönetimi Apple'ın ilgili hesap ve destek kanallarından yapılır. Aynı mağaza hesabındaki uygun satın alımlar, uygulamadaki Satın Alımları Geri Yükle düğmesiyle geri yüklenebilir. Hesap silmek aboneliği kendiliğinden iptal etmez.

        Harici içerikler
        Dış kaynak bağlantılarının içerikleri ve koşulları ilgili yayıncıya aittir. İçerikleri yeniden dağıtma hakkı verilmiş sayılmaz. Üçüncü taraf içeriklerin kullanılabilirliği değişebilir.

        Güvenlik ve destek
        Şifrenizi ve doğrulama kodlarınızı paylaşmayın. Sorunlu içerikleri uygulamadaki şikâyet işleviyle bildirin; rahatsız eden kullanıcıları engelleyebilirsiniz. Hesap, veri ve içerik haklarıyla ilgili taleplerinizi destek adresine iletin. Bu koşullar tüketicilerin kanunen sahip olduğu hakları ortadan kaldırmaz.
    """.trimIndent()

    val community = """
        Toplulukta saygılı olun. Tehdit, taciz, nefret söylemi, müstehcen veya hukuka aykırı içerik, kişisel bilgilerin izinsiz paylaşılması, spam ve telif ihlali yasaktır.

        Bir gönderi veya mesajın menüsünden Şikâyet Et seçeneğini kullanabilirsiniz. Şikâyetler inceleme için kaydedilir. Kullanıcıyı Engelle seçeneği, ilgili kullanıcıyla içerik ve iletişim etkileşimini sınırlar. Engellerinizi Ayarlar'dan yönetebilirsiniz.

        Görsel veya ses içeren yeni paylaşımlar yayınlanmadan önce incelemeye alınabilir. Kuralları ihlal eden içerikler kaldırılabilir. Destek: samatyayazilim@gmail.com.
    """.trimIndent()
}

data class LegalScreen(val document: LegalDocument) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current
        var contactError by remember { mutableStateOf(false) }
        Scaffold(topBar = {
            TopAppBar(title = { Text(document.title) }, navigationIcon = {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                }
            })
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(when (document) {
                    LegalDocument.PRIVACY -> LegalContent.privacy
                    LegalDocument.TERMS -> LegalContent.terms
                    LegalDocument.COMMUNITY -> LegalContent.community
                })
                TextButton(onClick = {
                    try { uriHandler.openUri("mailto:${LegalContent.supportEmail}") }
                    catch (error: Exception) { contactError = true }
                }) { Text("Destek: ${LegalContent.supportEmail}") }
                if (contactError) Text("E-posta uygulaması açılamadı. Bu adrese kendi e-posta uygulamanızdan yazabilirsiniz.")
            }
        }
    }
}
