"""Sube el número de versión del contenido para que las apps lo descarguen.
Uso: python3 fuente/publicar.py "texto corto de lo que cambió"
Luego: git add -A && git commit && git push."""
import json, os, sys, datetime
R = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
p = os.path.join(R, 'version.json')
v = json.load(open(p, encoding='utf-8'))
v['v'] = int(v['v']) + 1
v['notas'] = sys.argv[1] if len(sys.argv) > 1 else ''
v['fecha'] = datetime.date.today().isoformat()
json.dump(v, open(p, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
print('version.json ->', v)
