import re
import os

def regex_replace(path, pattern, new):
    if not os.path.exists(path): return
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    content = re.sub(pattern, new, content)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\PremiumTrialBanner.kt'
regex_replace(path, r'import com\.semtinazizi\.kitapduy\.AppOpenAdManager', '')
regex_replace(path, r'AppOpenAdManager\.isPremiumUser', 'false')
regex_replace(path, r'AppOpenAdManager\.isTemporarilyAdFree\(\)', 'false')
regex_replace(path, r'AppOpenAdManager\.showAdIfNotPremium\(.*?\)', '')
regex_replace(path, r'AppOpenAdManager\.showAdWithCallback\(.*?\)', '')

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\ListeningStatsCard.kt'
regex_replace(path, r'import com\.semtinazizi\.kitapduy\.AppOpenAdManager', '')
regex_replace(path, r'AppOpenAdManager\.isPremiumUser', 'false')
regex_replace(path, r'AppOpenAdManager\.isTemporarilyAdFree\(\)', 'false')
regex_replace(path, r'AppOpenAdManager\.showAdIfNotPremium\(.*?\)', '')
regex_replace(path, r'import com\.semtinazizi\.kitapduy\.util\.com\.semtinazizi\.kitapduy\.util\.KmpBookProgressManager', 'import com.semtinazizi.kitapduy.util.KmpBookProgressManager')

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\ModernGreetingSection.kt'
regex_replace(path, r'HeaderPremiumButton\(onClick = onPremiumClick\)', '/* HeaderPremiumButton */')
regex_replace(path, r'ModernMoodCard\(mood = mood,', 'MoodQuickAccessCard(mood = mood,')

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\MoodDiscovery.kt'
regex_replace(path, r'ModernMoodCard\(mood = mood,', 'MoodQuickAccessCard(mood = mood,')

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'
regex_replace(path, r'selectedMoodId = null,\s*selectedMoodId = null', 'selectedMoodId = null')
regex_replace(path, r'selectedMoodId = null,\s*onMoodClick =', 'onMoodClick =')

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\HeroBookCard.kt'
regex_replace(path, r'if \(darkTheme\) Color\.Black else Color\.White', 'Color.Black')

print("Fixed again.")
