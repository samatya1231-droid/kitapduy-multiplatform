import os
import re

src_path = r'C:\Users\Rahmi\AndroidStudioProjects\kitapduy3\app\src\main\java\com\semtinazizi\kitapduy\ui\composables\QuizScreen.kt'
dst_path = r'C:\Users\Rahmi\AndroidStudioProjects\kitapduy-multiplatform\composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\QuizScreen.kt'

with open(src_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update package
content = content.replace('package com.semtinazizi.kitapduy.ui.composables', 'package com.semtinazizi.kitapduy.ui.screens')

# 2. Fix imports
content = content.replace('import androidx.navigation.NavController\n', '')
content = content.replace('import com.semtinazizi.kitapduy.MainActivity\n', '')
content = content.replace('import com.semtinazizi.kitapduy.util.InterstitialAdManager\n', '')
content = content.replace('import com.google.firebase.auth.FirebaseAuth\n', '')
content = content.replace('import com.google.firebase.firestore.FirebaseFirestore\n', '')
content = content.replace('import com.google.firebase.firestore.FieldValue\n', '')
content = content.replace('import com.google.firebase.firestore.SetOptions\n', '')

# 3. Change QuizScreen to Voyager Screen
old_sig = '''@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    navController: NavController,
    matchId: String = "yeni_oyun",
    onBackClick: () -> Unit,
    onGuessAuthorClick: () -> Unit,
    onWatchAdForJoker: (onSuccess: () -> Unit) -> Unit
) {'''

new_sig = '''import cafe.adriel.voyager.core.screen.Screen

class QuizScreen(
    val matchId: String = "yeni_oyun",
    val onBackClick: () -> Unit = {},
    val onGuessAuthorClick: () -> Unit = {},
    val onWatchAdForJoker: (onSuccess: () -> Unit) -> Unit = {}
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {'''

content = content.replace(old_sig, new_sig)

# 4. Remove AdMob/Toast usages
content = re.sub(r'Toast\.makeText\(.*?\)\.show\(\)', 'println("Toast called")', content)
content = re.sub(r'InterstitialAdManager\.loadAd\(.*?\)', '// InterstitialAdManager.loadAd', content)
content = re.sub(r'com\.semtinazizi\.kitapduy\.AppOpenAdManager\.isAdShowing', 'false', content)
content = re.sub(r'com\.semtinazizi\.kitapduy\.AppOpenAdManager\.showAdIfAvailable\(.*?\)', '// show app open ad', content)


# 5. Firebase stubs
content = content.replace('FirebaseAuth.getInstance()', 'null')

# Create a Firebase stub object so it compiles instead of using null which breaks method chains
firebase_stub = '''
object DummyFirebase {
    val currentUser: Any? = null
    fun collection(name: String): DummyCollection = DummyCollection()
}
class DummyCollection {
    fun document(name: String): DummyDocument = DummyDocument()
    fun document(): DummyDocument = DummyDocument()
    fun add(obj: Any): DummyTask = DummyTask()
    fun limit(n: Int): DummyCollection = this
    fun whereEqualTo(k: String, v: Any): DummyCollection = this
    fun get(): DummyTask = DummyTask()
}
class DummyDocument {
    fun set(obj: Any, options: Any? = null): DummyTask = DummyTask()
    fun update(map: Map<String, Any>): DummyTask = DummyTask()
    fun get(): DummyTask = DummyTask()
    fun delete(): DummyTask = DummyTask()
    fun addSnapshotListener(listener: (Any?, Any?) -> Unit): DummyListener = DummyListener()
}
class DummyTask {
    suspend fun await(): Any? = null
    fun addOnSuccessListener(l: (Any?) -> Unit): DummyTask = this
}
class DummyListener {
    fun remove() {}
}
'''
content = content.replace('FirebaseFirestore.getInstance()', 'DummyFirebase')

# We need to add the stub at the top
content = content.replace('import kotlinx.coroutines.launch', 'import kotlinx.coroutines.launch\n' + firebase_stub)

# Fix FieldValue and SetOptions since they were removed from imports
content = content.replace('FieldValue.increment', '10 /* FieldValue.increment */')
content = content.replace('SetOptions.merge()', 'null /* SetOptions.merge() */')

# Fix Task.await() unresolved reference by providing our own await
content = content.replace('import kotlinx.coroutines.tasks.await', '')

# Fix data classes and casting
content = content.replace('toObject(QuizMatch::class.java)', 'null as QuizMatch?')
content = content.replace('toObject(QuizQuestion::class.java)', 'null as QuizQuestion?')
content = content.replace('snapshot.documents.mapNotNull', 'emptyList<Any>().mapNotNull')
content = content.replace('snapshot != null && snapshot.exists()', 'snapshot != null')
content = content.replace('snapshot.getString', '(snapshot as? Map<String, String>)?.get')
content = content.replace('query.documents.firstOrNull()', 'null')
content = content.replace('roomDoc.id', '"dummy_id"')
content = content.replace('snapshot.documents.map', 'emptyList<Any>().map')

# 6. Navigation payload
content = content.replace('navController.navigate(', '// navigate(')
content = content.replace('navController.popBackStack()', '// popBackStack()')

# 7. Context / Activity fixes
content = content.replace('val context = LocalContext.current', 'val context = null')
content = content.replace('val activity = context as? Activity', 'val activity = null')
content = content.replace('(context as? MainActivity)?.billingManager?.launchSubscriptionFlow', '// launch flow')

# 8. Remove ArenaPreview
content = re.sub(r'@Preview\(showBackground = true\)\n@Composable\nfun ArenaPreview\(\) \{[\s\S]*?\}\n?', '', content)

# 9. Add closing brace for Screen
content += '\n}\n'

with open(dst_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('QuizScreen recopied and cleaned via script.')
