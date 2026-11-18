package com.semtinazizi.kitapduy.ui.screens

internal data class MasterpieceKnowledge(
    val title: String,
    val author: String,
    val keywords: List<String>,
    val briefSummary: String,
    val standardAnalysis: String,
    val deepAnalysis: String,
    val suggestedTags: List<String> = emptyList()
)

internal data class ConceptKnowledge(
    val topic: String,
    val keywords: List<String>,
    val categoryLabel: String,
    val briefSummary: String,
    val standardAnalysis: String,
    val deepAnalysis: String,
    val recommendedAuthors: List<String> = emptyList()
)

internal object KmpLiteraryAdvisorKnowledge {

    fun normalize(input: String): String =
        input.replace('İ', 'i').lowercase()
            .replace("\u0307", "")
            .replace('ç', 'c')
            .replace('ğ', 'g')
            .replace('ı', 'i')
            .replace('ö', 'o')
            .replace('ş', 's')
            .replace('ü', 'u')
            .replace('â', 'a')
            .replace('î', 'i')
            .replace('û', 'u')
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    val masterpieces: List<MasterpieceKnowledge> = listOf(
        MasterpieceKnowledge(
            title = "İçimizdeki Şeytan",
            author = "Sabahattin Ali",
            keywords = listOf("icimizdeki seytan", "omer macide", "bedri sabahattin"),
            briefSummary = "**İçimizdeki Şeytan**, Sabahattin Ali’nin Ömer ile Macide’nin ilişkisi üzerinden insanın kendi irade zayıflığını dışsal bahanelerle gizleyişini anlattığı klasik romanıdır.",
            standardAnalysis = "**İçimizdeki Şeytan**, Sabahattin Ali’nin Ömer ile Macide’nin ilişkisi üzerinden insanın kendi seçimlerinin sorumluluğunu sorguladığı güçlü bir psikolojik ve toplumsal romandır. Ömer, savunamadığı zaaflarını ve tutarsızlıklarını ‘içindeki şeytana’ yükleyerek vicdanından kaçmaya çalışır. Roman, 1930’lar İstanbul’unun sahte aydın çevrelerini eleştirirken gerçek aşkın emek ve dürüstlük istediğini gösterir.",
            deepAnalysis = "1940 yılında yayımlanan eser, bireyin içsel ahlak hesaplaşmasını erken Cumhuriyet dönemi aydınlarının yozlaşmış çevresiyle birleştirir. Ömer’in ‘içimde benden bağımsız bir şeytan var’ savunması, aslında ahlaki sorumluluktan kaçış mekanizmasıdır; karşıt kutupta ise Macide’nin saf içtenliği ve Bedri’nin sessiz ama dirençli emeği durur. Eseri dinlerken Ömer’in kendisi hakkında kurduğu süslü cümlelerle eylemleri arasındaki uçuruma dikkat etmek romanın özünü ortaya çıkarır.",
            suggestedTags = listOf("sabahattin ali", "turk edebiyati", "psikolojik", "klasik")
        ),
        MasterpieceKnowledge(
            title = "Kürk Mantolu Madonna",
            author = "Sabahattin Ali",
            keywords = listOf("kurk mantolu madonna", "raif efendi", "maria puder"),
            briefSummary = "**Kürk Mantolu Madonna**, içine kapanık Raif Efendi’nin Berlin’de tanıştığı Maria Puder ile yaşadığı unutulmaz aşkın ve hayal kırıklıklarının zarif öyküsüdür.",
            standardAnalysis = "**Kürk Mantolu Madonna**, sıradan ve silik görünen Raif Efendi’nin siyah kaplı defterinden çıkan sarsıcı bir aşk ve varoluş hikâyesidir. Raif’in 1920’ler Berlin’inde bağımsız ruhlu ressam Maria Puder’le kurduğu bağ, toplumun kalıplarına sığmayan derin bir ruhdaşlığa dönüşür.",
            deepAnalysis = "Çerçeve anlatı tekniğini ustalıkla kullanan Sabahattin Ali, önce genç anlatıcının gözünden Raif Efendi’nin dışarıdaki suskun ve alay konusu hâlini çizer; ardından günlükle birlikte o sessizliğin ardındaki muazzam duyarlılığı açar. Maria Puder karakteri, kendi ayakları üzerinde duran modern kadın kimliğiyle yalnızca bir sevilen değil, Raif’i kendine uyandıran aynadır.",
            suggestedTags = listOf("kurk mantolu", "sabahattin ali", "melankolik", "ask")
        ),
        MasterpieceKnowledge(
            title = "Kuyucaklı Yusuf",
            author = "Sabahattin Ali",
            keywords = listOf("kuyucakli yusuf", "muazzez"),
            briefSummary = "**Kuyucaklı Yusuf**, anne babasını yitirdikten sonra evlat edinilen Yusuf’un kasaba eşrafının zorbalığına karşı onur ve aşk mücadelesini anlatır.",
            standardAnalysis = "Türk edebiyatında toplumcu gerçekçi romanın zirvelerinden olan **Kuyucaklı Yusuf**, doğayla bütünleşmiş dürüst bir gencin (Yusuf) Edremit kasabasındaki çıkar ilişkileri ve bürokratik çürümüşlükle çarpışmasını işler. Muazzez ile arasındaki sevgi, çürümüş düzen içinde kalabilen tek temiz sığınaktır.",
            deepAnalysis = "Yusuf yalnızca feodal ve bürokratik baskıya başkaldıran bir karakter değil, doğallığını yitirmemiş bir vicdanın temsilcisidir. Kasabanın Kaymakam’ı Salahattin Bey’in iyi niyetli ama pasif kaldığı düzende Şakir ve Hilmi Efendi gibi tipler güç yozlaşmasını somutlaştırır.",
            suggestedTags = listOf("sabahattin ali", "turk edebiyati", "klasik")
        ),
        MasterpieceKnowledge(
            title = "Suç ve Ceza",
            author = "Fyodor Dostoyevski",
            keywords = listOf("suc ve ceza", "raskolnikov", "sonya"),
            briefSummary = "**Suç ve Ceza**, yoksul hukuk öğrencisi Raskolnikov’un işlediği cinayet sonrası vicdanıyla verdiği amansız savaşı ve Sonya aracılığıyla arınmasını anlatır.",
            standardAnalysis = "Dostoyevski’nin başyapıtı **Suç ve Ceza**, ‘olağanüstü insanlar sıradan ahlak yasalarının üstündedir’ teorisini sınayan Raskolnikov’un eylem sonrası düştüğü ruhsal cehennemi inceler. Asıl ceza mahkeme hükmü değil, insanın kendi vicdanıyla ve insanlıktan kopuşuyla yüzleşmesidir; kurtuluşun kapısını ise koşulsuz merhametiyle Sonya açar.",
            deepAnalysis = "Petersburg’un boğucu yaz sıcağı ve dar odaları Raskolnikov’un bunalmış bilincinin dışavurumudur. Üç büyük karşılaşma — kurnaz sorgu yargıcı Porfiri Petroviç’le zekâ düellosu, Svidrigaylov’da gördüğü karanlık ayna ve Sonya’nın merhameti — rasyonel kibrin insan ruhunu neden kurtaramayacağını çarpıcı biçimde sergiler.",
            suggestedTags = listOf("dostoyevski", "suc ve ceza", "rus edebiyati", "felsefe")
        ),
        MasterpieceKnowledge(
            title = "Karamazov Kardeşler",
            author = "Fyodor Dostoyevski",
            keywords = listOf("karamazov kardesler", "ivan karamazov", "alyosa", "dimitri"),
            briefSummary = "**Karamazov Kardeşler**, bir babanın öldürülmesi etrafında inanç, kuşku, akıl ve vicdan çatışmalarını üç kardeş üzerinden ele alan dev felsefi romandır.",
            standardAnalysis = "Dostoyevski’nin son ve en kapsamlı eseri olan **Karamazov Kardeşler**, tutkulu Dimitri, rasyonalist ve kuşkucu İvan ile şefkatli Alyoşa üzerinden ‘Tanrı yoksa her şey mubah mıdır?’ sorusunu tartışır. Eserin kalbindeki ‘Büyük Engizisyoncu’ bölümü insan özgürlüğü ve otorite üzerine yazılmış en derin metinlerdendir.",
            deepAnalysis = "Her kardeş insan ruhunun bir boyutunu temsil eder: İvan aklın acımasız mantığını, Dimitri tutkunun savruluşunu, Alyoşa ise sevgiyle onarılan imanı taşır. Baba Fyodor Pavlovich’in ölümü, yalnızca polisiye bir düğüm değil tüm ailenin ve modern çağın ahlaki krizinin sembolüdür.",
            suggestedTags = listOf("dostoyevski", "felsefe", "klasik")
        ),
        MasterpieceKnowledge(
            title = "Yeraltından Notlar",
            author = "Fyodor Dostoyevski",
            keywords = listOf("yeraltindan notlar", "yeralti adami"),
            briefSummary = "**Yeraltından Notlar**, toplumdan kopmuş kırgın bir memurun keskin itiraflarıyla insan iradesinin salt akla ve faydacılığa sığmayacağını savunur.",
            standardAnalysis = "Modern varoluşçu edebiyatın ilk büyük manifestosu sayılan **Yeraltından Notlar**, kendi bilincinin fazlalığından felç olmuş Yeraltı Adamı’nın hem okura hem kendisine yönelttiği sert bir iç monologdur. İnsanın sırf özgür olduğunu kanıtlamak için bile kendi çıkarına aykırı kararlar alabildiğini vurgular.",
            deepAnalysis = "Roman iki bölüme ayrılır: İlk bölüm 19. yüzyıl rasyonalizmine ve ‘Kristal Saray’ ütopyasına felsefi bir başkaldırıdır; ikinci bölüm (‘Sulu Sepken Kar Üzerine’) ise kahramanın kırık kibriyle Lisa ve eski okul arkadaşları karşısında yaşadığı aşağılanma sahnelerini gösterir.",
            suggestedTags = listOf("dostoyevski", "felsefe", "varolus")
        ),
        MasterpieceKnowledge(
            title = "Budala",
            author = "Fyodor Dostoyevski",
            keywords = listOf("budala", "prens miskin", "nastasia"),
            briefSummary = "**Budala**, saf iyiliği ve koşulsuz merhametiyle tanınan Prens Mişkin’in çıkarlarla örülü yüksek sosyete içinde düştüğü trajediyi anlatır.",
            standardAnalysis = "**Budala**, ‘tam anlamıyla güzel ve iyi bir insan bugünün acımasız dünyasına girse ne olur?’ sorusunun yanıtını arar. Prens Mişkin’in içtenliği ve merhameti çevresindekilerde önce hayranlık uyandırır, ancak hırs ve gururla dolu dünyada saflık ‘budalalık’ olarak etiketlenir.",
            deepAnalysis = "Mişkin, Nastasya Filippovna ile Aglaya arasında bir sevgi-merhamet ikileminde kalırken, Rogojin’in yıkıcı tutkusu romanın gerilimini tırmandırır. Holbein’ın ‘Mezardaki Ölü İsa’ tablosu roman boyunca umut ile hiçlik arasındaki gerilimi simgeler.",
            suggestedTags = listOf("dostoyevski", "klasik")
        ),
        MasterpieceKnowledge(
            title = "Hayvan Çiftliği",
            author = "George Orwell",
            keywords = listOf("hayvan ciftligi", "napoleon", "snowball", "boxer"),
            briefSummary = "**Hayvan Çiftliği**, insanların yönetimini devirip eşitlikçi bir düzen kuran hayvanların zamanla yeni bir tiranlığa sürüklenişini anlatan politik bir fabldır.",
            standardAnalysis = "George Orwell’in keskin siyasal alegorisi **Hayvan Çiftliği**, ‘Bütün hayvanlar eşittir’ ilkesiyle başlayan devrimin güç tutkusu ve propaganda yoluyla ‘ama bazıları daha eşittir’ noktasına nasıl yozlaştığını gözler önüne serer.",
            deepAnalysis = "Domuz Napoleon’un iktidarı tekelleştirmesi, Squealer’ın tarihi ve emirleri sürekli yeniden yazması (propaganda), çalışkan at Boxer’ın sadakatinin sömürülmesi; yalnızca 1917 sonrasını değil her çağdaki otoriterleşmenin anatomisini sade ama sarsıcı bir dille resmeder.",
            suggestedTags = listOf("orwell", "distopya", "klasik")
        ),
        MasterpieceKnowledge(
            title = "1984",
            author = "George Orwell",
            keywords = listOf("1984", "bin dokuz yuz seksen dort", "winston smith", "buyuk birader", "big brother"),
            briefSummary = "**1984**, Büyük Birader’in her düşünceyi ve dili kontrol ettiği totaliter Okyanusya’da Winston Smith’in hakikat ve özgürlük arayışını anlatır.",
            standardAnalysis = "Distopya edebiyatının mihenk taşı **1984**, gözetim devleti, tarih çarpıtma (‘geçmişi kontrol eden geleceği kontrol eder’), ‘Yenisöylem’ yoluyla düşünme sınırlarını daraltma ve ‘Çiftdüşün’ kavramlarıyla özgürlüğün nasıl içeriden yok edildiğini gösterir.",
            deepAnalysis = "Winston Smith’in gizli günlüğü ve Julia ile kurduğu yasak bağ, iktidarın yok etmek istediği bireysel hafıza ve mahremiyetin sığınağıdır. Hakikat Bakanlığı’nın ‘Savaş Barıştır, Özgürlük Köleliktir, Cahilliğin Güçtür’ sloganları dil felsefesi açısından bugün hâlâ güncelliğini korur.",
            suggestedTags = listOf("orwell", "distopya", "felsefe")
        ),
        MasterpieceKnowledge(
            title = "Dönüşüm",
            author = "Franz Kafka",
            keywords = listOf("donusum", "gregor samsa", "kafka donusum"),
            briefSummary = "**Dönüşüm**, bir sabah kendini dev bir böceğe dönüşmüş halde bulan Gregor Samsa’nın ailesiyle ilişkilerinin ve insanî değerinin nasıl eridiğini anlatır.",
            standardAnalysis = "Franz Kafka’nın başyapıtı **Dönüşüm**, modern insanın yalnızca ‘üretken ve faydalı’ olduğu sürece aile ve toplum içinde değer gördüğü gerçeğini absürt ama buz gibi gerçekçi bir metaforla açığa vurur.",
            deepAnalysis = "Kafka hikâyeyi bir şokla başlatır ancak asıl trajedi dönüşümün kendisi değil, Gregor’un hâlâ tren saatini ve ailesinin borçlarını düşünmesidir. Zamanla kız kardeşi Grete’nin şefkati de yorgunluğa ve yabancılaşmaya dönüşür; odadaki elma sahnesi suçluluk ve dışlanmanın doruk noktasıdır.",
            suggestedTags = listOf("kafka", "klasik", "varolus")
        ),
        MasterpieceKnowledge(
            title = "Dava",
            author = "Franz Kafka",
            keywords = listOf("dava kafka", "josef k"),
            briefSummary = "**Dava**, neyle suçlandığını hiç öğrenemeden görünmez bir mahkemenin çarklarına yakalanan Josef K.’nın bürokratik kâbusunu anlatır.",
            standardAnalysis = "Kafka’nın tamamlanmamış başyapıtı **Dava**, bireyin adını bilmediği, kurallarına erişemediği anonim bir bürokrasi ve modern adalet mekanizması karşısındaki çaresizliğini simgeler.",
            deepAnalysis = "Katedraldeki ‘Yasanın Kapısında’ meseli romanın felsefi anahtarıdır: Kapının önünde ömür tüketen taşralı adam gibi Josef K. da hem sistemin kurbanı hem de kendi suçluluk duygusunun tutsağı olur.",
            suggestedTags = listOf("kafka", "felsefe", "klasik")
        ),
        MasterpieceKnowledge(
            title = "Satranç",
            author = "Stefan Zweig",
            keywords = listOf("satranc", "dr b", "czentovic", "zweig satranc"),
            briefSummary = "**Satranç**, Gestapo tarafından mutlak tecritte tutulan Dr. B.’nin zihnini korumak için satranca sığınışını ve bir gemi yolculuğunda dünya şampiyonuyla karşılaşmasını anlatır.",
            standardAnalysis = "Stefan Zweig’ın son başyapıtı **Satranç**, faşizmin ruhsal işkencesi altında akıl sağlığını satranç kitabını ezberleyip kendi zihninde maçlar oynayarak koruyan Dr. B.’nin iç bölünmesini ustalıkla işler. Mekanik ve duygusuz şampiyon Czentovic ile insanî kırılganlık arasındaki zıtlık nefes kesicidir.",
            deepAnalysis = "Zweig’ın üslubu bu kısa novellada psikolojik gerilimi doruğa taşır: Tecrit (hiçbir uyaranın olmadığı beyaz oda) insan zihnini o kadar aç bırakır ki, satranç bir kurtuluş kapısı olmaktan çıkıp zihni ikiye bölen ‘satranç zehirlenmesine’ dönüşür.",
            suggestedTags = listOf("zweig", "psikolojik", "klasik")
        ),
        MasterpieceKnowledge(
            title = "Bilinmeyen Bir Kadının Mektubu",
            author = "Stefan Zweig",
            keywords = listOf("bilinmeyen bir kadinin mektubu", "zweig mektup"),
            briefSummary = "**Bilinmeyen Bir Kadının Mektubu**, ünlü bir yazarın kırk birinci yaş gününde aldığı isimsiz bir mektupla ömür boyu sürmüş tek taraflı bir aşkı öğrenmesini anlatır.",
            standardAnalysis = "Zweig’ın insan kalbinin en kırılgan kıvrımlarını işlediği **Bilinmeyen Bir Kadının Mektubu**, karşılıksız bir tutkunun sessiz adanmışlığını ve yıllar boyunca bir mektupta biriken itirafın yakıcı gücünü sunar.",
            deepAnalysis = "Eser boyunca erkek anlatıcının hafızasızlığı ve her şeyi yüzeyde yaşaması, kadının sarsılmaz belleği ve adanmışlığıyla çarpıcı bir tezat oluşturur. Zweig, melodrama düşmeden psikolojik gözlemlerle okurun vicdanını yoklar.",
            suggestedTags = listOf("zweig", "melankolik", "ask")
        ),
        MasterpieceKnowledge(
            title = "Amok Koşucusu",
            author = "Stefan Zweig",
            keywords = listOf("amok kosucusu", "amok"),
            briefSummary = "**Amok Koşucusu**, Uzak Doğu’da yalnızlığa gömülmüş bir doktorun kibirli bir kadına duyduğu saplantılı sorumlulukla kendini tüketişini anlatır.",
            standardAnalysis = "Bir gemi güvertesinde gece karanlığında anlatılan **Amok Koşucusu**, bastırılmış öfke, mesleki gurur ve önüne geçilemeyen bir tutkunun insanı nasıl kör bir koşuya (‘amok’) sürüklediğini gösterir.",
            deepAnalysis = "Zweig bu eserde anlatıcıya çerçeve kurdurarak itirafın gerilimini artırır; doktorun başlangıçtaki kibirli reddedişi birkaç saat içinde pişmanlık ve onur kurtarma saplantısına evrilir.",
            suggestedTags = listOf("zweig", "psikolojik")
        ),
        MasterpieceKnowledge(
            title = "Tutunamayanlar",
            author = "Oğuz Atay",
            keywords = listOf("tutunamayanlar", "turgut ozben", "selim isik"),
            briefSummary = "**Tutunamayanlar**, arkadaşı Selim Işık’ın intiharının ardından izini süren Turgut Özben’in modern burjuva kalıplarıyla ve kendi benliğiyle hesaplaşmasını anlatır.",
            standardAnalysis = "Türk postmodern edebiyatının dönüm noktası olan **Tutunamayanlar**, toplumsal kurallara ve yüzeysel başarı kalıplarına uyum sağlayamayan bireyin ironi, bilinç akışı ve derin hüzünle örülü iç dünyasını gözler önüne serer.",
            deepAnalysis = "Oğuz Atay, ansiklopedi maddeleri, tiyatro diyalogları, mektuplar ve noktalama işaretlerini kaldıran 67 sayfalık tek cümle gibi yenilikçi biçimlerle Selim’in ‘Tutunamayanlar Ansiklopedisi’ni ve Turgut’un iç sesi Olric’i kurar. Alaycılık aslında yaralı bir duyarlılığın zırhıdır.",
            suggestedTags = listOf("oguz atay", "turk edebiyati", "modernizm")
        ),
        MasterpieceKnowledge(
            title = "Saatleri Ayarlama Enstitüsü",
            author = "Ahmet Hamdi Tanpınar",
            keywords = listOf("saatleri ayarlama enstitusu", "hayri irdal", "halit ayarci"),
            briefSummary = "**Saatleri Ayarlama Enstitüsü**, Doğu ile Batı arasında kalmış toplumun modernleşme serüvenini Hayri İrdal’ın mizahi anıları üzerinden anlatan eşsiz bir hicivdir.",
            standardAnalysis = "Ahmet Hamdi Tanpınar’ın başyapıtı **Saatleri Ayarlama Enstitüsü**, hiçbir somut işlevi olmayan devasa bir kurumun bürokratik ciddiyetle büyümesini anlatarak köksüz taklitçiliği ve zaman algımızdaki parçalanmayı incelikle hicveder.",
            deepAnalysis = "Hayri İrdal’ın çocukluğundaki Muvakkit Nuri Efendi’nin manevî zaman anlayışından Halit Ayarcı’nın pragmatik ve gösterişçi düzenine geçişi, yalnızca kurum eleştirisi değil aynı zamanda modernleşen Türkiye’nin ruhsal haritasıdır.",
            suggestedTags = listOf("tanpinar", "turk edebiyati", "klasik")
        ),
        MasterpieceKnowledge(
            title = "Huzur",
            author = "Ahmet Hamdi Tanpınar",
            keywords = listOf("huzur tanpinar", "mumtaz nuran"),
            briefSummary = "**Huzur**, II. Dünya Savaşı arifesinde Mümtaz ile Nuran’ın aşkı üzerinden Doğu-Batı sentezi, İstanbul kültürü ve içsel dinginlik arayışını anlatır.",
            standardAnalysis = "Dört bölümden oluşan ve yirmi dört saatlik bir zaman kesitine sığan **Huzur**, Mümtaz’ın Nuran’a duyduğu aşkta eski İstanbul estetiği ile modern bunalım arasında kurduğu kırılgan dengeyi şiirsel bir dille aktarır.",
            deepAnalysis = "İhsan’ın tarihsel süreklilik fikri, Nuran’ın Dede Efendi’nin makamlarıyla somutlaşan estetiği ve Suad’ın nihilist gölgesi arasında kalan Mümtaz, aslında bütün bir medeniyet krizinin vicdanıdır.",
            suggestedTags = listOf("tanpinar", "turk edebiyati", "melankolik")
        ),
        MasterpieceKnowledge(
            title = "İnce Memed",
            author = "Yaşar Kemal",
            keywords = listOf("ince memed", "abdi aga"),
            briefSummary = "**İnce Memed**, Çukurova’da Abdi Ağa’nın zulmüne dayanamayıp dağa çıkan genç bir köylünün halkın umuduna dönüşen destansı direnişini anlatır.",
            standardAnalysis = "Yaşar Kemal’in dünya klasikleri arasına giren eseri **İnce Memed**, feodal baskı karşısında ‘mecbur kaldığı için’ eşkıyalığa sürüklenen bir insanın çaresizlikten doğan adalet arayışını doğanın görkemiyle birleştirir.",
            deepAnalysis = "Torosların çakırdikenleri, Çukurova’nın sıcağı ve sözlü halk destanı ritmi Yaşar Kemal’in epik üslubunu yaratır. Memed bir kahramanlık masalının değil, toprağa ve onura tutunan sıradan insanların hikâyesidir.",
            suggestedTags = listOf("yasar kemal", "turk edebiyati")
        ),
        MasterpieceKnowledge(
            title = "Simyacı",
            author = "Paulo Coelho",
            keywords = listOf("simyaci", "santiago", "kisisel menkibe"),
            briefSummary = "**Simyacı**, Endülüslü çoban Santiago’nun rüyasındaki hazineyi bulmak için çıktığı yolculukta kendi ‘Kişisel Menkıbesi’ni ve evrenin dilini keşfetmesini anlatır.",
            standardAnalysis = "Masalsı bir bilgelikle örülü **Simyacı**, insanın kalbinin sesini dinleyip konfor alanını terk ettiğinde karşısına çıkan sınavları ve asıl hazinenin yolun sonunda değil, yolculuk sırasında dönüştüğümüz kişide saklı olduğunu vurgular.",
            deepAnalysis = "Kristal dükkânı sahibi ve İngiliz gezgin gibi yan karakterler hayallerini erteleyen veya hayatı yalnızca kitaplardan okumaya çalışan tipleri simgelerken, çöldeki Simyacı ise bilgelik ile eylemi birleştirir.",
            suggestedTags = listOf("kisisel gelisim", "umut", "ilham")
        ),
        MasterpieceKnowledge(
            title = "Küçük Prens",
            author = "Antoine de Saint-Exupéry",
            keywords = listOf("kucuk prens", "tilki", "gul"),
            briefSummary = "**Küçük Prens**, küçük bir gezegenden yola çıkan çocuğun gözünden yetişkinlerin sayılara ve unvanlara sıkışmış dünyasına yöneltilen şiirsel bir sevgi ve sadakat masalıdır.",
            standardAnalysis = "‘İnsan ancak yüreğiyle baktığı zaman doğruyu görebilir’ dizesiyle hafızalara kazınan **Küçük Prens**, Tilki’nin ‘evcilleştirmek’ kavramıyla sevgi, sorumluluk ve emeğin gerçek değerini hatırlatır.",
            deepAnalysis = "Çöl ortasındaki pilotla kurulan dostluk; kibirli adam, kral, sarhoş ve işadamı gezegenleri üzerinden modern dünyanın yabancılaşmasını çocuk gözünün saflığıyla yargılar.",
            suggestedTags = listOf("kucuk prens", "umut", "klasik")
        ),
        MasterpieceKnowledge(
            title = "Sefiller",
            author = "Victor Hugo",
            keywords = listOf("sefiller", "jean valjean", "javert", "cosette"),
            briefSummary = "**Sefiller**, bir somun ekmek çaldığı için yıllarca kürek cezası çeken Jean Valjean’ın merhametle dönüşümünü ve adalet kavramının vicdanla sınavını anlatır.",
            standardAnalysis = "Victor Hugo’nun anıtsal eseri **Sefiller**, Piskopos Myriel’in bir akşam sergilediği bağışlayıcılıkla hayatı değişen Jean Valjean üzerinden hukuk kurallarının katılığı (Müfettiş Javert) ile insanî vicdanı karşı karşıya getirir.",
            deepAnalysis = "Fantine’in yoksulluğu, Cosette’e duyulan baba şefkati ve Paris barikatlarındaki genç idealistler üzerinden Hugo; yoksulluğun ve merhametsiz cezalandırmanın toplumu nasıl yaraladığını epik bir genişlikle gösterir.",
            suggestedTags = listOf("victor hugo", "klasik", "adalet")
        ),
        MasterpieceKnowledge(
            title = "Sherlock Holmes / Kızıl Dosya",
            author = "Arthur Conan Doyle",
            keywords = listOf("sherlock", "kizil dosya", "holmes", "watson"),
            briefSummary = "**Kızıl Dosya**, Dr. John Watson ile efsanevi dedektif Sherlock Holmes’ün ilk kez karşılaştığı ve tümdengelim sanatı eşliğinde Londra’daki esrarengiz bir cinayeti çözdüğü ilk romandır.",
            standardAnalysis = "Polisiye edebiyatının kurucu eseri **Kızıl Dosya**, Sherlock Holmes’ün sıradan insanların bakıp göremediği ayrıntılardan nasıl kesin sonuçlar çıkardığını ve Watson ile kurduğu benzersiz dostluğun doğuşunu soluk soluğa anlatır.",
            deepAnalysis = "Holmes yalnızca bir dedektif değil; soğukkanlı akıl yürütme ve bilimsel gözlemin Viktorya çağı Londra’sındaki cisimleşmiş halidir. Sesli dinlerken ayrıntıların zihinde nasıl örüldüğünü fark etmek büyük bir keyif verir.",
            suggestedTags = listOf("polisiye", "sherlock", "kizil dosya")
        ),
        MasterpieceKnowledge(
            title = "Moby Dick",
            author = "Herman Melville",
            keywords = listOf("moby dick", "kaptan ahab", "balina"),
            briefSummary = "**Moby Dick**, bacağını kaybettiği efsanevi beyaz balinaya karşı takıntılı bir intikam avına çıkan Kaptan Ahab ve Pequod gemisinin fırtınalı yolculuğunu anlatır.",
            standardAnalysis = "‘Bana İsmail deyin’ cümlesiyle açılan **Moby Dick**, insanın doğaya, kadere ve kendi içindeki saplantılı öfkeye açtığı umutsuz savaşı derin felsefi ve mitolojik göndermelerle işler.",
            deepAnalysis = "Pequod gemisi farklı kültürlerden ve inançlardan denizcileri taşıyan küçük bir insanlık mikrokozmosudur; Kaptan Ahab ise ölçüsüz kibrin (hubris) kendini ve çevresindekileri nasıl felakete sürüklediğinin trajik örneğidir.",
            suggestedTags = listOf("klasik", "macera", "moby dick")
        ),
        MasterpieceKnowledge(
            title = "Gurur ve Önyargı",
            author = "Jane Austen",
            keywords = listOf("gurur ve onyargi", "pride and prejudice", "elizabeth bennet", "mr darcy", "darcy"),
            briefSummary = "**Gurur ve Önyargı**, keskin zekâlı Elizabeth Bennet ile mesafeli Mr. Darcy’nin ilk izlenimlerin ve toplumsal önyargıların ötesine geçip birbirlerini tanıma hikâyesidir.",
            standardAnalysis = "İngiliz edebiyatının en sevilen klasiklerinden **Gurur ve Önyargı**, 19. yüzyıl taşra toplumunun evlilik ve sınıf kalıplarını zarif bir ironiyle eleştirirken aşkın insanı ancak önyargılardan arındığında büyüttüğünü anlatır.",
            deepAnalysis = "Austen, Darcy’nin ‘gururu’ ile Elizabeth’in ‘önyargısını’ karşılıklı bir olgunlaşma sürecine sokar. Her iki karakter de ancak kendi yanılabilirliğini kabul ettikten sonra gerçek saygıyı ve uyumu yakalar.",
            suggestedTags = listOf("klasik", "ask", "jane austen")
        )
    )

