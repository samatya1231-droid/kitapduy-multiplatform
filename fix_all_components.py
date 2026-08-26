import os
import re

components_dir = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\components'

def clean_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        c = f.read()

    # Common bad imports
    c = c.replace('import android.app.Activity', '// import android.app.Activity')
    c = c.replace('import android.widget.Toast', '// import android.widget.Toast')
    c = c.replace('import androidx.activity.compose.BackHandler', '// import androidx.activity.compose.BackHandler')
    c = c.replace('import androidx.lifecycle.Lifecycle', '// import androidx.lifecycle.Lifecycle')
    c = c.replace('import androidx.lifecycle.compose.LifecycleEventEffect', '// import androidx.lifecycle.compose.LifecycleEventEffect')
    c = c.replace('import androidx.compose.ui.platform.LocalContext', '// import androidx.compose.ui.platform.LocalContext')
    c = c.replace('import android.content.Intent', '// import android.content.Intent')
    c = c.replace('import android.net.Uri', '// import android.net.Uri')
    c = c.replace('import android.view.View', '// import android.view.View')
    c = c.replace('import com.google.android.gms.ads.', '// import com.google.android.gms.ads.')
    c = c.replace('import com.google.firebase.firestore.FirebaseFirestore', '// import com.google.firebase.firestore.FirebaseFirestore')
    c = c.replace('import com.valentinilk.shimmer.shimmer', '// import com.valentinilk.shimmer.shimmer')
    c = c.replace('import coil.', 'import coil3.')
    c = c.replace('import coil.compose.', 'import coil3.compose.')
    c = c.replace('.shimmer()', '')
    
    # AdMob stubs
    c = re.sub(r'AdmobBanner\([^)]*\)', 'Box(androidx.compose.ui.Modifier.fillMaxWidth().height(50.dp))', c)
    c = re.sub(r'NativeAdCard\([^)]*\)', 'Box(androidx.compose.ui.Modifier.fillMaxWidth().height(100.dp))', c)
    c = re.sub(r'RewardedAdHelper\([^)]*\)', 'Any()', c)

    # LocalContext
    c = c.replace('val context = LocalContext.current', 'val context: Any? = null')
    c = c.replace('val activity = LocalContext.current as Activity', 'val activity: Any? = null')
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(c)

for file in os.listdir(components_dir):
    if file.endswith('.kt'):
        clean_file(os.path.join(components_dir, file))

print("Cleaned all components.")
