import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { deleteVerifiedAccount } from './cleanup.ts'

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

    // Returned PostgREST errors do not throw automatically; validate every result.
    // Do not delete Auth or claim success after any application-data cleanup failure.
    await deleteVerifiedAccount(supabase, uid)

    // 7. Başarı — istemci bu yanıtı alınca yerel oturumu temizler ve çıkış ekranına gider.
    return json({ deleted: true, user_id: uid }, 200)

  } catch (err) {
    // Hiçbir zaman hata ayrıntısını (token, kullanıcı verisi) dışarıya sızdırma.
    console.error('delete-account cleanup failed')
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
