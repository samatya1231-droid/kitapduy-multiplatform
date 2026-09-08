import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

// ─── Kullanıcı verisi bulunan tüm tablolar ───────────────────────────────────
// Otomatik cascade olanlar (auth.users silinince zaten temizlenir):
//   quiz_leaderboard.user_id  → FOREIGN KEY REFERENCES auth.users ON DELETE CASCADE
//
// Manuel temizlenmesi gerekenler (text/uuid sütun, FK cascade yok):
//   comments          user_id     (posts.id → comments.post_id CASCADE var, posts silinince comments da silinir)
//   fcm_tokens        user_id
//   feed_comments     user_id
//   followers         follower_id, following_id
//   global_chat       user_id
//   notifications     receiver_id
//   poems             user_id (uuid)
//   posts             user_id     (silinince comments cascade ile silinir)
//   profiles          id
//   reading_clubs     ownerid
//   user_bookshelves  user_id
//   user_clubs        user_id
//   users             uid         (app kodu bu tabloyu kullanıyor, schema'da görünmüyor olabilir)
// ─────────────────────────────────────────────────────────────────────────────

Deno.serve(async (req: Request) => {
  // CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
      },
    })
  }

  // Yalnızca POST
  if (req.method !== 'POST') {
    return json({ error: 'Method not allowed' }, 405)
  }

  try {
    // 1. Gövdeyi oku; onay bayrağını doğrula
    const body = await req.json().catch(() => ({}))
    if (body.confirm !== true) {
      return json({ error: 'confirm:true gerekli' }, 400)
    }

    // 2. Bearer token'ı al
    const authHeader = req.headers.get('Authorization') ?? ''
    if (!authHeader.startsWith('Bearer ')) {
      return json({ error: 'Yetkilendirme başlığı eksik' }, 401)
    }
    const token = authHeader.slice(7)

    // 3. Admin client (service role) — kimlik bilgileri yalnızca sunucuda kalır
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!,
      { auth: { autoRefreshToken: false, persistSession: false } }
    )

    // 4. Token'ı Supabase Auth üzerinden doğrula (yalnızca JWT decode değil)
    //    Hedef kullanıcı ID'si her zaman buradan türetilir; istek gövdesinden asla alınmaz.
    const { data: { user }, error: authError } = await supabase.auth.getUser(token)
    if (authError || !user) {
      return json({ error: 'Geçersiz veya süresi dolmuş token' }, 401)
    }
    const uid = user.id   // UUID string

    // 5. Uygulama verilerini temizle
    //    Sıralama önemli: önce bağımlı tablolar, sonra ana tablolar.

    // 5a. Takip ilişkileri (her iki yön)
    await supabase.from('followers').delete().eq('follower_id', uid)
    await supabase.from('followers').delete().eq('following_id', uid)

    // 5b. Kulüp üyelikleri
    await supabase.from('user_clubs').delete().eq('user_id', uid)

    // 5c. Sahip olduğu kulüpler (user_clubs ON DELETE CASCADE ile üyeler de silinir)
    await supabase.from('reading_clubs').delete().eq('ownerid', uid)

    // 5d. Gönderiler (posts silinince comments ON DELETE CASCADE ile silinir)
    await supabase.from('posts').delete().eq('user_id', uid)

    // 5e. Diğer tablolardaki yorum kayıtları
    await supabase.from('feed_comments').delete().eq('user_id', uid)
    await supabase.from('comments').delete().eq('user_id', uid)

    // 5f. Şiirler
    await supabase.from('poems').delete().eq('user_id', uid)

    // 5g. Kitaplık
    await supabase.from('user_bookshelves').delete().eq('user_id', uid)

    // 5h. Global sohbet mesajları
    await supabase.from('global_chat').delete().eq('user_id', uid)

    // 5i. Bildirimler
    await supabase.from('notifications').delete().eq('receiver_id', uid)

    // 5j. FCM push token
    await supabase.from('fcm_tokens').delete().eq('user_id', uid)

    // 5k. Profil tablosu (public.profiles)
    await supabase.from('profiles').delete().eq('id', uid)

    // 5l. users tablosu — app kodu postgrest["users"] kullanıyor;
    //     schema'da görünmüyorsa hata fırlatmaz, sessizce geçer.
    await supabase.from('users').delete().eq('uid', uid)

    // 5m. Quiz liderlik tablosu (FK cascade var ama açık silelim)
    await supabase.from('quiz_leaderboard').delete().eq('user_id', uid)

    // 6. Auth kullanıcısını sil (bu adım quiz_leaderboard cascade'ini de tetikler)
    const { error: deleteError } = await supabase.auth.admin.deleteUser(uid)
    if (deleteError) {
      // Uygulama verisi silinmiş olsa bile auth kaydı silinemedi → hata döndür.
      // İstemci oturumu temizlemez; kullanıcı tekrar deneyebilir.
      console.error('auth.admin.deleteUser hatası:', deleteError.message)
      return json({ error: 'Hesap silinemedi. Lütfen tekrar deneyin.' }, 500)
    }

    // 7. Başarı — istemci bu yanıtı alınca yerel oturumu temizler ve çıkış ekranına gider.
    return json({ deleted: true, user_id: uid }, 200)

  } catch (err) {
    // Hiçbir zaman hata ayrıntısını (token, kullanıcı verisi) dışarıya sızdırma.
    console.error('delete-account beklenmedik hata:', err instanceof Error ? err.message : String(err))
    return json({ error: 'Sunucu hatası' }, 500)
  }
})

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*',
    },
  })
}
