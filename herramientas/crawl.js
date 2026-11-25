// Pegar en la consola de https://www.cruzverde.cl/medicamentos/ DESPUÉS de elegir "Mostrar 48 Productos".
// Recorre todas las páginas con pausas (no apurar: Cruz Verde bloquea si se va muy rápido).
// Resultado en window.__cv2 (href -> nombre del producto). Se guarda también en localStorage '__cv2bak' (bórralo al final).
window.__cv2=JSON.parse(localStorage.getItem('__cv2bak')||'{}');
window.__grab=()=>{for(const a of document.querySelectorAll('main a[href$=".html"]')){const n=a.textContent.trim();if(n)__cv2[a.getAttribute('href')]=n}};
window.__cur=()=>{const a=[...document.querySelectorAll('main .rounded-full')].find(e=>String(e.className).includes('bg-main!'));return a?+a.textContent.trim():0};
window.__crawl1=async()=>{const first=()=>{const a=document.querySelector('main a[href$=".html"]');return a?a.getAttribute('href'):''};
 const waitChange=(b)=>new Promise(res=>{if(first()&&first()!==b)return res(true);const mo=new MutationObserver(()=>{if(first()&&first()!==b){mo.disconnect();res(true)}});mo.observe(document.body,{childList:true,subtree:true});setTimeout(()=>{mo.disconnect();res(first()!==b&&!!first())},25000)});
 const sleep=ms=>new Promise(r=>setTimeout(r,ms));
 __grab();
 while(true){const cur=__cur();const el=[...document.querySelectorAll('main .rounded-full')].filter(e=>e.textContent.trim()===String(cur+1));if(!el.length)return 'fin en '+cur;const b=first();el[0].click();const ok=await waitChange(b);if(!ok)return 'trabado en '+cur;await sleep(1500);__grab();localStorage.setItem('__cv2bak',JSON.stringify(__cv2))}};
window.__done=null;__crawl1().then(r=>window.__done=r,e=>window.__done='err '+e);
// Revisar avance: 'pag '+__cur()+' total '+Object.keys(__cv2).length+' '+__done
