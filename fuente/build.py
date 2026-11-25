"""Genera las versiones de la app a partir de fuente/buscador.html.
Uso: python3 fuente/build.py   (desde la raíz del repositorio)
Crea: app.html (app Android, se descarga sola), index.html (página web) y
fuente/android/app/src/main/assets/index.html (la que va dentro del APK)."""
import re, os
R = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
h = open(os.path.join(R, 'fuente', 'buscador.html'), encoding='utf-8').read()
o = re.sub(r'<link rel="preconnect"[^>]*>\n', '', h)
o = re.sub(r'<link href="https://fonts.googleapis.com[^>]*>\n', '', o)
o = o.replace('<br><span class="grp">Puedes preguntarle a Claude, pero la respuesta no viene verificada.</span><br><button class="btn" id="ask">Preguntar a Claude</button>', '<br><span class="grp">Revisa cómo está escrito o búscalo en el sistema del local.</span>')
old = 'try{await navigator.clipboard.writeText(t);toast(`"${t}" copiado`)}catch(err){toast(t)}'
assert old in o
o = o.replace(old, 'try{if(window.Android&&Android.copiar){Android.copiar(t)}else{await navigator.clipboard.writeText(t)}toast(`"${t}" copiado`)}catch(err){toast(t)}')
assert 'Creado por Thomas Ogaz' in o and '</html>' in o and len(o) > 100000
open(os.path.join(R, 'app.html'), 'w', encoding='utf-8').write(o)
open(os.path.join(R, 'fuente', 'android', 'app', 'src', 'main', 'assets', 'index.html'), 'w', encoding='utf-8').write(o)
p = o.replace('<title>Buscador de marcas</title>', '<title>Buscador de marcas</title>\n<link rel="manifest" href="manifest.json">\n<meta name="theme-color" content="#12355b">\n<link rel="icon" href="icon-192.png">\n<link rel="apple-touch-icon" href="icon-192.png">', 1)
p = p.replace('</body>', '<script>if("serviceWorker" in navigator){navigator.serviceWorker.register("sw.js").catch(()=>{})}</script>\n</body>', 1)
open(os.path.join(R, 'index.html'), 'w', encoding='utf-8').write(p)
print('ok', len(o))
