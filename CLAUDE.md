# Buscador de marcas — instrucciones de mantenimiento

App de Thomas Ogaz (auxiliar de farmacia en Cruz Verde, Chile). Escribe un principio activo o una marca y muestra las marcas chilenas, mg, condición de venta, "para qué sirve", combinaciones, versiones, calculadora, cámara (leer texto y códigos) y opciones.

Thomas no sabe de computación: háblale en español, simple, con pasos cortos. Firma visible de la app: "Creado por Thomas Ogaz" (no cambiar).

## Archivos

- `fuente/buscador.html` — **archivo maestro**. Todo se edita aquí.
- `fuente/build.py` — genera `app.html` (la descargan las apps Android), `index.html` (página web) y `fuente/android/app/src/main/assets/index.html` (va dentro del APK).
- `fuente/publicar.py "notas"` — sube `v` en `version.json` para que las apps se actualicen solas.
- `version.json` — `v`: versión del contenido; `min`: versionCode mínimo del APK que puede usar este contenido; `apk`: link de descarga del APK.
- `BuscadorMarcas.apk` — último APK.
- `fuente/android/` — proyecto Android (Java, WebView). En el PC de Thomas está en `C:\Users\ogaz3\Desktop\BuscadorMarcasApp` (la raíz del proyecto es esa carpeta, NO la subcarpeta `BuscadorApp`).
- `herramientas/` — `crawl.js` + `resumir.js` (catálogo de Cruz Verde), `verificar.py`, `qa.py`, `stems.json`.

La app Android revisa https://raw.githubusercontent.com/rafaja3021-hub/buscador-marca/main/version.json al abrirse; si `v` es mayor que la que tiene, baja `app.html` y ofrece "Actualizar".

## Datos dentro de buscador.html

- `D` = lista de `[principio activo, grupo, marcas Cruz Verde[], otras marcas[], venta, nota, bio(0/1)]`.
- `USO` (activo → para qué sirve), `XUSO` (componentes sin entrada propia), `MG` (marca → dosis), `COMBO` (marca → [[activo, dosis]...]), `VARS`/`PRES` (versiones), `CUSE_OVR` (para qué sirve de una combinación).
- Al cargar, las combinaciones de `COMBO` se agregan solas a cada componente (sección "Combinado con otros principios activos"). No repetirlas a mano en las listas de otro activo.
- **Descripciones "para qué sirve" SIN nombres de enfermedades** (ej. "Baja la presión arterial", no "para la hipertensión").
- No inventar principios activos: si no se puede confirmar, dejar la marca en "Marcas sin principio activo en el nombre".
- Nunca copiar fotos de cajas de las farmacias (copyright); la app dibuja una caja genérica.

## Revisión semanal (tarea programada)

1. `git pull`.
2. Catálogo de Cruz Verde (necesita el computador de Thomas prendido con la app de Claude abierta; usar el navegador integrado):
   abrir https://www.cruzverde.cl/medicamentos/, elegir "Mostrar 48 Productos", pegar `herramientas/crawl.js` con la herramienta de JavaScript, esperar (revisar avance), luego `resumir.js` con stems.json, copiar el resultado a `herramientas/catalogo.txt`, correr `python3 herramientas/verificar.py`. Ir lento (Cruz Verde muestra "No se han encontrado resultados" si se va muy rápido; si pasa, esperar y recargar). Al final `localStorage.removeItem('__cv2bak')`.
   Si el computador no responde, saltar este paso y seguir.
3. Corregir lo que salga en W (mal ubicadas) y M (falta componente). Agregar marcas nuevas solo si el nombre del producto dice el principio activo.
4. `python3 fuente/build.py` y `python3 herramientas/qa.py` (todo debe salir sin problemas).
5. Si hubo cambios: `python3 fuente/publicar.py "resumen corto"`, commit y push a `main`. Republicar también el artifact de claude.ai si la sesión tiene las herramientas (slug `48db4ca0-6ecf-4694-aee3-bff6445a191c`).
6. Mandar a Thomas un resumen corto en español: qué marcas se agregaron o corrigieron. Si no hubo cambios, decirlo en una línea.

## Si hay que hacer un APK nuevo (solo cuando cambia el Java)

- `versionCode` del APK debe ser MAYOR que el `v` actual de `version.json`. Después poner `v` = ese versionCode y, si el contenido nuevo usa funciones Java nuevas, `min` = ese versionCode (las apps viejas mostrarán "Hay una versión nueva de la app para instalar").
- Compilar en Android Studio del PC (acceso solo de clic): ventana Build → botón "Restart". El APK queda en `app\build\outputs\apk\debug\app-debug.apk`. Copiarlo a `BuscadorMarcas.apk` en el repo.
