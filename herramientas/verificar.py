"""Compara herramientas/catalogo.txt (de resumir.js) con fuente/buscador.html.
Muestra marcas mal ubicadas (W), combinaciones a las que les falta un componente (M) y marcas de Cruz Verde que no están en la app."""
import json, re, unicodedata, collections, os
R = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
S = json.load(open(os.path.join(R, 'herramientas', 'stems.json')))
cat = collections.defaultdict(set)
for it in open(os.path.join(R, 'herramientas', 'catalogo.txt')).read().strip().split(';'):
    b, a = it.rsplit('|', 1); cat[b].update(int(x) for x in a.split(','))
h = open(os.path.join(R, 'fuente', 'buscador.html'), encoding='utf-8').read()
s = h.index('const D=[') + len('const D='); e = h.index('];\n', s) + 1
D = json.loads(h[s:e])
C = json.loads(h[h.index('const COMBO=') + 12:h.index(';\n', h.index('const COMBO='))])
SI = {x: i for i, x in enumerate(S)}
nn = lambda x: unicodedata.normalize('NFD', x.lower()).encode('ascii', 'ignore').decode()
stems = lambda n: {SI[w[:7]] for w in re.split(r'[^a-z0-9]+', nn(n)) if len(w) >= 6 and w[:7] in SI}
app = collections.defaultdict(set)
for d in D:
    if d[0].startswith('Marcas sin'): continue
    st = stems(d[0])
    for b in d[2] + d[3]:
        c = re.sub(r'\s*\(.*?\)\s*', ' ', b).strip(); k = nn(c); app[k].update(st)
        if c in C:
            for a, _ in C[c]: app[k].update(stems(a))
ok = 0
for ck, cst in sorted(cat.items()):
    cands = [k for k in app if ck == k or ck.startswith(k + ' ')]
    if not cands: continue
    ak = max(cands, key=len)
    if not app[ak]: continue
    if not (cst & app[ak]): print('W', ck, '->', ak, [S[i] for i in cst], 'app:', [S[i] for i in app[ak]])
    elif cst - app[ak]: print('M', ck, 'le falta', [S[i] for i in cst - app[ak]])
    else: ok += 1
fw = {k.split(' ')[0] for k in app}
print('ok', ok)
print('NO ESTÁN EN LA APP:', sorted({ck for ck in cat if ck.split(' ')[0] not in fw}))
