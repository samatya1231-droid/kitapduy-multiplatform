import os

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\SplashScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('navigator.replace(OnboardingScreen())', 'navigator.replace(WelcomeScreen())')

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)

print("Updated SplashScreen navigation")
