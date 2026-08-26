const fs = require('fs');
const buf = fs.readFileSync('C:/Users/Rahmi/AndroidStudioProjects/kitapduy3/functions/podcasts.json');
const text = buf.toString('utf16le').replace(/^\uFEFF/, '');
const podcasts = JSON.parse(text);

let code = '    private fun getDummyPodcasts(): List<com.semtinazizi.kitapduy.model.Podcast> {\n        return listOf(\n';

podcasts.forEach((p, i) => {
    let name = (p.name || p.title || '').replace(/"/g, '\\"');
    let author = (p.author || '').replace(/"/g, '\\"');
    let rssUrl = (p.rss_url || p.res_url || '').replace(/"/g, '\\"');
    let imageUrl = (p.image_url || '').replace(/"/g, '\\"');
    let category = (p.category || 'Genel').replace(/"/g, '\\"');
    
    code += '            com.semtinazizi.kitapduy.model.Podcast("' + p.id + '", "' + name + '", "' + author + '", "' + rssUrl + '", "' + imageUrl + '", "' + category + '")';
    if(i < podcasts.length - 1) code += ',\n';
});

code += '\n        )\n    }';

fs.writeFileSync('C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/scratch.kt', code, 'utf8');