    val concepts: List<ConceptKnowledge> = listOf(
        ConceptKnowledge(
            topic = "Varoluşçuluk (Existentialism)",
            keywords = listOf("varolusculuk", "varolus", "existentialism", "sartre", "camus", "absurdizm", "sacma"),
            categoryLabel = "Felsefi Akım",
            briefSummary = "**Varoluşçuluk**, ‘varoluş özden önce gelir’ ilkesiyle insanın önceden çizilmiş bir kadere değil, kendi seçimlerine ve eylemlerine anlam borçlu olduğunu savunur.",
            standardAnalysis = "**Varoluşçuluk**, Jean-Paul Sartre, Albert Camus, Kierkegaard ve Dostoyevski gibi düşünürlerle özdeşleşen felsefi yaklaşımdır. İnsanın dünyaya belirli bir amaçla gelmediğini; kendi anlamını özgür seçimleriyle ve o seçimlerin sorumluluğunu üstlenerek yarattığını savunur. Camus’ye göre hayatın kayıtsızlığı karşısında umutsuzluğa düşmek yerine yaşamaya ve yaratmaya devam etmek gerçek başkaldırıdır.",
            deepAnalysis = "Sartre ‘insan özgürlüğe mahkûmdur’ derken, seçmeme lüksümüzün bile bir seçim olduğunu hatırlatır. Kaygı (angst) aslında özgürlüğün baş dönmesidir; edebiyatta Kafka’nın yabancılaşması, Dostoyevski’nin Yeraltı Adamı ve Camus’nün Yabancı’sı bu felsefi zeminin en güçlü edebi karşılıklarıdır.",
            recommendedAuthors = listOf("Dostoyevski", "Franz Kafka", "Albert Camus", "Jean-Paul Sartre")
        ),
        ConceptKnowledge(
            topic = "Stoacılık (Stoicism)",
            keywords = listOf("stoacilik", "stoa", "marcus aurelius", "seneca", "epiktetos", "kendime dusunceler"),
            categoryLabel = "Felsefi Akım",
            briefSummary = "**Stoacılık**, kontrol edebildiğimiz tek şeyin kendi zihnimiz, tepkilerimiz ve erdemli seçimlerimiz olduğunu hatırlatarak içsel huzur ve metanet kazandıran kadim felsefedir.",
            standardAnalysis = "**Stoacılık**; Epiktetos, Seneca ve Roma İmparatoru Marcus Aurelius tarafından şekillendirilen pratik yaşam felsefesidir. ‘İnsanları olaylar değil, olaylar hakkındaki yargıları sarsar’ düşüncesini merkeze alır. Dış koşulları (öfke, kayıp, şans) yönetemeyiz ama karakterimizi ve tavrımızı her an yönetebiliriz.",
            deepAnalysis = "Stoacı öğreti bilgelik, cesaret, adalet ve ölçülülüğü dört temel erdem sayar. *Amor Fati* (kaderi sevmek) pasif bir boyun eğiş değil, yaşanan her zorluğu karakteri güçlendiren bir antrenman sahası olarak kabul etmektir.",
            recommendedAuthors = listOf("Marcus Aurelius", "Seneca", "Epiktetos")
        ),
        ConceptKnowledge(
            topic = "Nietzsche, Üstinsan & Nihilizm",
            keywords = listOf("nietzsche", "ustinsan", "ubermensch", "nihilizm", "hiclik", "amor fati"),
            categoryLabel = "Felsefe",
            briefSummary = "**Nihilizm** geleneksel değerlerin çöküşünü ifade ederken, **Friedrich Nietzsche** eski değerleri aşarak kendi yaratıcı anlamını kuran ‘Üstinsan’ idealini önerir.",
            standardAnalysis = "Friedrich Nietzsche, nihilizmi yaşamı reddetmek değil, sahte konfor ve sürü ahlakı yıkıldığında yeniden değer yaratmak için bir geçiş aşaması olarak görür. *Böyle Buyurdu Zerdüşt* eserindeki ‘Üstinsan’, başkalarından üstün olan değil, kendi zaaflarını aşabilen ve hayata bütün acılarıyla ‘evet’ diyebilen kişidir.",
            deepAnalysis = "Dostoyevski’nin *Suç ve Ceza*’daki Raskolnikov’u ‘olağanüstü insan’ fikrini yanlışlayarak vicdanın reddini yıkıma dönüştürürken, Nietzsche acıyı dönüştüren sanatsal ve yaratıcı iradeyi savunur.",
            recommendedAuthors = listOf("Friedrich Nietzsche", "Fyodor Dostoyevski")
        ),
        ConceptKnowledge(
            topic = "Psikoloji, Arketipler & Anlam Arayışı",
            keywords = listOf("psikoloji", "psikanaliz", "jung", "freud", "arketip", "golge", "viktor frankl", "anlam arayisi", "kaygi"),
            categoryLabel = "Psikoloji & Felsefe",
            briefSummary = "**Derinlik Psikolojisi**, insan davranışlarının ardındaki bilinçdışı motifleri, içsel gölgeyi ve zorluklar ortasında anlam bulma kapasitesini inceler.",
            standardAnalysis = "Sigmund Freud bilinçdışının bastırılmış itkilerini, Carl Gustav Jung ise kolektif bilinçdışını ve bastırdığımız karanlık yanımız olan ‘Gölge Arketipi’ni vurgular. Viktor Frankl ise en ağır acılar içinde bile insanı ayakta tutan gücün anlam arayışı olduğunu gösterir.",
            deepAnalysis = "Edebiyat psikolojinin en canlı laboratuvarıdır: Sabahattin Ali’nin *İçimizdeki Şeytan*’ı kendi gölgesiyle yüzleşemeyen insanı anlatırken, Zweig’ın novellaları bilinçdışı dürtülerin anlık kırılmalarını mikroskop altına yatırır.",
            recommendedAuthors = listOf("Stefan Zweig", "Sabahattin Ali", "Fyodor Dostoyevski")
        ),
        ConceptKnowledge(
            topic = "Tasavvuf, Mevlana & Yunus Emre",
            keywords = listOf("tasavvuf", "mevlana", "yunus emre", "mesnevi", "hakikat", "gönul", "ask tasavvuf"),
            categoryLabel = "Anadolu İrfanı & Felsefe",
            briefSummary = "**Tasavvuf**, kibir ve bencilliği arındırarak insan-ı kâmil olma yolunda sevgiyi, hoşgörüyü ve içsel birliği arayan köklü irfan geleneğidir.",
            standardAnalysis = "Mevlana Celaleddin-i Rumi’nin *Mesnevi*’si ve Yunus Emre’nin sade Türkçe şiirleri insan kalbini evrenin aynası olarak görür. Bilgi yalnızca kuru akılla değil, gönül terbiyesi ve öz-bilgiyle olgunlaşır: ‘İlim ilim bilmektir, ilim kendin bilmektir.’",
            deepAnalysis = "Yunus Emre’de gurbet, ölüm korkusu ve fanilik sevgiyle aşılır; Mevlana’da neyin feryadı insanın asıl kaynağına duyduğu hasretin simgesidir.",
            recommendedAuthors = listOf("Mevlana", "Yunus Emre")
        ),
        ConceptKnowledge(
            topic = "Sabahattin Ali & Türk Edebiyatı",
            keywords = listOf("sabahattin ali", "turk edebiyati", "resat nuri", "refik halid", "omer seyfettin"),
            categoryLabel = "Yazar & Edebiyat",
            briefSummary = "**Sabahattin Ali**, Cumhuriyet dönemi Türk edebiyatında insan ruhunun en ince kırılmalarını ve Anadolu gerçeğini yalın, lirik ama sarsıcı bir gerçekçilikle anlatmış ustadır.",
            standardAnalysis = "**Sabahattin Ali**, öykü ve romanlarında memurları, yoksul Anadolu insanını, imkânsız aşkları ve iç hesaplaşmaları kusursuz bir gözlem gücüyle işler. *Kürk Mantolu Madonna*, *İçimizdeki Şeytan* ve *Kuyucaklı Yusuf* zamana meydan okuyan üç temel yapıtaşıdır.",
            deepAnalysis = "Sade cümlelerinin ardında büyük bir psikolojik derinlik taşır; kahramanlarını yargılamadan zaaflarıyla birlikte resmeder.",
            recommendedAuthors = listOf("Sabahattin Ali")
        ),
        ConceptKnowledge(
            topic = "Fyodor Dostoyevski",
            keywords = listOf("dostoyevski", "fyodor", "rus edebiyati", "tolstoy"),
            categoryLabel = "Büyük Yazar",
            briefSummary = "**Dostoyevski**, insan ruhunun karanlık koridorlarını, suçluluk, iman, kibir ve merhameti eşine az rastlanır bir psikolojik derinlikle çözen edebiyat devidir.",
            standardAnalysis = "19. yüzyıl Rus edebiyatının zirvelerinden Dostoyevski ve Tolstoy, bireyin ahlaki sınavını bütün toplumun vicdanı haline getirmiştir. Dostoyevski’de karakterler yalnızca olay yaşamaz, fikirler uğruna acı çeker ve kendilerini sınar; Tolstoy ise yaşamın büyük tarihsel akışında sıradan insanın hakikatini arar.",
            deepAnalysis = "Dostoyevski’ye başlamak için *Beyaz Geceler* veya *Suç ve Ceza*, olgunluk dönemi derinliği içinse *Karamazov Kardeşler* eşsiz duraklardır.",
            recommendedAuthors = listOf("Fyodor Dostoyevski", "Lev Tolstoy")
        ),
        ConceptKnowledge(
            topic = "Stefan Zweig",
            keywords = listOf("stefan zweig", "zweig"),
            categoryLabel = "Büyük Yazar",
            briefSummary = "**Stefan Zweig**, tek bir gecede veya kısa bir mektupta insan ruhunun nasıl tamamen altüst olabildiğini nefes kesen bir psikolojik gerilimle anlatan novella ustasıdır.",
            standardAnalysis = "Viyanalı yazar Stefan Zweig; *Satranç*, *Bilinmeyen Bir Kadının Mektubu*, *Amok Koşucusu* ve *Olağanüstü Bir Gece* gibi kısa ama yoğun eserleriyle tek oturuşta bitirilip günlerce üzerinde düşünülen unutulmaz portreler sunar.",
            deepAnalysis = "Freud’la dostluğunun getirdiği derin bilinçdışı gözlemi, Zweig’ın akıcı cümlelerinde dramatik bir hızla birleşir; özellikle sesli kitap formatında dinlemesi son derece sürükleyicidir.",
            recommendedAuthors = listOf("Stefan Zweig")
        ),
        ConceptKnowledge(
            topic = "Distopya, Alegori & Bilinç Akışı",
            keywords = listOf("distopya", "alegori", "bilinc akisi", "modernizm", "postmodernizm", "romantizm", "realizm", "katarsis", "katharsis", "yabancilasma"),
            categoryLabel = "Edebî Kavram",
            briefSummary = "**Edebî teknikler ve akımlar**, yazarların yalnızca ne anlattığını değil, insan gerçeğini okurun zihninde nasıl yeniden kurduğunu belirler.",
            standardAnalysis = "**Distopya** özgürlüğün yitirildiği karanlık gelecek tasarımlarını (*1984*, *Cesur Yeni Dünya*), **Alegori** soyut fikirlerin simgesel öykülerle anlatımını (*Hayvan Çiftliği*), **Bilinç Akışı** ise kahramanın zihninden geçen çağrışımları sansürsüz aktarışını (*Tutunamayanlar*) ifade eder. **Katarsis** ise okurun tragedya ve sanat yoluyla duygusal arınma yaşamasıdır.",
            deepAnalysis = "Realizm hayatı olduğu gibi yansıtmaya odaklanırken Modernizm bireyin içsel parçalanmasını merkeze alır; Postmodernizm ise anlatının kendisini ve okurla kurulan oyunu görünür kılar.",
            recommendedAuthors = listOf("George Orwell", "Franz Kafka", "Oğuz Atay", "Ahmet Hamdi Tanpınar")
        ),
        ConceptKnowledge(
            topic = "Bilim, Evren, Tarih & Medeniyet",
            keywords = listOf("bilim", "evren", "kuantum", "uzay", "tarih", "medeniyet", "antik yunan", "sokrates", "platon", "aristoteles", "roenesans", "ronesans", "aydinlanma", "yapay zeka"),
            categoryLabel = "Bilim & Tarih",
            briefSummary = "**Bilim tarihi ve düşünce tarihi**, insanlığın ‘Evren nasıl işler?’ ve ‘Adil bir toplum nasıl kurulur?’ sorularına yüzyıllar boyunca verdiği ortak yanıtların öyküsüdür.",
            standardAnalysis = "Antik Yunan’da Sokrates’in sorgulama yöntemi, Platon’un *Devlet* ve Aristoteles’in mantık temelleriyle başlayan akıl yürütme geleneği, İbn Haldun’un *Mukaddime*’sinde toplumların yasalarını inceleyen tarih bilimine ve Aydınlanma’dan modern fizik devrimine uzanır. Merak duygusu edebiyatla bilimi aynı kökten besler.",
            deepAnalysis = "Tarih okumaları bugünün krizlerini anlamamızı sağlarken, popüler bilim ve klasik felsefe eserleri zihinsel ufku genişletir.",
            recommendedAuthors = listOf("Sokrates", "Platon", "Marcus Aurelius", "Barış Özcan")
        )
    )

