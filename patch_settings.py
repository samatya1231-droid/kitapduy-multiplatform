import os

toml_path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/gradle/libs.versions.toml"
with open(toml_path, "r", encoding="utf-8") as f:
    toml = f.read()

if "multiplatform-settings-no-arg" not in toml:
    replacement = 'multiplatform-settings-core = { module = "com.russhwolf:multiplatform-settings", version.ref = "multiplatform-settings" }\nmultiplatform-settings-no-arg = { module = "com.russhwolf:multiplatform-settings-no-arg", version.ref = "multiplatform-settings" }'
    toml = toml.replace('multiplatform-settings-core = { module = "com.russhwolf:multiplatform-settings", version.ref = "multiplatform-settings" }', replacement)
    with open(toml_path, "w", encoding="utf-8") as f:
        f.write(toml)
    print("Patched libs.versions.toml")

gradle_path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/build.gradle.kts"
with open(gradle_path, "r", encoding="utf-8") as f:
    gradle = f.read()

if "multiplatform.settings.no.arg" not in gradle:
    replacement = "            implementation(libs.multiplatform.settings.core)\n            implementation(libs.multiplatform.settings.no.arg)"
    gradle = gradle.replace("            implementation(libs.multiplatform.settings.core)", replacement)
    with open(gradle_path, "w", encoding="utf-8") as f:
        f.write(gradle)
    print("Patched build.gradle.kts")
