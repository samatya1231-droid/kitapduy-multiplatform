const fs = require('fs');

let repoPath = 'C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/data/repository/SupabaseBookRepository.kt';
let repoContent = fs.readFileSync(repoPath, 'utf8');

let newPodcasts = fs.readFileSync('C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/scratch2.kt', 'utf8');

// The block to replace
let startIdx = repoContent.indexOf('    private fun getDummyPodcasts(): List<com.semtinazizi.kitapduy.model.Podcast> {');
let endIdx = repoContent.indexOf('    }', startIdx) + 5;

let newRepoContent = repoContent.substring(0, startIdx) + newPodcasts + repoContent.substring(endIdx);
fs.writeFileSync(repoPath, newRepoContent, 'utf8');

// Also update DummyBookRepository
let dummyRepoPath = 'C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/data/repository/DummyBookRepository.kt';
let dummyRepoContent = fs.readFileSync(dummyRepoPath, 'utf8');
startIdx = dummyRepoContent.indexOf('    private fun getDummyPodcasts(): List<com.semtinazizi.kitapduy.model.Podcast> {');
endIdx = dummyRepoContent.indexOf('    }', startIdx) + 5;
if (startIdx !== -1) {
    let newDummyRepoContent = dummyRepoContent.substring(0, startIdx) + newPodcasts + dummyRepoContent.substring(endIdx);
    fs.writeFileSync(dummyRepoPath, newDummyRepoContent, 'utf8');
}

console.log("Updated both repositories successfully!");
