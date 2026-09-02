package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.model.QuizQuestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.QuizMultiplayerRepository
import com.semtinazizi.kitapduy.data.model.QuizLeaderboardEntry
import com.semtinazizi.kitapduy.data.model.QuizMatch
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class QuizGameState {
    STARTING, PLAYING, GAME_OVER, SUCCESS, LEADERBOARD, MULTIPLAYER_LOBBY
}

class QuizScreenModel(private val authRepo: AuthRepository, private val multiplayerRepo: QuizMultiplayerRepository) : ScreenModel {

    private val allQuestions = listOf(
        com.semtinazizi.kitapduy.model.QuizQuestion("02iJNJdZ1lSftMkq4MUi", "Rousseau'nun 'Émile' eseri hangi alana katkıda bulunur?", listOf("Eğitim felsefesi", "Savaş stratejileri", "Ekonomi teorisi", "Astronomi"), 0, "Bireyin doğal gelişimi ve eğitim tartışılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("0RfNZNlRlc8uLySh47p7", "Hangi roman Orhan Pamuk'un Nobel sonrası en bilinen eserlerinden biridir?", listOf("Sessiz Ev", "Kar", "Masumiyet Müzesi", "Benim Adım Kırmızı"), 3, "Osmanlı minyatür sanatı etrafında geçen tarihi kurgu."),
        com.semtinazizi.kitapduy.model.QuizQuestion("0Z904noF231rd12rHdPj", "Peyami Safa'nın 'Dokuzuncu Hariciye Koğuşu' romanı neyi anlatır?", listOf("Hastalık ve psikoloji", "Savaş", "Siyaset", "Macera"), 0, "Otobiyografik unsurlar içerir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("0cT5ujsBjjaT40Uig7s7", "Satirik şiir neyi eleştirir?", listOf("Toplumsal aksaklıkları", "Doğayı", "Aşkı", "Bilimi"), 0, "Hiciv içerir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("0gatBWF8qq1xGRHyZ0JI", "Kant'a göre ahlakın temeli nedir?", listOf("Görev", "Mutluluk", "Haz", "Güç"), 0, "Kategorik imperatif."),
        com.semtinazizi.kitapduy.model.QuizQuestion("0vpbqzbiPS6DGoIinv0h", "Naturalizm neyi temel alır?", listOf("Determinist doğa yasaları", "Hayal gücü", "Sanat", "Din"), 0, "Bilimsel yaklaşım."),
        com.semtinazizi.kitapduy.model.QuizQuestion("10gn4CDweuZRon8D0qDc", "Émile Zola'nın edebiyatta katkısı nedir?", listOf("Naturalizmi yaymak", "Romantizmi savunmak", "Symbolizmi kurmak", "Modernizmi reddetmek"), 0, "Doğalcı roman anlayışını öne çıkarır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("17L19HWp8EHlw3r4C0st", "Fransız yazar Marcel Proust'un yedi ciltten oluşan nehir romanının genel adı nedir?", listOf("İnsanlık Komedyası", "Kayıp Zamanın İzinde", "Sefiller", "Goriot Baba"), 1, "Madlen keki sahnesiyle meşhurdur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("19XjWQKzJLp9hCvOFp5Z", "Dramatik şiir hangi özelliği taşır?", listOf("Tiyatroya uygun olması", "Sadece doğa anlatımı", "Bilimsel olması", "Tarihi olması"), 0, "Sahneye uygundur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("1NcPs0klD3t4FIbw3JIU", "Hume hangi akımı temsil eder?", listOf("Empirizm", "Rasyonalizm", "İdealizm", "Stoacılık"), 0, "Deneyim temelli bilgi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("20Lhx9ga6zDGsjaOgyDX", "Nietzsche hangi felsefi akımla ilişkilidir?", listOf("Varoluşçuluk", "Empirizm", "Pozitivizm", "Stoacılık"), 0, "Birey ve değerler üzerine düşünür."),
        com.semtinazizi.kitapduy.model.QuizQuestion("2fIN2DfSziSYjcerunau", "Dante Alighieri'nin 'İlahi Komedya' adlı eserinde Dante'ye Cehennem ve Araf'ta rehberlik eden Latin şair kimdir?", listOf("Ovidius", "Horatius", "Vergilius", "Homeros"), 2, "Aeneas destanının da yazarıdır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("2kFFhm7BTd9rjas75zpO", "Nietzsche'nin 'Tanrı'nın ölümü' söylemi neyi ima eder?", listOf("Modern değer boşluğu ve yeni değer yaratma zorunluluğu", "Dinin fiziksel ölümü", "Sadece dinsel törenlerin azalması", "Doğa bilimlerinin çöküşü"), 0, "Değerlerin yeniden inşası gerekliliği vurgulanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("2kivKM4S3ehUa2B5Psly", "Büyülü gerçekçilik (magical realism) hangi coğrafi edebiyatla sık ilişkilendirilir?", listOf("Latin Amerika", "İskandinavya", "Japonya", "Afrika kuzeyi"), 0, "Günlük gerçeklik ile olağanüstü öğeler iç içe geçer."),
        com.semtinazizi.kitapduy.model.QuizQuestion("2w6GLWDXYLoTA1EXP47s", "Stoacılara göre erdem nedir?", listOf("Doğaya uygun yaşamak", "Zenginlik", "Güç", "Şöhret"), 0, "Akıl ön planda."),
        com.semtinazizi.kitapduy.model.QuizQuestion("3HpAJfwa4VCUjDvUT9rD", "Herder'in 'Volksgeist' kavramı neyi vurgular?", listOf("Ulusal ruh ve halk kültürünün önemini", "Küresel homojen kültürü", "Teknolojik ilerlemeyi", "Ekonomi politikalarını"), 0, "Dil ve folklor kimliği oluşturur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("3OnUH3QmSpESeZfhncJ0", "Dostoyevski'nin 'Karamazov Kardeşler'inde en merkezî tartışma hangisidir?", listOf("Tanrı, ahlak ve sorumluluk", "Ekonomi politikası", "Mimari estetik", "Doğa bilimleri"), 0, "İnanç ve ahlak meseleleri derinlemesine irdelenir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("3jLno9w6lXCekD1nmdWs", "Durkheim neyi inceler?", listOf("Toplumsal yapıyı", "Bireyi", "Sanatı", "Doğayı"), 0, "Sosyoloji."),
        com.semtinazizi.kitapduy.model.QuizQuestion("4JN0zV7CrpCAEoIdSjQJ", "Marx neyi savunur?", listOf("Sınıf mücadelesi", "Bireysel özgürlük", "Din", "Sanat"), 0, "Tarihsel materyalizm."),
        com.semtinazizi.kitapduy.model.QuizQuestion("4QX5hfg7obe4KZkgYKBg", "İdealizm neyi savunur?", listOf("Fikirlerin gerçek olduğunu", "Maddenin gerçek olduğunu", "Tesadüfü", "Kaosu"), 0, "Platon savunur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("50wMUIMhhXZqw9VKgX6d", "Liminal (eşiksel) durum edebi anlatıda neyi ifade eder?", listOf("Geçiş ve belirsizlik hâllerini", "Sabit son noktalarını", "Sadece başlangıçları", "Sadece pastoral sahneleri"), 0, "Kimlik ve ritüel geçişleriyle ilişkilidir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("54Ov4gzzORF84bIxC5rX", "Mythos ne demektir?", listOf("Mit ve efsane anlatımı", "Bilim", "Akıl", "Deney"), 0, "Sözlü anlatım."),
        com.semtinazizi.kitapduy.model.QuizQuestion("59fQ9si00Jn18i28DBot", "Ziya Gökalp hangi fikir akımıyla tanınır?", listOf("Türkçülük", "Batıcılık", "Osmanlıcılık", "İslamcılık"), 0, "Milli kimlik vurgusu vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("5NdtkxfpCSRKVuCLzyJo", "Divan edebiyatında 'gazel' hangi temaları işler?", listOf("Aşk ve güzellik", "Savaş", "Tarih", "Bilim"), 0, "Lirik ve aşkla ilgilidır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("5XtKcpD9PQwGaL0mBs1g", "Fyodor Dostoyevski en çok hangi türde eserlerle tanınır?", listOf("Psikolojik romanlar", "Komedi", "Bilimkurgu", "Masal"), 0, "İnsanın içsel çatışmalarını işler."),
        com.semtinazizi.kitapduy.model.QuizQuestion("617OyyNtCkXhaObzrb9M", "Kafiye nedir?", listOf("Ses benzerliği", "Anlam", "Ölçü", "Tema"), 0, "Uyak."),
        com.semtinazizi.kitapduy.model.QuizQuestion("69GaEaoa2nzuiuNrqXd9", "Fabl türünün özelliği nedir?", listOf("Hayvanların konuşturulması", "Gerçek olaylar", "Bilimsel anlatım", "Tarih"), 0, "La Fontaine örnektir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("6GWeHr6u15ZfYEp5ozCN", "Kant'a göre bilgi nedir?", listOf("Deney ve aklın birleşimi", "Sadece deney", "Sadece akıl", "Sezgi"), 0, "Eleştirel yaklaşım."),
        com.semtinazizi.kitapduy.model.QuizQuestion("6K2uoOKuTkEp7cJHX1Mg", "Montesquieu'nun en önemli katkısı nedir?", listOf("Güçler ayrılığı", "Monarşi", "Teokrasi", "Anarşi"), 0, "Modern devlet yapısı."),
        com.semtinazizi.kitapduy.model.QuizQuestion("6SPsi4hMyzq7f0zgRlna", "Rousseau hangi kavramı savunur?", listOf("Toplum sözleşmesi", "Mutlak monarşi", "Feodalizm", "Anarşi"), 0, "Genel irade."),
        com.semtinazizi.kitapduy.model.QuizQuestion("6Wdk6Xeahex2x2G5b97E", "Postmodernizm neyi reddeder?", listOf("Tek doğruyu", "Bilimi", "Sanatı", "Tarihi"), 0, "Çoğulculuk savunulur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("6ssQnqk9QxvpHo3swi6j", "Antik Yunan'da 'Bildiğim tek şey, hiçbir şey bilmediğimdir' diyerek sorgulayıcı felsefenin temellerini atan düşünür kimdir?", listOf("Aristoteles", "Epikuros", "Sokrates", "Diogenes"), 2, "Savunmasıyla da edebiyat ve felsefe tarihine geçmiştir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("6xWbd5FXSy5DssvlH8Zc", "Halit Ziya Uşaklıgil'in 'Aşk-ı Memnu' romanında Bihter'in annesinin adı nedir?", listOf("Peyker", "Firdevs Hanım", "Matmazel de Courton", "Şayeste"), 1, "Melih Bey Takımı'ndan olarak bilinir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("7Iqdb7Wtqf2RByJnYB2R", "Allegory (alegori) edebiyatta ne işe yarar?", listOf("Soyut fikirleri somut karakter ve olaylarla temsil etmek", "Sadece gerçekçi betimleme", "Sadece şiirsel imgeler", "Sadece diyalog kullanımı"), 0, "Daha geniş ahlaki veya politik mesaj iletir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("7YOweqDTQ7CobHZkiqVP", "Fabula ile syuzhet arasındaki fark nedir?", listOf("Fabula olayların mantıksal sırası; syuzhet anlatım sırasıdır", "Aynı şeydir", "Syuzhet sadece şiirde olur", "Fabula sadece tarihçedir"), 0, "Kurgu ve sunum arasındaki farkı belirtir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("7h1kkvlOo5HJGQikQ7cd", "Postmodernizm neyi reddeder?", listOf("Tek bir mutlak gerçeği", "Sanatı", "Bilimi", "Dili"), 0, "Çoğulculuk vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("7uV91SXAsRYJ6jxlxzKU", "Aristoteles'e göre bilgi nasıl elde edilir?", listOf("Gözlem ve deneyle", "Sadece akılla", "Sezgiyle", "İnançla"), 0, "Empirik yaklaşım."),
        com.semtinazizi.kitapduy.model.QuizQuestion("8BuS5kbJmLuGArVPFlXF", "Empirizm neyi savunur?", listOf("Bilginin deneyden geldiğini", "Doğuştan geldiğini", "Tanrısal olduğunu", "İmkansız olduğunu"), 0, "Deneycilik."),
        com.semtinazizi.kitapduy.model.QuizQuestion("8d4c9Ph7bKYwUBStrYEP", "Epik şiirin konusu nedir?", listOf("Kahramanlık", "Aşk", "Doğa", "Günlük hayat"), 0, "Destansı anlatım vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("8hpO64tB3AAGjvn2NV8j", "Halit Ziya Uşaklıgil hangi edebi topluluğa aittir?", listOf("Servet-i Fünun", "Tanzimat", "Fecr-i Ati", "Milli Edebiyat"), 0, "Modern Türk romanının öncülerindendir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("8pTCgQRK9BZhPN7MfLWA", "Monolog nedir?", listOf("Tek kişinin konuşması", "İki kişi konuşması", "Toplu konuşma", "Sessizlik"), 0, "Tiyatro terimi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("9G0MVcP5b3CNr9tdbaAg", "Orhan Veli Kanık hangi şiir akımının kurucularındandır?", listOf("Garip", "Sembolizm", "Realizm", "Naturalizm"), 0, "Sade ve günlük dili savunur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("9kM1d82ocpe4cZxIaQC3", "Romanın hikayeden farkı nedir?", listOf("Daha uzun ve detaylı olması", "Kısa olması", "Şiir olması", "Düzensiz olması"), 0, "Karakter gelişimi vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("A8RHblO4sziWyJce1fpu", "Halit Ziya Uşaklıgil hangi edebi topluluğa aittir?", listOf("Servet-i Fünun", "Tanzimat", "Fecr-i Ati", "Milli Edebiyat"), 0, "Modern Türk romanının öncülerindendir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("AR4H4j4LBANP4DzPZSBI", "Jean-Paul Sartre'ın 'varoluş özden önce gelir' sözü neyi belirtir?", listOf("İnsanın önce var olup sonra özünü seçmesi", "Özün varlıktan önce gelmesi", "Tanrısal belirlenmişlik", "Toplumsal rolün sabitliği"), 0, "Bireysel özgürlük ve sorumluluk vurgulanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("AXYgjitmiyX6es6INJGU", "Metafor edebiyatta hangi işlevi görür?", listOf("Bir şeyi başka bir şeyle ilişkilendirerek anlamı zenginleştirmek", "Sadece gerçek anlamı vermek", "Sadece sayısal veri sunmak", "Sadece diyalog oluşturmak"), 0, "İmgesel ilişkilendirme ile anlam çoğalır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("B1uZN1jD4ZNU6QGH47Xq", "Dualizm nedir?", listOf("İki temel varlık olduğunu savunur", "Tek varlık", "Hiçlik", "Madde"), 0, "Zihin-beden ayrımı."),
        com.semtinazizi.kitapduy.model.QuizQuestion("BPNhQBVobTh4ijmtQPXs", "Karacaoğlan hangi edebiyat türünün önemli temsilcisidir?", listOf("Halk şiiri", "Divan şiiri", "Roman", "Tiyatro"), 0, "Sade, doğrudan söyleyiş dikkat çeker."),
        com.semtinazizi.kitapduy.model.QuizQuestion("BPfMh58Upc9krAiJpZxP", "Tragedya nedir?", listOf("Acıklı tiyatro türü", "Komedi", "Roman", "Deneme"), 0, "Acı son."),
        com.semtinazizi.kitapduy.model.QuizQuestion("BW7gQkDfFfgjhxZobabr", "Epistemoloji neyi inceler?", listOf("Bilgi", "Varlık", "Ahlak", "Sanat"), 0, "Bilgi felsefesi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("Bajt9u7DaJ1EdKIELrEn", "Lacan'ın 'ayna evresi' kavramı neyi tanımlar?", listOf("Çocuğun benlik algısının oluştuğu dönem", "Bir edebi akımı", "Ekonomik gelişmeyi", "Felsefi argümanı"), 0, "Benlik tasarımının dış lansmanı üzerinde durur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("BrptOwJzOjEM93BKcxRH", "Brecht'in epik tiyatrosunun temel amacı nedir?", listOf("İzleyiciyi eleştirel düşünmeye sevk etmek", "Sadece eğlendirmek", "Duygusal içe çekmek", "Ritüel gösteri yapmak"), 0, "Yabancılaştırma teknikleriyle mesafe oluşturur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("C3jMisBxOTW5aPQo7KAZ", "Hegel'in diyalektiğinin üç aşaması genelde hangileridir?", listOf("Tez, antitez, sentez", "Giriş, gelişme, sonuç", "Olgu, yorum, sonuç", "Soru, cevap, tartışma"), 0, "Tarihsel ve düşünsel ilerlemeyi açıklar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("CW5chb6sxltZVK26nAo5", "Ahmet Haşim'in şiirleri genelde hangi akımla anılır?", listOf("Sembolizm", "Realizm", "Romantizm", "Modernizm"), 0, "İmge ve kapalılık ön plandadır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("CZOzLcbf5NoM0LAGd9Bi", "Shakespeare hangi ülke edebiyatına aittir?", listOf("İngiltere", "Fransa", "Almanya", "İtalya"), 0, "Dram yazarıdır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("CbYHHVHVKE9iPlCiamH2", "Recaizade Mahmut Ekrem hangi türde eserleriyle tanınır?", listOf("Roman ve şiir", "Sadece tiyatro", "Sadece deneme", "Hikaye"), 0, "Araba Sevdası önemli eseridir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("CelSkZP1rIR27P6GjzpC", "Kafka'nın eserlerinde sık görülen temel tema hangisidir?", listOf("Yabancılaşma ve bürokrasi", "Aşk ve romantizm", "Tarihsel destan", "Macera"), 0, "Absürt ve anlamsız bürokrasi öne çıkar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("Coeet6xRYXq3AojM9VX4", "Rasyonalizm neyi savunur?", listOf("Aklı", "Deneyi", "Sezgiyi", "Duyguyu"), 0, "Descartes temsilcidir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("D4SnmqO29RFolEXPbWOo", "Ziya Gökalp hangi fikir akımıyla tanınır?", listOf("Türkçülük", "Batıcılık", "Osmanlıcılık", "İslamcılık"), 0, "Milli kimlik vurgusu yapar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("DJZmgdn27g4CPjZCrCzW", "Etik neyi inceler?", listOf("Ahlak", "Bilgi", "Sanat", "Doğa"), 0, "Doğru-yanlış."),
        com.semtinazizi.kitapduy.model.QuizQuestion("DQdRAFgrxDwMPFmDZ9NG", "Satire (hiciv) edebiyatta ne amaçla kullanılır?", listOf("Toplumsal kusurları eleştirmek ve düzeltmeye çalışmak", "Saf eğlence üretmek", "Sadece aşkı yüceltmek", "Salt tarih anlatmak"), 0, "İğneleyici üslupla eleştiri sunar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("DqY0C4swJvx9u87XUvXS", "Örnek Soru 1?", listOf("A", "B", "C", "D"), 0, "İpucu 1"),
        com.semtinazizi.kitapduy.model.QuizQuestion("DvYh6YjpfqNmdL5tclEQ", "Ahmet Haşim hangi akımla ilişkilidir?", listOf("Sembolizm", "Realizm", "Naturalizm", "Romantizm"), 0, "Şiirde anlam kapalılığı vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("E9gzhpLqO3pZdM0hw1ja", "Aşık Veysel hangi tür eserleriyle bilinir?", listOf("Halk şiiri", "Roman", "Tiyatro", "Deneme"), 0, "Sazlı şiir geleneğinin önemli figürüdür."),
        com.semtinazizi.kitapduy.model.QuizQuestion("EBzeSpBqsFKv2WlZ7Okf", "Kafka'nın eserlerinde sık görülen tema nedir?", listOf("Yabancılaşma", "Aşk", "Savaş", "Macera"), 0, "Absürd durumlar içerir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("EJBWh7CjP5vXkBNgxawx", "Dostoyevski hangi ülkenin yazarıdır?", listOf("Rusya", "Fransa", "Almanya", "İtalya"), 0, "Psikolojik roman ustası."),
        com.semtinazizi.kitapduy.model.QuizQuestion("FQojRc9p493KMVrXTok3", "Victor Hugo'nun 'Sefiller' romanı hangi akıma aittir?", listOf("Romantizm", "Realizm", "Naturalizm", "Modernizm"), 0, "Toplumsal adaletsizlik teması."),
        com.semtinazizi.kitapduy.model.QuizQuestion("FXsQq8ur5PYM8EjAH3zi", "Determinizm ne demektir?", listOf("Her şeyin neden-sonuç ilişkisine bağlı olması", "Özgürlük", "Tesadüf", "Şans"), 0, "Zorunluluk ilkesi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("FgSQbgr62CMHk2p8bLT7", "Ateizm neyi savunur?", listOf("Tanrı'nın olmadığını", "Tanrı vardır", "Bilimsel şüphe", "İnanç"), 0, "Tanrı reddi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("FzYF38m9gzeT0nQuh9ua", "Türk edebiyatında ilk psikolojik roman olarak kabul edilen 'Eylül' kimin eseridir?", listOf("Mehmet Rauf", "Halit Ziya Uşaklıgil", "Namık Kemal", "Recaizade Mahmut Ekrem"), 0, "Suad, Süreyya ve Necip romanın karakterleridir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("GKUkTUP0GTleqpZThKMN", "Türkçe'de bildungsroman türüne verilebilecek bir örnek hangisidir?", listOf("Peyami Safa - Dokuzuncu Hariciye Koğuşu", "Orhan Pamuk - Benim Adım Kırmızı", "Ahmet Haşim - Bütün Şiirleri", "Nazım Hikmet - Şiirler"), 0, "Bireyin olgunlaşma sürecini anlatır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("GhOQwyt63FcTqIA6PRH5", "İngiliz edebiyatında 'Uğultulu Tepeler' (Wuthering Heights) romanının tek eseri olan yazar kimdir?", listOf("Jane Austen", "Charlotte Bronte", "Emily Bronte", "Virginia Woolf"), 2, "Romanın ana karakterleri Heathcliff ve Catherine'dir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("GmXI5vvbZKyg2HiEjiiJ", "Türk edebiyatında bilinç akışına örnek olan yazar kimdir?", listOf("Oğuz Atay", "Recaizade", "Namık Kemal", "Halit Ziya"), 0, "İç monolog ve zihin akışı deneyleri barındırır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("GnOmUDLMXauLY17Fndx5", "Ekphrasis hangi edebî olguyu tanımlar?", listOf("Bir görsel eserin sözel betimlenmesi", "Sadece roman kurulumu", "Tarihsel kronoloji", "Mimari plan"), 0, "Görsel sanatın kelimelerle yeniden inşasıdır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("Gppf2UWfQKA8U5ZY4epK", "Mary Shelley'nin 'Frankenstein' eseri hangi etik sorunu gündeme getirir?", listOf("Bilimsel sorumluluk ve yaratmanın sonuçları", "Sadece aşkı", "Savaş propagandasını", "Ekonomik sistemi"), 0, "Bilim ve etik arasındaki gerilim tartışılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("HBZuTU7Jrno75FGrrkaf", "Aristoteles'e göre mutluluk nedir?", listOf("Erdemli yaşam", "Zenginlik", "Güç", "Şöhret"), 0, "Eudaimonia kavramı."),
        com.semtinazizi.kitapduy.model.QuizQuestion("HZAdTqBkQyQjmPGK3CmL", "Metafiction (meta-kurgu) metinlerde ne yapar?", listOf("Kurgunun kendisini ve yazılma koşullarını konu edinir", "Sadece tarihi anlatır", "Matematiksel problem çözer", "Sadece doğal betimleme yapar"), 0, "Metin ile okur arasındaki sınırları oynar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("HmbtWFZ2fQw9psckBu48", "Hangi eser post-kolonyal ve büyülü gerçekçilik öğeleriyle bilinir:", listOf("Yüzyıllık Yalnızlık", "Suç ve Ceza", "Faust", "Tutunamayanlar"), 0, "Macondo, Buendía ailesi"),
        com.semtinazizi.kitapduy.model.QuizQuestion("Hng5w1xGnSPWvzgDXeET", "Stream of consciousness (bilinç akışı) tekniğini kimler yoğun kullandı?", listOf("James Joyce ve Virginia Woolf", "Charles Dickens ve Austen", "Tolstoy ve Dostoyevski", "Homer ve Vergilius"), 0, "Modernist akımın iç monolog deneyleridir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("HqTQz2mEEd6FreZhrElx", "Oğuz Atay'ın 'Tutunamayanlar' romanındaki ana karakterin adı nedir?", listOf("Selim Işık", "Turgut Özben", "Hikmet Benol", "C. (Cemil)"), 1, "Arkadaşı Selim'in intiharını araştıran mühendistir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("IIgP9CbctaiK05gY0gGK", "Camus hangi kavramla tanınır?", listOf("Absürd", "İdealizm", "Determinizm", "Pozitivizm"), 0, "Anlamsızlık teması."),
        com.semtinazizi.kitapduy.model.QuizQuestion("IdGAztPEXadyLTtD446v", "'İnsan insanın kurdudur' (Homo homini lupus) diyerek devletin mutlak gücünü savunan 'Leviathan' yazarı kimdir?", listOf("John Locke", "Thomas Hobbes", "Jean-Jacques Rousseau", "Machiavelli"), 1, "Doğa durumunun bir savaş hali olduğunu söyler."),
        com.semtinazizi.kitapduy.model.QuizQuestion("Iguls8jdoMl3SP6qLsk9", "Redif nedir?", listOf("Aynı görevdeki ek tekrarları", "Kafiye", "Ölçü", "Nazım"), 0, "Ek tekrar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("Ij9bXayXNqnv51ly98cj", "Proust'un anlatımında sık kullanılan teknik hangisidir?", listOf("Bellek yoluyla anımsama ve ayrıntılı betimleme", "Keskin diyaloglar", "Müzikal yapı", "Sadece şiir dili"), 0, "Duyusal anımsamalar anlatımı şekillendirir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("JE3sYynaIeoPnTpkpjn0", "Fyodor Dostoyevski'nin 'Suç ve Ceza' romanında Raskolnikov'un öldürdüğü tefeci kadının adı nedir?", listOf("Sonya", "Dunya", "Alyona İvanovna", "Katerina İvanovna"), 2, "Baltayla cinayeti işler."),
        com.semtinazizi.kitapduy.model.QuizQuestion("JJbwkYAdSmg2Q3KlNhyr", "George Orwell'in 'Hayvan Çiftliği' neyi eleştirir?", listOf("Totaliter rejimler", "Demokrasi", "Kapitalizm", "Bilim"), 0, "Siyasi alegori."),
        com.semtinazizi.kitapduy.model.QuizQuestion("JOP2Eadxu0GE6VrPPu8C", "Pascal'ın 'Pensees' eserinde temel tartışma nedir?", listOf("İnanç ile akıl arasındaki gerilim ve insanın yeri", "Sadece ekonomi politikası", "Madde bilimi", "Sadece edebiyat teorisi"), 0, "İnanç-akıl dengesi üzerinde durulur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("JU6Fqk72UZHKVpQ45rNh", "Asonans nedir?", listOf("Ünlü harf tekrarı", "Ünsüz tekrarı", "Kafiye", "Ölçü"), 0, "Ses tekrarları."),
        com.semtinazizi.kitapduy.model.QuizQuestion("JXqwRjVR4J4LpTfX6YCN", "Sembolizm akımında ne önemlidir?", listOf("İmgeler ve semboller", "Gerçekçilik", "Deney", "Toplum"), 0, "Kapalı anlatım vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("JYpTG6Fnx3GPpgVg4unu", "Dil felsefesinde çığır açan 'Tractatus Logico-Philosophicus' adlı eserin sahibi Avusturyalı filozof kimdir?", listOf("Ludwig Wittgenstein", "Martin Heidegger", "Karl Popper", "Edmund Husserl"), 0, "'Üzerine konuşulamayan konusunda susmalı' demiştir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("JkfhQhXi7ymTb0YKPd5o", "Karacaoğlan hangi edebiyat türünde eser vermiştir?", listOf("Halk edebiyatı", "Divan edebiyatı", "Modern edebiyat", "Tanzimat"), 0, "Sade dil kullanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("KDCESBUvojO3GzgMH0r8", "Komedya nedir?", listOf("Güldürücü tiyatro", "Trajedi", "Roman", "Şiir"), 0, "Mutlu son."),
        com.semtinazizi.kitapduy.model.QuizQuestion("KKtb2NhWC4UBL69xZgUa", "Pastiche terimi edebiyatta neyi ifade eder?", listOf("Farklı stillerin bilinçli olarak bir araya getirilmesi", "Tamamen özgün eser", "Sadece epik anlatı", "Sadece komik taklit"), 0, "Postmodern pratiklerde sık kullanılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("KLwAzsUdG6mTiMMN1gBa", "Rousseau'nun 'genel irade' kavramı siyaset felsefesinde neyi ifade eder?", listOf("Toplumun ortak çıkarını ifade eden irade", "Bireysel çıkarların toplamı", "Askeri strateji", "Ekonomik plan"), 0, "Siyasal meşruiyetin kaynağı halka dayanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("KqpJ2Go4ZqMi77yRRAgh", "'In medias res' terimi edebiyatta ne anlama gelir?", listOf("Olayların ortasından başlama", "Tüm geçmişi detaylandırma", "Sadece gelecekten bahsetme", "Sonsuz betimleme"), 0, "Anlatı doğrudan eylemin ortasına girer."),
        com.semtinazizi.kitapduy.model.QuizQuestion("KzH2WVfT4BvC543fWbGc", "Materyalizm neyi savunur?", listOf("Maddenin temel olduğunu", "Ruhu", "Tanrıyı", "İdeayı"), 0, "Maddecilik."),
        com.semtinazizi.kitapduy.model.QuizQuestion("L1EGYLmm1j6FVo85VMa5", "Nazım birimi nedir?", listOf("Şiirin yapı taşı", "Roman", "Hikaye", "Deneme"), 0, "Beyit, dörtlük."),
        com.semtinazizi.kitapduy.model.QuizQuestion("LBLtIyMnDre12ytoTGNA", "Platon'un öğrencisi kimdir?", listOf("Aristoteles", "Sokrates", "Descartes", "Nietzsche"), 0, "Lykeion kurucusu."),
        com.semtinazizi.kitapduy.model.QuizQuestion("LIGb8g7kOSjDWYdICNtH", "Oğuz Atay'ın 'Tutunamayanlar' romanı hangi akımla ilişkilidir?", listOf("Postmodernizm", "Realizm", "Romantizm", "Naturalizm"), 0, "Deneysel anlatım teknikleri içerir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("Le9nLUaOPtGVhID9l2dK", "Kant'ın 'Saf Aklın Eleştirisi' neyi ele alır?", listOf("Bilginin sınırlarını", "Ahlakı", "Sanatı", "Toplumu"), 0, "Epistemoloji."),
        com.semtinazizi.kitapduy.model.QuizQuestion("LpIoYMCRpj8ffo3dBS2q", "Bilinç akışı tekniği hangi dönemde yaygınlaşmıştır?", listOf("Modernizm", "Romantizm", "Klasik", "Ortaçağ"), 0, "20. yy başı."),
        com.semtinazizi.kitapduy.model.QuizQuestion("LphEqfwt7hqcNKBSA4XR", "Jung neyi savunur?", listOf("Kolektif bilinçdışı", "Bireysel bilinç", "Toplum", "Madde"), 0, "Arketipler."),
        com.semtinazizi.kitapduy.model.QuizQuestion("LuyyXPH9scjTXiZKnQzN", "Çehovvari atmosfer deyince genelde hangi unsur akla gelir?", listOf("Gündelik hayatın içindeki sessiz dramatik gerilim", "Epik savaş sahneleri", "Hızlı aksiyon", "Fantastik ögeler"), 0, "Sıradanlığın trajik yanlarına vurgu yapılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("M4HkUNAhkMBrjZxA355N", "Yakup Kadri Karaosmanoğlu'nun 'Yaban'ı hangi dönemi ve çatışmayı işler?", listOf("Milli Mücadele ve aydın-köylü çatışması", "Osmanlı yükselişi", "1980 sonrası", "Tanzimat reformları"), 0, "İçsel ve toplumsal yabancılaşma vurgulanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("M8glzd5vkNb2daPFzoHz", "Deizm neyi savunur?", listOf("Tanrı'nın evreni yaratıp müdahale etmediğini", "Tanrı yoktur", "Din vardır", "Bilim yoktur"), 0, "Yaratıcı ama müdahalesiz."),
        com.semtinazizi.kitapduy.model.QuizQuestion("MJ05QlPMSgZrHM7rKb9O", "Epigraph (önyazı alıntısı) metinde genelde ne işe yarar?", listOf("Eserin temasına veya tonuna işaret etmek", "Yazarın biyografisini sunmak", "Kitabın fiyatını belirtmek", "Sadece kapak resmi belirtmek"), 0, "Okura başlangıçta çerçeve sağlar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("MPgBSXfqExlkt4EbEXZx", "Karl Marx'a göre tarihsel gelişmenin itici gücü nedir?", listOf("Sınıf mücadelesi", "İnsan erdemi", "Dinsel dogmalar", "Yazarların etkisi"), 0, "Üretim ilişkileri ve sınıf çatışması belirleyicidir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("MaRonS7UHCrb8IfmloIO", "Namık Kemal hangi edebi akımın temsilcisidir?", listOf("Romantizm", "Realizm", "Naturalizm", "Sembolizm"), 0, "Vatan ve hürriyet teması öne çıkar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("MjZRbXyZkZelzjFI39Dl", "William Shakespeare hangi dönemde yazmıştır?", listOf("İngiliz Rönesansı / Elizabet dönemi", "Victoria dönemi", "Romantik dönem", "Georgian dönem"), 0, "Hamlet, Macbeth bu dönemde yazıldı."),
        com.semtinazizi.kitapduy.model.QuizQuestion("MmokpXFWNGZH0Jx2RmN2", "Metonimi nedir?", listOf("Ad aktarması", "Benzetme", "Abartı", "Karşıtlık"), 0, "Parça-bütün ilişkisi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("Nzx5JmUMf3v5wg3B3lQ5", "Immanuel Kant'ın 'kategorik imperatif'inin alanı nedir?", listOf("Ahlak felsefesi", "Estetik", "Siyaset bilimi", "Bilim kuramı"), 0, "Evrensel ahlak ilkelerini tanımlar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("O7pFHRtGGm6mM2JbqjpT", "Postkolonyal eleştiri genelde hangi konuları irdeler?", listOf("Sömürgecilik sonrası kimlik ve anlatı ilişkileri", "Sadece şiir formunu", "Matematiksel modelleri", "Fiziksel teorileri"), 0, "Kültürel güç dengeleri ve anlatıların yeniden okunması önemlidir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("O8bb7xHcD1LlbBwqY8ko", "Mehmet Akif Ersoy'un 'Safahat' eseri hangi türdedir?", listOf("Şiir", "Roman", "Deneme", "Tiyatro"), 0, "Toplumsal içerikli şiir kitabıdır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("OHYLcQLxxqCt90MVZOFC", "Mise-en-scène terimi hangi disipline aittir ve neyi ifade eder?", listOf("Tiyatro/film; sahneye koyma ve görsel düzen", "Sadece edebiyat", "Matematik", "Fizik"), 0, "Sahneleme, görsel düzen ve estetik unsurlarını kapsar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("OUizbEhvuLlELZmIP86R", "Hermeneutik neyi ifade eder?", listOf("Yorumlama sanatı", "Bilim", "Deney", "Gözlem"), 0, "Metin yorumlama."),
        com.semtinazizi.kitapduy.model.QuizQuestion("PDFuAtQ4JILxHIihJL7s", "Denis Diderot'nun en önemli katkısı nedir?", listOf("Ansiklopediyi düzenlemek ve Aydınlanma bilgisini yaymak", "Savaş stratejisi yazmak", "Tiyatro kurmak", "Tıp literatürü hazırlamak"), 0, "Bilginin organize edilip halka sunulması hedeflendi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("PIamCaa09H0LrlKjWush", "Sartre'a göre insan nedir?", listOf("Özgür bir varlık", "Belirlenmiş", "Pasif", "Makine"), 0, "Varoluş özden önce gelir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("QKiTTKPqBmacj0SklUS2", "Camus neyi savunur?", listOf("Absürd yaşam", "Determinism", "Pozitivizm", "İdealizm"), 0, "Anlamsızlık."),
        com.semtinazizi.kitapduy.model.QuizQuestion("QMe3oODPeNOhO9cRz9Nq", "Pozitivizm neyi savunur?", listOf("Bilimin tek geçerli bilgi olduğunu", "Dini", "Sanatı", "Sezgiyi"), 0, "Comte temsilcidir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("QT97CcS93DFZn7uTC7pA", "Logos ne demektir?", listOf("Akıl ve söz", "Duygu", "Madde", "Doğa"), 0, "Antik Yunan."),
        com.semtinazizi.kitapduy.model.QuizQuestion("QduLUku2ZaCi2gcBSad7", "Edebiyatta 'anti-kahraman' nedir?", listOf("Geleneksel kahramanlık özelliklerinden yoksun başkahraman", "Klasik kahraman", "Destansı lider", "Mitolojik varlık"), 0, "Kusurlu veya pasif karakterlerdir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("QodnMjquxhQwWrxKfA8E", "Nihilizm neyi savunur?", listOf("Hiçbir değerin olmadığını", "Ahlakı", "Toplumu", "Bilimi"), 0, "Değer reddi vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("R0TH5jd3yaODkRY4sRt0", "T.S. Eliot'un 'The Waste Land' şiiri hangi dönemin önemli örneğidir?", listOf("Modernizm", "Romantizm", "Realizm", "Klasisizm"), 0, "Parçalanmışlık ve mitoloji göndermeleri bulunur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("RBn39x0VQ1AcBpSbDovV", "Camus'nun 'Düşüş' romanında öne çıkan tema hangisidir?", listOf("Suç, vicdan ve modern bireyin yargılanması", "Savaş destanı", "Sadece aşk", "Tarih anlatısı"), 0, "İtiraf ve ahlaki sorgulama ön plandadır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("RVhwH08b7bJtvwoKHHPc", "Distopik edebiyatın başyapıtlarından 1984 romanında toplumu gözetleyen otoritenin adı nedir?", listOf("Yüce Lider", "Büyük Birader", "Otorite", "Gözcü"), 1, "Seni izliyor..."),
        com.semtinazizi.kitapduy.model.QuizQuestion("RhGF4duKTSuva4Py4ixq", "İlk edebi roman hangisidir?", listOf("İntibah", "Taaşşuk-ı Talat ve Fitnat", "Araba Sevdası", "Eylül"), 0, "Namık Kemal yazmıştır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("RljijIXHh5uKdsongl4c", "Beyit nedir?", listOf("İki mısralık birim", "Dört mısra", "Tek mısra", "Paragraf"), 0, "Divan şiiri."),
        com.semtinazizi.kitapduy.model.QuizQuestion("RmwdV5QtwyNuwLoNJ68f", "Novalis hangi edebi akımla ilişkilendirilir?", listOf("Alman Romantizmi", "Realizm", "Modernizm", "Naturalizm"), 0, "Mistik ve şiirsel romantik öğeler taşır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("RoL9YbbgjrBUdrRr7FgP", "Heidegger neyi inceler?", listOf("Varlığın anlamını", "Bilimi", "Sanatı", "Ekonomiyi"), 0, "Ontoloji."),
        com.semtinazizi.kitapduy.model.QuizQuestion("SGOvFzL544qf6rPP973d", "Çehov öykülerinde sık görülen özellik hangisidir?", listOf("Günlük hayattan küçük kesitler ve sessiz dramatik gerilim", "Büyük epik çatışmalar", "Bilimkurgu öğeleri", "Aksiyon yoğunluğu"), 0, "Sıradanlığın içindeki dramatik öğeler vurgulanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("SlrmWUV3yMYpJXHqANt4", "Søren Kierkegaard genelde hangi temayı işler?", listOf("Bireysel inanç ve seçim sorumluluğu", "Ekonomi politikası", "Dilbilim", "Mimarlık"), 0, "Bireysel varoluş ve inanç sorgulanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("TKmVQ41G2dnq1VZfKikg", "Foreshadowing (ön işaret) edebi cihazı ne işe yarar?", listOf("Gelecekte olacak olaylara dair ipucu vermek", "Sadece geçmişi anlatmak", "Karakterleri yok saymak", "Anlatım akışını bozmak"), 0, "Okurun beklentisini şekillendirir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("TP3MzHhjntRj6F6QWTBN", "George Orwell'in 'Hayvan Çiftliği' eseri neyi eleştirir?", listOf("Totaliter rejimleri ve devrimlerin yozlaşmasını", "Demokrasi uygulamasını", "Edebiyat eleştirisini", "Ekonomi teorilerini"), 0, "Siyasî alegori şeklindedir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("TQ0xoEc7MNvajhcfOEic", "Descartes hangi felsefi yöntemi kullanır?", listOf("Metodik şüphe", "Deney", "Gözlem", "Sezgi"), 0, "Kesin bilgi arayışı."),
        com.semtinazizi.kitapduy.model.QuizQuestion("TSM7p4SYEUyH4z7laE1m", "Recaizade Mahmut Ekrem'in ünlü eseri hangisidir?", listOf("Araba Sevdası", "Çalıkuşu", "Kuyucaklı Yusuf", "Kayıp Zaman"), 0, "Realist hiciv öğeleri barındırır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("Thr9jcYvJIASDrjtPcBi", "New Criticism ve F.R. Leavis tarzı eleştiri neyi vurgular?", listOf("Metin-içi analiz ve biçim-öz uyumunu", "Yazar biyografisini", "Toplumsal bağlamı", "Sadece popüler okuryazarlığı"), 0, "Metin merkezli eleştiri yaklaşımıdır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("TiMqy9rTNc4nIcGe0cbe", "Destan nedir?", listOf("Ulusun kahramanlık hikayesi", "Kısa şiir", "Makale", "Roman"), 0, "Sözlü gelenek ürünü."),
        com.semtinazizi.kitapduy.model.QuizQuestion("TqHQbHmabYr4zCtAOaE6", "Freud'a göre rüyaların işlevi nedir?", listOf("Bilinçdışındaki arzuların gizli biçimde ifadesi", "Salt rasgele beyin parazitleri", "Sadece fizyolojik atık", "Toplumsal olayların kopyası"), 0, "Rüyalar bilinçdışının dili olarak okunur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("UTp6OJefxwOqOR7lndMy", "Tolstoy'un 'Anna Karenina' romanı hangi temayı işler?", listOf("Aşk ve toplum", "Savaş", "Bilim", "Fantastik"), 0, "Toplumsal normlar eleştirilir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("UgqCF7Hg5PhN1FtZqnl3", "Egzistansiyalist felsefeyi Danimarka'dan başlatan ve 'Korku ve Titreme' adlı eserin sahibi olan düşünür kimdir?", listOf("Soren Kierkegaard", "Karl Jaspers", "Miguel de Unamuno", "Gabriel Marcel"), 0, "İnanç sıçraması kavramını ortaya atmıştır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("UoD0Hkq7uNcABfYkzBcQ", "Nonce word (uydurma kelime) edebiyatta neden kullanılır?", listOf("Yazarın özgün ifade ve etki yaratması için", "Dil kurallarına sıkı bağlılık için", "Sadece akademik yazılar için", "Resmi dil kullanımı için"), 0, "Dilde oyun ve yenilik sağlar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("VDbjy63vJ4YDQRjIJLS2", "Divan edebiyatında gazel neyi anlatır?", listOf("Aşk ve güzellik", "Savaş", "Bilim", "Tarih"), 0, "Lirik bir nazım şeklidir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("VNjGcxYENy3ynsTfFNoY", "Aruz ölçüsü nedir?", listOf("Uzun-kısa hece ölçüsü", "Hece sayısı", "Serbest", "Kafiye"), 0, "Divan şiiri."),
        com.semtinazizi.kitapduy.model.QuizQuestion("VbbXUFqsXfvIqELyQQOT", "Minyatür geleneği hangi kültür coğrafyasında güçlüdür?", listOf("İslam dünyası (Osmanlı-İran çevresi)", "Skandinavyalı Kuzey", "Güney Amerika", "Okyanusya"), 0, "Detaylı küçük resim geleneğiyle bilinir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("Vn55PDL3cQ9pUiDMydk9", "Albert Camus'ya göre 'absürd' nedir?", listOf("İnsanın anlam arayışı ile anlamsız dünya arasındaki çelişki", "Sadece trajik durum", "Gülünç bir komedi türü", "Siyasi bir ideoloji"), 0, "Anlam arayışı ve evren arasındaki uyumsuzluk anlatılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("WMsPNsEt6MycCT0JpHgQ", "Modernizm akımı neyi sorgular?", listOf("Gerçeklik ve bireyin iç dünyasını", "Sadece doğayı", "Savaşları", "Ekonomiyi"), 0, "İçsel çözümleme vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("WZKuadatbwFiE5D8jIUr", "Pathetic fallacy terimi neyi ifade eder?", listOf("Doğanın insan duygularını yansıtacak şekilde betimlenmesi", "Objektif betimleme", "Sadece tarihsel referans", "Teknik açıklama"), 0, "Hava ve doğa duyguları pekiştirmek için kullanılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("WgqXxX1HFD0audDvuWdX", "Şinasi'nin edebiyatımıza katkısı nedir?", listOf("İlk tiyatro eserini yazması", "İlk romanı yazması", "İlk hikayeyi yazması", "İlk şiiri yazması"), 0, "Şair Evlenmesi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("WlfD2UyGHsomw3kePula", "Orhan Kemal'in eserlerinde en çok işlenen tema nedir?", listOf("İşçi sınıfı ve toplum", "Mitoloji", "Fantastik dünya", "Bilim"), 0, "Toplumcu gerçekçi yazardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("WvCMWPYzwTDuNA1Wg7kB", "Agnostisizm ne demektir?", listOf("Tanrı'nın bilinemez olduğunu savunur", "Tanrı yoktur", "Tanrı vardır", "Bilim reddedilir"), 0, "Bilinemezcilik."),
        com.semtinazizi.kitapduy.model.QuizQuestion("X6FY02eqKNaaDR8PMCuV", "Locke'a göre insan zihni nasıldır?", listOf("Boş levha", "Dolu", "Sabit", "Değişmez"), 0, "Tabula rasa."),
        com.semtinazizi.kitapduy.model.QuizQuestion("XH7CWNhkrHE6d6ydywpg", "Nihilizm ve Üstinsan kavramlarıyla tanınan, 'Tanrı öldü' sözünün sahibi filozof kimdir?", listOf("Arthur Schopenhauer", "Friedrich Nietzsche", "Karl Marx", "Soren Kierkegaard"), 1, "Böyle Buyurdu Zerdüşt en bilinen eseridir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("XP9NNBBTk8haWEdXIQz7", "Yakup Kadri Karaosmanoğlu'nun 'Yaban' romanı hangi dönemi anlatır?", listOf("Milli Mücadele", "Osmanlı yükseliş", "Tanzimat", "Cumhuriyet sonrası"), 0, "Köy-aydın çatışması vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("XdWDXG20P8QM8oR6Ig9E", "Egzistansiyalizm neyi ele alır?", listOf("Bireyin varoluşunu", "Toplumu", "Doğayı", "Bilimi"), 0, "Varoluş felsefesi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("Y9k40YjM7DS6knTrcNFo", "Pragmatizm (Faydacılık) felsefesinin kurucularından kabul edilen ve 'Eğitim ve Demokrasi' eserini yazan filozof kimdir?", listOf("William James", "John Dewey", "Charles Sanders Peirce", "Bertrand Russell"), 1, "Eğitimi 'hayata hazırlık değil, hayatın ta kendisi' olarak görür."),
        com.semtinazizi.kitapduy.model.QuizQuestion("YLIQWuGuPZpXg1qtSCo2", "Bakhtin'in 'heteroglossia' kavramı neyi açıklar?", listOf("Metin içindeki farklı sosyal seslerin çoğulluğunu", "Tek bir dil kullanımı", "Sadece şiirsel sesleri", "Sadece tarihsel dili"), 0, "Farklı sesler metin içinde etkileşir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("YZ9YdMkjzZu7ooySZuVP", "Halide Edip Adıvar hangi dönemde eser vermiştir?", listOf("Milli Edebiyat", "Servet-i Fünun", "Divan", "Tanzimat"), 0, "Kurtuluş Savaşı'nı işlemiştir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("YjN8z6Ag2MSMm18K2FGJ", "'Unreliable narrator' (güvenilmez anlatıcı) ne demektir?", listOf("Anlatıcının verdiği bilgilerin güvenilmez olması", "Her şeyi bilen anlatıcı", "Tam tarafsız gözlemci", "Yalnızca diyalog anlatımı"), 0, "Okurun anlatılanları sorgulaması gerekir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("ZDb6m91pjpXR1V3Shrcm", "Camus'un 'Yabancı' romanındaki Meursault karakterinin tavrı nasıl özetlenir?", listOf("Dünya ile duygusal kopukluk ve kayıtsızlık", "Sürekli coşku", "Aşırı politizasyon", "Yoğun romantizm"), 0, "DUygusal mesafe ve kayıtsızlık dikkat çeker."),
        com.semtinazizi.kitapduy.model.QuizQuestion("ZEvNMHypEu3L9RUDsQyf", "Tevfik Fikret hangi topluluğun şairidir?", listOf("Servet-i Fünun", "Tanzimat", "Milli Edebiyat", "Garip"), 0, "Toplumsal eleştirileriyle bilinir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("ZI6vyPNasyEeBcm6Ooi7", "Zihin ve bedenin birbirinden ayrı iki töz olduğunu savunan 'Düalizm' (İkicilik) kuramını felsefeye yerleştiren kimdir?", listOf("Rene Descartes", "Baruch Spinoza", "Gottfried Leibniz", "David Hume"), 0, "Kozalaksı bezin bu ikisini bağladığını düşünmüştür."),
        com.semtinazizi.kitapduy.model.QuizQuestion("Zavf346VANs0mqfOIr1O", "Varoluşçuluk akımının edebiyattaki en önemli temsilcilerinden Jean-Paul Sartre'ın 'Bulantı' romanının ana karakteri kimdir?", listOf("Meursault", "Roquentin", "Joseph K.", "Gregor Samsa"), 1, "Tarihsel bir araştırma için Bouville kasabasına gelir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("ZdiUuiWa0pgunKdG6WBe", "Nietzsche neyi eleştirir?", listOf("Geleneksel ahlakı", "Bilimi", "Sanatı", "Doğayı"), 0, "Değer eleştirisi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("aErchmx7vI2I0wuiH3tF", "Ünlü 'Dönüşüm' öyküsünde bir sabah dev bir böceğe uyanan karakterin adı nedir?", listOf("Joseph K.", "Karl Rossmann", "Gregor Samsa", "Eduard"), 2, "Pazarlamacı olarak çalışmaktadır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("aLRw30bmW6rE1fGOfxVX", "Aydınlanma çağı felsefesinin en önemli isimlerinden Immanuel Kant'ın temel eseri hangisidir?", listOf("Saf Aklın Eleştirisi", "Leviathan", "Toplum Sözleşmesi", "Ütopya"), 0, "Kategorik imperatif kavramını burada işlemez ama epistemolojik temellerini atar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("ac8tFI4X1kRwlp9Ynfk5", "Sartre'ın 'Bulantı' romanı hangi temayı işler?", listOf("Varoluşun anlamsızlığı ve yabancılaşma", "Aşk ve romantizm", "Tarihsel epik", "Ekonomik teori"), 0, "Bireyin varlıkla kurduğu ilişki sorgulanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("ak7zZyna4L755OvybLDz", "Peyami Safa'nın 'Dokuzuncu Hariciye Koğuşu' romanı neyi işler?", listOf("Hastalık ve psikoloji", "Savaş", "Siyaset", "Macera"), 0, "Otobiyografik izler taşır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("atq6eiNZsiNDP0bFkQvx", "Freud neyi inceler?", listOf("Bilinçdışı", "Toplum", "Ekonomi", "Bilim"), 0, "Psikanaliz."),
        com.semtinazizi.kitapduy.model.QuizQuestion("b0VIKFmkblt8uCBBET8n", "Hedonizm neyi savunur?", listOf("Hazcılık", "Ahlak", "Bilim", "Din"), 0, "Epikür örnektir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("bI9Gxlj5mDENuUSu1RYX", "Adorno ve Horkheimer 'Aydınlanmanın Diyalektiği'nde neyi eleştirir?", listOf("Aydınlanmanın rasyonalite adı altında baskı üretebilmesi", "Sadece sanatın önemini", "Teknolojiyi tamamen reddetmeyi", "Edebiyatın sonunu"), 0, "Kültür endüstrisi ve rasyonalitenin karanlık yönleri irdelenir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("bJpbuTUh2r6myF29Gp02", "Amerikan edebiyatının başyapıtlarından 'Moby Dick' romanının anlatıcısı ve hayatta kalan tek denizcisi kimdir?", listOf("Kaptan Ahab", "Starbuck", "Queequeg", "Ishmael"), 3, "Roman 'Bana ... deyin' cümlesiyle başlar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("bXT4c2aQZRIgD62QRzFr", "Omniscient narrator (her şeyi bilen anlatıcı) hangi avantajı sağlar?", listOf("Tüm karakterlerin düşüncelerine hâkim anlatım sunar", "Sadece dışsal betimleme yapar", "Anlatıyı böl ve daraltır", "Sadece diyalog verir"), 0, "Geniş bakış açısı ve yorum imkânı tanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("baTfvEc0ptmtJj6W39iH", "Örnek Soru 1?", listOf("A", "B", "C", "D"), 0, "İpucu 1"),
        com.semtinazizi.kitapduy.model.QuizQuestion("bnGmnn5zBWvZ1s8Fao02", "Calque (kelime çevirisi) nedir?", listOf("Bir ifadeyi başka dile kelimesi kelimesine aktarma", "Tamamen yaratıcı bir çeviri", "Düzeltme işlemi", "Sadece argo aktarımı"), 0, "Kelime düzeyinde doğrudan çeviri sonucu ortaya çıkar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("c8DQh5IiH6wBWQJrsH3U", "Foucault hangi alanda çalışmıştır?", listOf("İktidar ve bilgi", "Fizik", "Kimya", "Biyoloji"), 0, "Toplumsal yapılar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("c9hh5veU70OuiXlITkSH", "Rainer Maria Rilke hangi tür şiirlerle bilinir?", listOf("Lirik ve varoluşsal şiirler", "Politik manifestolar", "Bilimsel makaleler", "Polisiye öyküler"), 0, "İçsel deneyim ve derin imgeler öne çıkar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("d2EOaaQW80epM0cJwbeP", "Homerik epitheton ornans (süsleyici sıfat) ne işe yarar?", listOf("Karakterin sabit niteliğini vurgulayıp ritim sağlar", "Karakter gelişimini engeller", "Sadece müzik için kullanılır", "Tarihi kronoloji kurar"), 0, "Örn: 'akıllı Odysseus' gibi tekrarlar ritm ve anlam üretir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("dWdrQiCi4SUVgjn5Plyo", "İlk Türk romanı hangisidir?", listOf("Taaşşuk-ı Talat ve Fitnat", "İntibah", "Araba Sevdası", "Mai ve Siyah"), 0, "Şemsettin Sami yazmıştır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("dxSNWYE9bmHWJTJ9YzZR", "Epikür'e göre mutluluğun yolu hangisidir?", listOf("Acıdan kaçınma ve dingin haz", "Güç elde etme", "Şöhret edinme", "Zenginlik toplama"), 0, "Haz dengesi ve acıdan kaçınma önemlidir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("eLbUV1RQP69LJzdX3Rux", "Dysphemism terimi dilde ne anlama gelir?", listOf("Olumsuz veya küçültücü ifade kullanımı", "Nazik ve yumuşatıcı dil", "Tarafsız dil kullanımı", "Sadece teknik terimler"), 0, "Olumsuz duygusal etki yaratır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("eLkPNEdj5arMJdB99Gbz", "Dilthey neyi savunur?", listOf("Beşeri bilimler ayrı incelenmelidir", "Bilim birdir", "Matematik önemlidir", "Doğa tek gerçek"), 0, "Yorumlama."),
        com.semtinazizi.kitapduy.model.QuizQuestion("eO3m4oN3SyPz9bVGExke", "Aristoteles'e göre trajedinin amacı nedir?", listOf("Katharsis ile acıma ve korku duygularını arındırmak", "Sadece eğlendirmek", "Bilgi vermek", "Siyasal propaganda"), 0, "Duygusal arınma (katharsis) hedeflenir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("eRd8g3X1UpPJnUT5iJfQ", "Serbest şiir nedir?", listOf("Ölçüsüz şiir", "Hece ölçülü", "Aruzlu", "Kafiyeli"), 0, "Kuralsızlık."),
        com.semtinazizi.kitapduy.model.QuizQuestion("eXIU4dS7KAa0cT2F5tRN", "Sartre'a göre insan nedir?", listOf("Özgür bir varlıktır", "Belirlenmiştir", "Makinedir", "Ruhsuzdur"), 0, "Özgürlük vurgusu."),
        com.semtinazizi.kitapduy.model.QuizQuestion("elHmIYOfKAZqCeBkby35", "Hikaye türünün kısa olması neyi sağlar?", listOf("Yoğun anlatım", "Detaysızlık", "Anlamsızlık", "Karışıklık"), 0, "Tek olay vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("eztQSIBOV12LrZI6vTg9", "Servet-i Fünun döneminde dilin özelliği nedir?", listOf("Ağır ve sanatlıdır", "Sade ve halk dilidir", "Argo ağırlıklıdır", "Tamamen yabancıdır"), 0, "Sanat için sanat anlayışı vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("f0d9kZvd5f4SULIHKCcE", "Teşbih nedir?", listOf("Benzetme", "Abartı", "İroni", "Karşıtlık"), 0, "Gibi kullanılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("f99CXAmjju8Cp3ra1bUe", "Lirik şiir neyi anlatır?", listOf("Duygular", "Savaş", "Tarih", "Bilim"), 0, "İçten anlatım vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("fsztN1YTDpDgx4PfVjKP", "Thomas Mann'ın 'Doktor Faustus' romanında hangi tema ön plandadır?", listOf("Sanat, etik ve toplumsal çöküş", "Sadece aşk ilişkileri", "Bilimsel keşif", "Spor yarışları"), 0, "Sanat ve toplum ilişkisi derinlemesine irdelenir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("fvmJhVM5JvTeJMOLs53O", "Ahmet Mithat Efendi'nin en belirgin özelliği nedir?", listOf("Halkı eğitici eserler yazması", "Sadece şiir yazması", "Bilimsel makale yazması", "Tiyatro yazmaması"), 0, "Halk için yazmıştır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("gCq4MX3yiUa6C8U6SDi4", "Felsefede 'İdealar Kuramı'nı ortaya atan ve mağara alegorisiyle bunu açıklayan düşünür kimdir?", listOf("Platon", "Sokrates", "Aristoteles", "Herakleitos"), 0, "Devlet adlı diyalog eseri ona aittir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("gKWf9Zing2xHdcY9Yg8f", "Victor Hugo'nun 'Sefiller' romanı hangi edebi akımı temsil eder?", listOf("Romantizm", "Realizm", "Naturalizm", "Modernizm"), 0, "Toplumsal adalet teması öne çıkar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("gYcf0elZoqt4VPzhGT2O", "Eylül romanı hangi türün ilk örneğidir?", listOf("Psikolojik roman", "Tarih romanı", "Macera romanı", "Bilimkurgu"), 0, "Mehmet Rauf yazmıştır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("gpMeE6dR7AsvGX5uDO2M", "Dörtlük nedir?", listOf("Dört mısralık birim", "İki mısra", "Tek mısra", "Roman"), 0, "Halk şiiri."),
        com.semtinazizi.kitapduy.model.QuizQuestion("gpYWYq7ZnDBDifms27zc", "Garip akımının temsilcilerinden biri kimdir?", listOf("Orhan Veli Kanık", "Nazım Hikmet", "Ahmet Haşim", "Necip Fazıl"), 0, "Sade dil kullanımı öne çıkar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("gwfQ9eJV0O5TcKx0bAir", "Realizm hangi özelliği savunur?", listOf("Gerçekçi tasvir", "Hayal gücü", "Abartı", "Mitoloji"), 0, "Gözleme dayanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("hS4ip9jxRmrA7W30nBNK", "Stoacılık neyi savunur?", listOf("Duygulara hakim olmayı", "Hazcılığı", "Kaosu", "Tesadüfü"), 0, "Erdemli yaşam."),
        com.semtinazizi.kitapduy.model.QuizQuestion("hX66aUpdALIBFVBRYckW", "Reşat Nuri Güntekin'in ünlü eseri hangisidir?", listOf("Çalıkuşu", "Yaban", "Sinekli Bakkal", "Ateşten Gömlek"), 0, "Feride karakteri vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("hgSv0f9A2qwxzmajjJmg", "Aristoteles'e göre 'eudaimonia' (mutluluk) nasıl elde edilir?", listOf("Erdemli yaşamla", "Zenginlikle", "Güçle", "Şöhretle"), 0, "Erdem ve alışkanlıkla bağdaştırılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("hrmvGzFIs9CJy1o1B3a1", "Yaşar Kemal'in ünlü 'İnce Memed' serisinde Memed'in başkaldırdığı zalim ağanın adı nedir?", listOf("Ali Safa Bey", "Abdi Ağa", "Hamza", "Durmuş Ali"), 1, "Dikenli Tarla köyünün ağasıdır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("ihGlDlV2QfweNvDs2ELB", "Oğuz Atay'ın 'Tutunamayanlar' romanı hangi akımla ilişkilidir?", listOf("Postmodernizm", "Realizm", "Romantizm", "Naturalizm"), 0, "Deneysel anlatım öne çıkar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("iqteUFNygW2xXqlRgubC", "Epikür'e göre mutluluk nedir?", listOf("Acıdan kaçınma", "Zenginlik", "Güç", "Şöhret"), 0, "Haz ama ölçülü."),
        com.semtinazizi.kitapduy.model.QuizQuestion("isNz2j1fKsDu1sXOmySX", "Psikanalizin kurucusu Sigmund Freud'un insan zihnini ayırdığı üç temel yapıdan, ilkel arzuları temsil eden hangisidir?", listOf("Ego", "Süperego", "İd", "Libido"), 2, "Tamamen haz ilkesiyle çalışır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("j80HcIq4pseBD0vnch4Z", "Mehmet Akif Ersoy'un 'Safahat' eseri hangi türdendir?", listOf("Şiir", "Roman", "Deneme", "Tiyatro"), 0, "Toplumsal şiirleri içerir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("jKlDirCleawuyGS2nmZ4", "Marx'a göre tarih neye dayanır?", listOf("Sınıf mücadelesi", "Din", "Ahlak", "Bilim"), 0, "Tarihsel materyalizm."),
        com.semtinazizi.kitapduy.model.QuizQuestion("jPPTsCmvjIXXNK8mNjJy", "Platon'un mağara alegorisi temel olarak neyi açıklar?", listOf("Algı ile gerçeklik arasındaki farkı", "Aşkın doğasını", "Savaş stratejilerini", "Ekonomik düzeni"), 0, "Bilginin doğası ve yanılsamalar irdelenir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("jcMevuSDQkSfuCDIdCP2", "Namık Kemal hangi edebi akımın temsilcisidir?", listOf("Romantizm", "Realizm", "Naturalizm", "Sembolizm"), 0, "Vatan ve hürriyet teması öne çıkar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("jy2ilxjgW0o8LFnJ4D0x", "'Bildungsroman' terimi ne anlama gelir?", listOf("Kahramanın olgunlaşma romanı", "Aksiyon macerası", "Bilimkurgu alt türü", "Ana karakterin ölümü anlatısı"), 0, "Kişilik ve eğitim gelişimi merkezlidir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("kA7Nnxco6itlG902s0mH", "Tevfik Fikret hangi edebi akımla ilişkilendirilir?", listOf("Servet-i Fünun", "Milli Edebiyat", "Garip", "Fecr-i Ati"), 0, "Toplumsal eleştiri ve lirik şiirler yazar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("kGDaRzAXuxtTzSdYmUhp", "Honoré de Balzac hangi akımla ilişkilendirilir?", listOf("Realizm", "Romantizm", "Sembolizm", "Sürrealizm"), 0, "Toplum betimlemeleriyle bilinir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("kdJ0aNksiUDVvlZx0z1T", "Teizm neyi savunur?", listOf("Tanrı'nın var olduğunu", "Tanrı yoktur", "Bilim", "Doğa"), 0, "Yaratıcı vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("kdoWb7FIQzNQF4DLfTyB", "Friedrich Nietzsche hangi kavramla sıkça anılır?", listOf("Üstinsan (Übermensch)", "Tabula rasa", "Kategorik imperatif", "Sosyal sözleşme"), 0, "Değerlerin yeniden değerlendirilmesi vurgulanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("lGQZ6ibwk6UQ66zofyMW", "Romantizm hangi duyguyu öne çıkarır?", listOf("Duygusallık", "Mantık", "Bilim", "Teknik"), 0, "Bireysellik ön planda."),
        com.semtinazizi.kitapduy.model.QuizQuestion("lLS1F886UfrqAoGb7mdk", "Aristoteles mantık alanında hangi katkıyla bilinir?", listOf("Kıyas (syllogism) ve sınıflandırma", "Deneysel yöntem", "Modern matematiksel mantık", "Bilişim teorisi"), 0, "Klasik mantığın temellerini atmıştır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("mi059cSRIAYNtG4KQsT0", "Örnek Soru 2?", listOf("A", "B", "C", "D"), 1, "İpucu 2"),
        com.semtinazizi.kitapduy.model.QuizQuestion("mrDEAVmHpDSoS3evPtSk", "Samuel Johnson hangi alanda önemli bir eser üretmiştir?", listOf("Sözlük ve edebi eleştiri", "Roman serisi", "Epik şiir", "Komedi oyunları"), 0, "Modern İngilizce sözlüğünün temeli sayılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("mu6U8ZvVo2DcAolHCEbk", "Tasavvuf felsefesinde 'Vahdet-i Vücut' (Varlığın Birliği) inancının en büyük temsilcisi olan ve 'Füsusu'l Hikem'i yazan düşünür kimdir?", listOf("Mevlana Celaleddin Rumi", "Hacı Bektaş Veli", "Muhyiddin İbnü'l-Arabi", "Yunus Emre"), 2, "Şeyh-i Ekber unvanıyla da bilinir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("mzQPjadfmyyF1caei4kH", "Emile Zola hangi akımı temsil eder?", listOf("Naturalizm", "Romantizm", "Realizm", "Modernizm"), 0, "Deneysel roman anlayışı."),
        com.semtinazizi.kitapduy.model.QuizQuestion("n5CfyQj1syu7kNy9N7u0", "Kürk Mantolu Madonna'da Raif Efendi'nin büyük aşkı olan Maria Puder'in mesleği nedir?", listOf("Hemşire", "Ressam", "Şarkıcı", "Öğretmen"), 1, "Kendi otoportresi romanın merkezinde yer alır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("nDq8Liryon3MJFP92XbI", "Open ending (açık son) edebiyatta hangi etkiyi yaratır?", listOf("Okura yorum yapma alanı bırakır", "Her şeyi kesin biçimde kapatır", "Sadece mutlu son verir", "Sadece trajik son sunar"), 0, "Belirsizlik ile okur katılımı artırılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("nrVMMxht2sWcbxSmPYL8", "Aşık Veysel hangi türde eserler vermiştir?", listOf("Halk şiiri", "Roman", "Tiyatro", "Deneme"), 0, "Saz eşliğinde şiirler söyler."),
        com.semtinazizi.kitapduy.model.QuizQuestion("o89Eg0vSE4N38fnmSwb1", "Heidegger hangi kavramı inceler?", listOf("Varlık", "Para", "Siyaset", "Sanat"), 0, "Ontoloji."),
        com.semtinazizi.kitapduy.model.QuizQuestion("oeRmv9toWwSnDEtqUou4", "Makale nedir?", listOf("Bilgi vermek amacıyla yazılan yazı", "Şiir", "Roman", "Masal"), 0, "Nesneldir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("omoNWN3xygfsvPWGj3a7", "Koşuk nedir?", listOf("Aşk ve doğa şiiri", "Tiyatro", "Roman", "Deneme"), 0, "Şölenlerde söylenir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("p4Owo2aniBlr6SOWQpzi", "Weber neyi inceler?", listOf("Toplum ve ekonomi ilişkisi", "Sadece din", "Sanat", "Doğa"), 0, "Anlamacı sosyoloji."),
        com.semtinazizi.kitapduy.model.QuizQuestion("pSEZSmMCFZmJuOxyVe1D", "Kinaye nedir?", listOf("Hem gerçek hem mecaz anlam", "Sadece gerçek", "Sadece mecaz", "Abartı"), 0, "İki anlamlılık."),
        com.semtinazizi.kitapduy.model.QuizQuestion("pXzU4MlZPrNhD4rsS9eM", "Ontoloji neyi inceler?", listOf("Varlık", "Bilgi", "Ahlak", "Sanat"), 0, "Varlık felsefesi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("pe2uFB4eJBXurqJKDskM", "Orhan Kemal'in eserlerinde en çok işlenen tema nedir?", listOf("İşçi sınıfı ve toplum", "Mitoloji", "Fantastik dünya", "Bilim"), 0, "Toplumcu gerçekçi bakış vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("peeZzFBDloPUidMWOgyc", "Walter Benjamin'in 'aura' kavramı neyi ifade eder?", listOf("Orijinal sanat eserinin benzersiz varlığını ve etkisini", "Tekrarlanabilirlik avantajını", "Sadece pazar değerini", "Sadece teknik üretimi"), 0, "Teknolojik kopyalama aurayı zayıflatır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("pzEBmwLqTO92RsTMETTW", "Descartes'in ünlü sözü nedir?", listOf("Düşünüyorum öyleyse varım", "Bilgi güçtür", "Varlık birdir", "İnsan özgürdür"), 0, "Cogito."),
        com.semtinazizi.kitapduy.model.QuizQuestion("qCi3Dv0IaEzbjwPGZBRe", "Mevlana Celaleddin Rumi hangi alanda eser vermiştir?", listOf("Tasavvuf ve ilahî aşk şiiri", "Politika", "Bilim", "Tarih"), 0, "Aşk ve birlik temaları ön plandadır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("qbIof4NPWtZtLXmlJtGN", "Gogol'un en bilinen kısa hikâyelerinden biri hangisidir?", listOf("Palto (The Overcoat)", "Savaş ve Barış", "Madame Bovary", "Suç ve Ceza"), 0, "Rus toplumsal hicvinin klasik örneğidir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("qhiirBNS86udFCvFWc0m", "Sokrates hangi yöntemi kullanır?", listOf("Soru-cevap", "Deney", "Gözlem", "Yazı"), 0, "Diyalog yöntemi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("qhjaPocNQwpxNX84tf6l", "Prosody (vezinbilim) neyi inceler?", listOf("Şiirde ölçü, ritim ve kafiye", "Romanın konusunu", "Dilin tarihini", "Sadece diyalogları"), 0, "Şiirin ses ve ritim yapısını ele alır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("qzAsu6xy20OHdVmlIwlI", "Albert Camus'nün 'Yabancı' romanının baş karakteri Meursault'nun cinayet işlediği yer neresidir?", listOf("Paris sokakları", "Cezayir'de bir plaj", "Marsilya limanı", "Karanlık bir ara sokak"), 1, "Güneşin kavurucu sıcağından etkilenmiştir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("qziHjiO9Fg3t9XCyWJC8", "Gabriel Garcia Marquez'in 'Yüzyıllık Yalnızlık' romanında olayların geçtiği kurgusal kasabanın adı nedir?", listOf("Comala", "Macondo", "Jefferson", "Yoknapatawpha"), 1, "Buendia ailesinin yedi nesli burada yaşar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("r3tbD5Jug9FAXs5dBhwf", "Mübalağa nedir?", listOf("Abartma", "Benzetme", "Karşıtlık", "İroni"), 0, "Aşırı anlatım."),
        com.semtinazizi.kitapduy.model.QuizQuestion("s5Hu8kYhs5NcslAQ833z", "Aristoteles'in 'Poetika' eseri neyi inceler?", listOf("Sanat ve tragedya", "Bilim", "Ekonomi", "Siyaset"), 0, "Trajedi analizi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("sBoWxqBtIHaXIu4xQQMm", "Sofistler neyi savunur?", listOf("Göreceli bilgi", "Kesin bilgi", "Tanrısal bilgi", "Bilimsel bilgi"), 0, "Relativizm."),
        com.semtinazizi.kitapduy.model.QuizQuestion("sBrpi8onHyFFjojN8mAB", "Naturalizm akımı hangi bilimden etkilenmiştir?", listOf("Biyoloji", "Fizik", "Kimya", "Astronomi"), 0, "Determinizm etkisi vardır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("sKutzAoglY0zUGZNVc2n", "Deneme türü nedir?", listOf("Yazarın kişisel görüşlerini anlattığı yazı", "Bilimsel yazı", "Tarih yazısı", "Hikaye"), 0, "Samimi dil kullanılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("sMqRWSwYEagMp9q0ieke", "Masalın özelliklerinden biri nedir?", listOf("Olağanüstü olaylar içerir", "Gerçek olaylara dayanır", "Bilimsel veriler içerir", "Belgeseldir"), 0, "Zaman belirsizdir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("sRCcyjdiRD6gcg22yPtO", "Estetik neyi inceler?", listOf("Güzellik ve sanat", "Bilgi", "Ahlak", "Bilim"), 0, "Sanat felsefesi."),
        com.semtinazizi.kitapduy.model.QuizQuestion("sRG1eHBqP37s9uzn6spk", "Klasik Türk Edebiyatı'nda (Divan Edebiyatı) 'Su Kasidesi' adlı ünlü eserin şairi kimdir?", listOf("Baki", "Nedim", "Fuzuli", "Şeyh Galip"), 2, "Aşk ve ıstırap şairi olarak bilinir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("serd5G2le0D0yP9SadGY", "Edebiyatta 'motif' terimi neyi tanımlar?", listOf("Tekrar eden simge veya tematik unsuru", "Sadece romanın sonunu", "Yazarın kişisel hayatını", "Sadece şiirde kullanılan kafiye"), 0, "Metnin anlamını pekiştiren ögeler tekrar edilir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("sleRIsvSJF9zbL8BeDRK", "Dramatik ironi (dramatic irony) izleyiciye ne sağlar?", listOf("İzleyici karakterden daha fazla bilgiye sahip olur", "Karakterlerin tümünü aynı anda anlamak", "Sadece komedi etkisi yaratmak", "Anlatıyı basitleştirmek"), 0, "Gerilim ve beklenti oluşturur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("t872hpF2OvRpHfIzNAyK", "J.R.R. Tolkien'in 'Yüzüklerin Efendisi'nde merkezî ırk hangisidir?", listOf("Hobbitler", "Kurganlar", "Amazonlar", "Vikingler"), 0, "Hobbitlerin yolculuğu anlatının merkezindedir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("tAh3MRvvefib1ohx0S4v", "Garip akımının amacı nedir?", listOf("Şiiri halkın anlayacağı hale getirmek", "Şiiri ağırlaştırmak", "Sadece seçkinlere hitap etmek", "Klasik şiiri sürdürmek"), 0, "Sade dil savunulur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("tIL1NJ5kCgtfRULVjFSc", "Anachronism (zaman uyumsuzluğu) edebiyatta ne demektir?", listOf("Tarihi bağlama uymayan ögenin kullanımı", "Tam kronolojik doğruluk", "Sadece fantezi öğesi", "Sadece şiirsel metafor"), 0, "Bilerek veya hata sonucu zaman uyumsuzlukları olur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("tLKjpg0wDBXI0xe3i8ZJ", "Platon'un 'Devlet' eserinde ne anlatılır?", listOf("İdeal devlet", "Aşk", "Savaş", "Bilim"), 0, "Adalet kavramı."),
        com.semtinazizi.kitapduy.model.QuizQuestion("tLOfIR3mFgmfCOzkmRbj", "Montaigne hangi türün öncüsüdür?", listOf("Kişisel deneme (essay)", "Epik roman", "Sürrealist şiir", "Polisiye öykü"), 0, "Kişisel düşüncenin edebi sunumu ön plandadır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("tMEe67CasHkLTx8ObhwX", "Romantizm akımı neyi ön plana çıkarır?", listOf("Duygu ve hayal gücünü", "Bilimi", "Mantığı", "Deneyi"), 0, "Sanatçı duygularını yansıtır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("tVxKNtTDg5MkFx5CkslH", "Intertextuality (metinlerarasılık) neyi tanımlar?", listOf("Metinlerin birbirine göndermede bulunması", "Yalnızca tek bir metinle sınırlı kalma", "Sadece çeviri teorisi", "Sadece şiire özgü bir olgu"), 0, "Metinler arası ilişkiler anlam üretimini zenginleştirir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("tbzGkCWFmLMjqhoswAzg", "Sembolizm hangi özelliğe sahiptir?", listOf("Kapalı anlatım", "Açık anlatım", "Bilimsel dil", "Resmi dil"), 0, "İmgesel anlatım."),
        com.semtinazizi.kitapduy.model.QuizQuestion("thDzhutJEAJv7RQYKlkA", "Balzac hangi edebi akımın temsilcisidir?", listOf("Realizm", "Romantizm", "Sembolizm", "Naturalizm"), 0, "Toplumsal gözlem yapar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("tjCa1cy42ahALYPzudwU", "Ironi nedir?", listOf("Söylenenin tersini ima etmek", "Doğruyu söylemek", "Abartmak", "Sade anlatım"), 0, "İnce alay."),
        com.semtinazizi.kitapduy.model.QuizQuestion("u9z1xaaihid6vbYyzFQz", "Antik epiklerde tanrı müdahaleleri genelde nasıl kullanılır?", listOf("Kader ve olayları yönlendiren etkenler olarak sıklıkla yer alır", "Hiç kullanılmaz", "Sadece yan unsurdur", "Anlatıdan hariç tutulur"), 0, "Tanrılar kahramanların kaderine müdahale eder."),
        com.semtinazizi.kitapduy.model.QuizQuestion("uDyedS4OSfd8kiErT9Uf", "Borges'in öykülerinde sık rastlanan motiflerden biri hangisidir?", listOf("Labirent ve sonsuzluk", "Savaş betimlemeleri", "Tarım yaşamı", "Spor müsabakaları"), 0, "Gerçeklik, kopya ve sonsuzluk sorgulanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("uZXUOPYMMo9w6osMP1Vh", "İslamiyet öncesi Türk edebiyatında 'sagu' nedir?", listOf("Ağıt", "Koşma", "Masal", "Destan"), 0, "Ölüm üzerine yazılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("uaPFfgwGsYT2hly70lds", "Realizm akımının temel özelliği nedir?", listOf("Gerçeği olduğu gibi yansıtmak", "Hayal gücüne dayanmak", "Duygusallık", "Abartı"), 0, "Gözlem önemlidir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("uqQeesbyia95S7o9Ak82", "Virginia Woolf'un 'Orlando' romanı hangi temaları işler?", listOf("Cinsiyet, kimlik ve zaman içinde dönüşüm", "Sadece savaş", "Ekonomi", "Tek mekan anlatısı"), 0, "Cinsiyet ve tarihsel süreklilik sorgulanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("uuc1tcLRzBbk1QGTP7iQ", "Camus'nun 'Sisifos Söyleni' nde Sisifos miti ne için kullanılır?", listOf("İnsanın anlamsız iş karşısında direnci ve anlam yaratma çabası", "Sadece mitoloji öğretmek", "Savaş anlatısı oluşturmak", "Ekonomi modeli sunmak"), 0, "Absürd durum karşısında insan direnci vurgulanır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("v2P0X4nCb0u2qU1nvtp5", "Milli Edebiyat döneminin temel ilkesi nedir?", listOf("Halka yönelmek ve sade dil kullanmak", "Ağır dil kullanmak", "Sadece saraya hitap etmek", "Yabancı dille yazmak"), 0, "Dil sadeleşmiştir."),
        com.semtinazizi.kitapduy.model.QuizQuestion("vGxCX5g7DnNtrU1UVugT", "Aliterasyon nedir?", listOf("Ses tekrarı", "Anlam tekrarı", "Kafiye", "Ölçü"), 0, "Ses uyumu."),
        com.semtinazizi.kitapduy.model.QuizQuestion("vjgUwEASI1ELaleE4VW7", "Hegel'in felsefesi hangi kavrama dayanır?", listOf("Diyalektik", "Deney", "Gözlem", "Sezgi"), 0, "Tez-antitez-sentez."),
        com.semtinazizi.kitapduy.model.QuizQuestion("vrfajLGbwxUr7hftUTG1", "Satir ile parodi arasındaki temel fark nedir?", listOf("Satir toplumsal kusuru düzeltmeye çalışır; parodi bir eseri taklit ederek güldürür", "İkisi tamamen aynıdır", "Parodi daha ciddi bir eleştiri yapar", "Satir sadece bireysel alaydır"), 0, "Amaç ve hedef farklıdır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("vwz1ggfjOuJ4pJztnXDv", "Cervantes'in ünlü eseri Don Kişot'un sadık yardımcısı ve seyisi kimdir?", listOf("Sancho Panza", "Rocinante", "Dulcinea", "Cardenio"), 0, "Gerçekçi ve pratik zekalı bir köylüdür."),
        com.semtinazizi.kitapduy.model.QuizQuestion("vz5mieISsr7b3divT0i0", "Norveçli yazar Jostein Gaarder'in felsefe tarihini bir roman kurgusu içinde anlattığı ünlü eseri hangisidir?", listOf("Gülün Adı", "Sofie'nin Dünyası", "Simyacı", "Böyle Buyurdu Zerdüşt"), 1, "Kitabın baş karakteri 15 yaşındaki bir kızdır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("wFoCmSmpW0T4rMx6UAXd", "Ahmet Hamdi Tanpınar'ın 'Saatleri Ayarlama Enstitüsü' romanında enstitünün kurucusu olan karakter kimdir?", listOf("Hayri İrdal", "Halit Ayarcı", "Nuri Efendi", "Seyit Lütfullah"), 1, "Modernleşmenin absürt yüzünü temsil eder."),
        com.semtinazizi.kitapduy.model.QuizQuestion("wVvEWJwG4YihZicM7qcg", "Tolstoy'un 'Anna Karenina' romanı hangi ana temayı işler?", listOf("Aşk ve toplumun yargısı", "Savaş", "Bilim", "Köy hayatı"), 0, "Toplumsal normlar ve birey çatışması mevcuttur."),
        com.semtinazizi.kitapduy.model.QuizQuestion("wbbBidyQIdecOREnJfOh", "John Stuart Mill'in özgürlük anlayışının temel ilkesi hangisidir?", listOf("Zarar vermeme ilkesi", "Toplumsal zorunluluk", "Dinî otorite", "Geleneksel emirler"), 0, "Bireysel özgürlüğün sınırını belirler."),
        com.semtinazizi.kitapduy.model.QuizQuestion("xYHXlKZpNWThkust0Y1g", "Michel Foucault hangi konularla çalışmıştır?", listOf("İktidar, bilgi ve kurumlar", "Astronomi", "Kimya", "Matematik"), 0, "Bilgi-iktidar ilişkileri üzerine analizler yapar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("yMsmu5ZspfqlIzNvFD35", "Divan edebiyatında 'mesnevi' nedir?", listOf("Uzun hikaye şiiri", "Kısa şiir", "Tiyatro", "Roman"), 0, "Beyitlerle yazılır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("z4Pybb7z7a7hzgrkL1VB", "Platon'a göre gerçek dünya nedir?", listOf("İdealar dünyası", "Duyular dünyası", "Madde", "Hiçlik"), 0, "Asıl gerçeklik ideadır."),
        com.semtinazizi.kitapduy.model.QuizQuestion("z9rlqvwdUPJ4LZWx7mK1", "Modernizm hangi özelliği taşır?", listOf("Deneysel anlatım", "Klasik yapı", "Basit hikaye", "Tek tema"), 0, "Yenilikçi teknikler."),
        com.semtinazizi.kitapduy.model.QuizQuestion("z9yZKVn6OQcay94PycTu", "Diyalog nedir?", listOf("Karşılıklı konuşma", "Tek konuşma", "Sessizlik", "Yazı"), 0, "İletişim."),
        com.semtinazizi.kitapduy.model.QuizQuestion("zS0wGBEYtfCpIwbT0uVo", "Örnek Soru 2?", listOf("A", "B", "C", "D"), 1, "İpucu 2"),
        com.semtinazizi.kitapduy.model.QuizQuestion("zcdcTD2muVN9OhwcL3Dw", "Hece ölçüsü nedir?", listOf("Hece sayısına göre ölçü", "Ses ölçüsü", "Serbest", "Aruz"), 0, "Halk şiiri."),
        com.semtinazizi.kitapduy.model.QuizQuestion("zjQV05S0eJaJ5fuH3aVS", "René Descartes felsefede hangi yöntemi kullanır?", listOf("Metodik şüphe", "Deneysel gözlem", "Tarihsel analiz", "Sezgi temelli yorum"), 0, "Her şeyi şüpheyle sorgulayarak başlar."),
        com.semtinazizi.kitapduy.model.QuizQuestion("zu1CTuGwaeiyamx848Z6", "Platon'un mağara alegorisi neyi anlatır?", listOf("Gerçeklik ve algı", "Aşk", "Savaş", "Ekonomi"), 0, "Bilgi felsefesiyle ilgilidir.")
    ).shuffled()

    private val _gameState = MutableStateFlow(QuizGameState.STARTING)
    val gameState: StateFlow<QuizGameState> = _gameState.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _currentQuestion = MutableStateFlow<QuizQuestion?>(null)
    val currentQuestion: StateFlow<QuizQuestion?> = _currentQuestion.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    
    private val _leaderboard = MutableStateFlow<List<QuizLeaderboardEntry>>(emptyList())
    val leaderboard = _leaderboard.asStateFlow()

    fun loadLeaderboard() {
        screenModelScope.launch {
            _leaderboard.value = multiplayerRepo.getLeaderboard()
        }
    }
    
    // Save Score to Leaderboard
    fun saveScoreToLeaderboard() {
        screenModelScope.launch {
            try {
                val uid = authRepo.currentUserUid
                if (uid != null) {
                    val profile = authRepo.getUserProfile(uid)
                    val userName = profile?.name ?: "Anonim"
                    multiplayerRepo.updateLeaderboard(uid, userName, _score.value.toLong())
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private val _lives = MutableStateFlow(3)
    val lives: StateFlow<Int> = _lives.asStateFlow()

    private val _timeLeft = MutableStateFlow(15) // Her soru 15 saniye
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()
    
    private val _selectedAnswer = MutableStateFlow<Int?>(null)
    val selectedAnswer: StateFlow<Int?> = _selectedAnswer.asStateFlow()
    
    private val _isCorrect = MutableStateFlow<Boolean?>(null)
    val isCorrect: StateFlow<Boolean?> = _isCorrect.asStateFlow()

    private val _isFiftyFiftyUsed = MutableStateFlow(false)
    val isFiftyFiftyUsed: StateFlow<Boolean> = _isFiftyFiftyUsed.asStateFlow()

    private val _isExtraTimeUsed = MutableStateFlow(false)
    val isExtraTimeUsed: StateFlow<Boolean> = _isExtraTimeUsed.asStateFlow()

    private val _isHintUsed = MutableStateFlow(false)
    val isHintUsed: StateFlow<Boolean> = _isHintUsed.asStateFlow()

    private val _lastEmoji = MutableStateFlow<String?>(null)
    val lastEmoji: StateFlow<String?> = _lastEmoji.asStateFlow()

    private val _showHintDialog = MutableStateFlow(false)
    val showHintDialog: StateFlow<Boolean> = _showHintDialog.asStateFlow()

    private val _eliminatedOptions = MutableStateFlow<List<Int>>(emptyList())
    val eliminatedOptions: StateFlow<List<Int>> = _eliminatedOptions.asStateFlow()

    private var timerJob: Job? = null

    
    private val _lobbyState = MutableStateFlow("Rakip Aranıyor...")
    val lobbyState = _lobbyState.asStateFlow()
    
    private var currentMatchId: String? = null

    fun findMultiplayerMatch() {
        _gameState.value = QuizGameState.MULTIPLAYER_LOBBY
        _lobbyState.value = "Rakip Aranıyor..."
        
        screenModelScope.launch {
            try {
                val uid = authRepo.currentUserUid
                if (uid == null) {
                    _lobbyState.value = "Önce giriş yapmalısınız!"
                    return@launch
                }
                val profile = authRepo.getUserProfile(uid)
                val userName = profile?.name ?: "Anonim"
                
                // Try finding waiting match
                val match = multiplayerRepo.findWaitingMatch()
                if (match != null) {
                    _lobbyState.value = "Rakip Bulundu! Bağlanıyor..."
                    val joinedMatch = multiplayerRepo.joinMatch(match.matchId, uid, userName)
                    if (joinedMatch != null) {
                        currentMatchId = joinedMatch.matchId
                        startGame(isMultiplayer = true)
                    } else {
                        _lobbyState.value = "Bağlantı hatası!"
                    }
                } else {
                    _lobbyState.value = "Oda Oluşturuluyor..."
                    val newMatch = multiplayerRepo.createMatch(uid, userName)
                    if (newMatch != null) {
                        currentMatchId = newMatch.matchId
                        _lobbyState.value = "Oda Oluşturuldu. Rakip Bekleniyor..."
                        
                        // Listen for player 2
                        multiplayerRepo.observeMatch(newMatch.matchId).collect { update ->
                            if (update.status == "PLAYING") {
                                _lobbyState.value = "Rakip Katıldı! Oyun Başlıyor..."
                                startGame(isMultiplayer = true)
                            }
                        }
                    } else {
                        _lobbyState.value = "Oda oluşturulamadı."
                    }
                }
            } catch (e: Throwable) {
                _lobbyState.value = "Hata: ${e.message}"
            }
        }
    }

    fun resetGame() {
        _gameState.value = QuizGameState.STARTING
        _score.value = 0
        _lives.value = 3
    }

    fun startGame(isMultiplayer: Boolean = false) {
        try {
            _score.value = 0
            _lives.value = 3
            _currentQuestionIndex.value = 0
            _gameState.value = QuizGameState.PLAYING
            _isFiftyFiftyUsed.value = false
            _isExtraTimeUsed.value = false
            _isHintUsed.value = false
            _showHintDialog.value = false
            _eliminatedOptions.value = emptyList()
            loadNextQuestion()
        } catch (e: Throwable) {
            println("QUIZ_ERROR: CRASH IN startGame: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadNextQuestion() {
        try {
            if (_lives.value <= 0) {
                _gameState.value = QuizGameState.GAME_OVER
                return
            }

            if (_currentQuestionIndex.value >= allQuestions.size) {
                _gameState.value = QuizGameState.SUCCESS
                return
            }

            _selectedAnswer.value = null
            _isCorrect.value = null
            _showHintDialog.value = false
            _eliminatedOptions.value = emptyList()
            _currentQuestion.value = allQuestions[_currentQuestionIndex.value]
            _timeLeft.value = 15
            
            startTimer()
        } catch (e: Throwable) {
            println("QUIZ_ERROR: CRASH IN loadNextQuestion: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = screenModelScope.launch {
            while (isActive && _timeLeft.value > 0) {
                delay(1000L)
                _timeLeft.value -= 1
            }
            if (_timeLeft.value <= 0) {
                // Zaman doldu, yanlış kabul et
                handleAnswer(-1)
            }
        }
    }

    fun submitAnswer(selectedIndex: Int) {
        if (_selectedAnswer.value != null || _gameState.value != QuizGameState.PLAYING) return
        handleAnswer(selectedIndex)
    }

    private fun handleAnswer(selectedIndex: Int) {
        timerJob?.cancel()
        _selectedAnswer.value = selectedIndex
        
        val question = _currentQuestion.value ?: return
        
        if (selectedIndex == question.correctIndex) {
            _isCorrect.value = true
            _score.value += 10 + _timeLeft.value // Hızlı cevap ekstra puan
        } else {
            _isCorrect.value = false
            _lives.value -= 1
        }
        
        // Biraz bekle ve sonraki soruya geç
        screenModelScope.launch {
            delay(1500)
            if (_lives.value > 0) {
                _currentQuestionIndex.value += 1
                loadNextQuestion()
            } else {
                _gameState.value = QuizGameState.GAME_OVER
            }
        }
    }
    
    override fun onDispose() {
        super.onDispose()
        timerJob?.cancel()
    }
    
    fun useFiftyFifty() {
        if (_isFiftyFiftyUsed.value) return
        _isFiftyFiftyUsed.value = true
        val question = _currentQuestion.value ?: return
        val wrongOptions = question.options.indices.filter { it != question.correctIndex }.shuffled().take(2)
        _eliminatedOptions.value = wrongOptions
    }

    fun useExtraTime() {
        if (_isExtraTimeUsed.value) return
        _isExtraTimeUsed.value = true
        _timeLeft.value += 15
    }



    fun useHint() {
        if (_isHintUsed.value) return
        _isHintUsed.value = true
        _showHintDialog.value = true
    }

    fun sendEmoji(emoji: String) {
        _lastEmoji.value = emoji
        screenModelScope.launch {
            delay(2000)
            if (_lastEmoji.value == emoji) {
                _lastEmoji.value = null
            }
        }
    }
    
    fun closeHint() {
        _showHintDialog.value = false
    }
}
