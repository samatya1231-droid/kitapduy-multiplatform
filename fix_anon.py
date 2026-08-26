import os

path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\data\repository\SupabaseAuthRepository.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

c = c.replace('val email = "guest_@kitapduy.app"', 'val email = "guest_@kitapduy.app"')
c = c.replace('username = "misafir_"', 'username = "misafir_"')

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)

print("Fixed guest fallback email")