    val allKnownWorkTitles: List<String> = masterpieces.map { it.title }

    fun findMasterpieceAnalysis(
        normalizedPrompt: String,
        matchedCatalogTitle: String?,
        depth: String
    ): MasterpieceKnowledge? {
        val normCatalogTitle = matchedCatalogTitle?.let(::normalize)
        // Prefer exact catalog title match first, then keyword/title match in query
        val byTitle = if (!normCatalogTitle.isNullOrBlank()) {
            masterpieces.firstOrNull { mp ->
                val normMpTitle = normalize(mp.title)
                normMpTitle == normCatalogTitle ||
                    normCatalogTitle.contains(normMpTitle) ||
                    mp.keywords.any { kw -> normCatalogTitle.contains(kw) }
            }
        } else null
        if (byTitle != null) return byTitle

        return masterpieces.firstOrNull { mp ->
            val normTitle = normalize(mp.title)
            Regex("\\b${Regex.escape(normTitle)}(?:ya|ye|yi|yu|dan|den|tan|ten|da|de|ta|te|nin|nun|in|un|i|u|a|e)?\\b")
                .containsMatchIn(normalizedPrompt) ||
                mp.keywords.any { normalizedPrompt.contains(it) }
        }
    }

    fun formatMasterpieceReply(mp: MasterpieceKnowledge, depth: String): String =
        when (depth) {
            "brief" -> mp.briefSummary
            "deep" -> "${mp.standardAnalysis}\n\n${mp.deepAnalysis}"
            else -> mp.standardAnalysis
        }

    fun findConceptReply(normalizedPrompt: String, depth: String): Pair<ConceptKnowledge, String>? {
        val hit = concepts.firstOrNull { concept ->
            concept.keywords.any { kw ->
                Regex("\\b${Regex.escape(kw)}\\w*").containsMatchIn(normalizedPrompt)
            }
        } ?: return null

        val body = when (depth) {
            "brief" -> hit.briefSummary
            "deep" -> "${hit.standardAnalysis}\n\n${hit.deepAnalysis}"
            else -> hit.standardAnalysis
        }
        return hit to body
    }
}
