// Después de crawl.js: reduce el catálogo a "marca|índices de principios activos" usando stems.json.
// Reemplaza STEMS por el contenido de herramientas/stems.json unido con espacios.
const S=STEMS.split(" ");
const SI=new Map(S.map((s,i)=>[s,i]));const nf=s=>s.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g,'');
const FORM=/^(comprimidos?|capsulas?|jarabe|solucion|suspension|crema|gel|gotas|spray|ampollas?|inyectable|sobres?|tabletas?|unguento|locion|polvo|grageas?|ovulos?|parches?|comp|cap|caps|x|mg|ml|gr|g|mcg|recubiertos?|oral|topica|oftalmica|nasal|bioequivalente|be|com|cpr|tabs)$/;
const out=new Set();for(const n of Object.values(__cv2)){const w=nf(n).split(/[^a-z0-9.,%\/-]+/).filter(Boolean);const br=[];const acts=new Set();let stop=false;for(const t of w){const t2=t.replace(/[^a-z]/g,'');const st=t2.length>=6?SI.get(t2.slice(0,7)):undefined;if(st!==undefined){acts.add(st);stop=true;continue}if(!stop){if(/\d/.test(t)||FORM.test(t2)||br.length>=3)stop=true;else br.push(t)}}if(acts.size&&br.length)out.add(br.join(' ')+'|'+[...acts].sort((a,b)=>a-b).join(','))}
window.__res=[...out];__res.length
// Copiar __res.join(';') a herramientas/catalogo.txt y correr verificar.py
