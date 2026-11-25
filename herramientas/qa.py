"""Prueba automática: cada principio activo y cada marca deben encontrar su tarjeta, y no debe haber errores.
Necesita playwright y Chrome. Uso: python3 herramientas/qa.py"""
import json, re, os
from playwright.sync_api import sync_playwright
R = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
h = open(os.path.join(R, 'fuente', 'buscador.html'), encoding='utf-8').read()
s = h.index('const D=[') + len('const D='); e = h.index('];\n', s) + 1
D = json.loads(h[s:e])
cl = lambda x: re.sub(r'\s*\(.*?\)\s*', ' ', x).strip()
with sync_playwright() as p:
    b = p.chromium.launch(executable_path=os.environ.get('CHROME', '/usr/bin/google-chrome'))
    pg = b.new_page(); errs = []; pg.on('pageerror', lambda e: errs.append(str(e)))
    pg.goto('file://' + os.path.join(R, 'app.html'))
    bad = pg.evaluate("""(names)=>{const q=document.getElementById('q');const out=[];for(const n of names){q.value=n;q.dispatchEvent(new Event('input',{bubbles:true}));const c=[...document.querySelectorAll('#list .card h2')].map(x=>x.childNodes[0].textContent.trim());if(c[0]!==n)out.push(n)}return out}""", [d[0] for d in D])
    brands = sorted({cl(x) for d in D if not d[0].startswith('Marcas sin') for x in d[2] + d[3]})
    bad2 = pg.evaluate("""(arr)=>{const q=document.getElementById('q');const out=[];for(const [b,acts] of arr){q.value=b;q.dispatchEvent(new Event('input',{bubbles:true}));const c=[...document.querySelectorAll('#list .card')].map(x=>x.querySelector('h2').childNodes[0].textContent.trim());if(!c.length||!acts.includes(c[0]))out.push(b)}return out}""", [[bb, [d[0] for d in D if any(cl(x) == bb for x in d[2] + d[3])]] for bb in brands])
    print('principios activos:', len(D), 'con problema:', bad)
    print('marcas:', len(brands), 'con problema:', bad2)
    print('errores JS:', errs)
