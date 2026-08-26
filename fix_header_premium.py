import re

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components\ModernGreetingSection.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('HeaderPremiumButton(', '/* HeaderPremiumButton(')
content = content.replace('                            )', '                            ) */')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
# Let's fix selectedMoodId. It says 'No value passed for parameter selectedMoodId'. 
# Because I added it to the function but I actually removed it earlier from the caller?
# Wait! In ModernGreetingSection, I added selectedMoodId to the parameter list?
# No, ModernGreetingSection TAKES selectedMoodId, and HomeScreen DOES NOT PASS IT!
content = content.replace('onMoodClick = { mood ->', 'selectedMoodId = null, onMoodClick = { mood ->')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

