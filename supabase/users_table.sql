-- ==============================================================================
-- Supabase public.users Table Setup
-- Run this script in the Supabase SQL Editor to initialize or repair the users table
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.users (
    uid TEXT PRIMARY KEY,
    name TEXT NOT NULL DEFAULT '',
    username TEXT UNIQUE,
    bio TEXT DEFAULT '',
    photo_url TEXT,
    cover_photo_url TEXT,
    badges TEXT[] DEFAULT '{}',
    my_referral_code TEXT DEFAULT '',
    referred_by TEXT,
    premium_until BIGINT DEFAULT 0,
    successful_invites INTEGER DEFAULT 0,
    streak INTEGER DEFAULT 0,
    is_premium BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable Row Level Security (RLS)
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

-- 1. Read Policy: All authenticated and anonymous users can read public user profiles
DROP POLICY IF EXISTS "Allow public read access to users" ON public.users;
CREATE POLICY "Allow public read access to users"
    ON public.users
    FOR SELECT
    USING (true);

-- 2. Insert Policy: Users can create their own profile row
DROP POLICY IF EXISTS "Allow individual insert access" ON public.users;
CREATE POLICY "Allow individual insert access"
    ON public.users
    FOR INSERT
    WITH CHECK (auth.uid()::text = uid);

-- 3. Update Policy: Users can update their own profile row
DROP POLICY IF EXISTS "Allow individual update access" ON public.users;
CREATE POLICY "Allow individual update access"
    ON public.users
    FOR UPDATE
    USING (auth.uid()::text = uid)
    WITH CHECK (auth.uid()::text = uid);

-- 4. Delete Policy: Users can delete their own profile row
DROP POLICY IF EXISTS "Allow individual delete access" ON public.users;
CREATE POLICY "Allow individual delete access"
    ON public.users
    FOR DELETE
    USING (auth.uid()::text = uid);

-- 5. Automatically maintain updated_at column
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_users_updated_at ON public.users;
CREATE TRIGGER set_users_updated_at
    BEFORE UPDATE ON public.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

-- 6. Automatically provision public.users record when new user signs up in auth.users
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.users (uid, name, username, bio, photo_url)
    VALUES (
        NEW.id::text,
        COALESCE(NULLIF(NEW.raw_user_meta_data->>'full_name', ''), NULLIF(NEW.raw_user_meta_data->>'name', ''), NULLIF(split_part(NEW.email, '@', 1), ''), 'Okur'),
        COALESCE(NULLIF(NEW.raw_user_meta_data->>'username', ''), NULLIF(split_part(NEW.email, '@', 1), ''), 'okur_' || substr(replace(NEW.id::text, '-', ''), 1, 8)),
        'Merhaba, ben yeni bir KitapDuy kullanıcısıyım!',
        COALESCE(NULLIF(NEW.raw_user_meta_data->>'avatar_url', ''), NULLIF(NEW.raw_user_meta_data->>'picture', ''), NULL)
    )
    ON CONFLICT (uid) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();
