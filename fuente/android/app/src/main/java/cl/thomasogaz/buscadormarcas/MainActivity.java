package cl.thomasogaz.buscadormarcas;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Base64;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;
import androidx.core.content.pm.PackageInfoCompat;

import com.google.android.gms.common.moduleinstall.ModuleInstall;
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQ_FOTO = 77;
    private WebView web;
    private boolean paginaLista = false;
    private String pendiente = null;
    private static final String BASE = "https://raw.githubusercontent.com/rafaja3021-hub/buscador-marca/main/";
    private boolean buscando = false;

    public class Puente {
        @JavascriptInterface
        public void copiar(String texto) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("texto", texto));
        }

        @JavascriptInterface
        public void compartir(String texto) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, texto);
            Intent c = Intent.createChooser(i, "Compartir");
            c.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(c);
        }

        @JavascriptInterface
        public void leerTexto() {
            runOnUiThread(MainActivity.this::abrirCamara);
        }

        @JavascriptInterface
        public void recargar() {
            runOnUiThread(() -> { paginaLista = false; web.loadUrl(urlInicial()); });
        }

        @JavascriptInterface
        public void buscarActualizacion() {
            revisarActualizacion(true);
        }

        @JavascriptInterface
        public String versiones() {
            return "app " + versionApp() + " · contenido " + Math.max(versionApp(), prefs().getInt("htmlV", 0));
        }

        @JavascriptInterface
        public void leerCodigo() {
            runOnUiThread(MainActivity.this::escanearCodigo);
        }
    }

    private File archivoFoto() {
        File dir = new File(getCacheDir(), "fotos");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "foto.jpg");
    }

    private Uri uriFoto() {
        return FileProvider.getUriForFile(this, getPackageName() + ".fotos", archivoFoto());
    }

    private void abrirCamara() {
        try {
            File f = archivoFoto();
            if (f.exists()) f.delete();
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, uriFoto());
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i, REQ_FOTO);
        } catch (Exception e) {
            enviar("error", "No se pudo abrir la cámara del celular.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_FOTO) return;
        if (resultCode != RESULT_OK || !archivoFoto().exists() || archivoFoto().length() == 0) {
            enviar("cancel", "");
            return;
        }
        enviar("leyendo", "");
        new Thread(() -> {
            try {
                Uri u = uriFoto();
                Bitmap b0 = cargarFoto(u, 2600);
                int rot = rotacion(u);
                if (rot != 0) {
                    Matrix m = new Matrix();
                    m.postRotate(rot);
                    b0 = Bitmap.createBitmap(b0, 0, 0, b0.getWidth(), b0.getHeight(), m, true);
                }
                final Bitmap bmp = b0;
                InputImage img = InputImage.fromBitmap(bmp, 0);
                runOnUiThread(() -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        .process(img)
                        .addOnSuccessListener(t -> enviarFoto(bmp, t))
                        .addOnFailureListener(e -> enviar("error", "No pude leer el texto. Intenta de nuevo con más luz y más cerca.")));
            } catch (Exception e) {
                enviar("error", "No pude abrir la foto. Intenta de nuevo.");
            }
        }).start();
    }


    private JSONArray caja(Rect r, int w, int h) throws Exception {
        JSONArray a = new JSONArray();
        a.put(r.left / (double) w);
        a.put(r.top / (double) h);
        a.put(r.width() / (double) w);
        a.put(r.height() / (double) h);
        return a;
    }

    private void enviarFoto(Bitmap b, Text t) {
        new Thread(() -> {
            try {
                int w = b.getWidth(), h = b.getHeight();
                float sc = Math.min(1f, 1400f / Math.max(w, h));
                Bitmap chica = sc < 1f ? Bitmap.createScaledBitmap(b, Math.round(w * sc), Math.round(h * sc), true) : b;
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                chica.compress(Bitmap.CompressFormat.JPEG, 72, bo);
                JSONObject o = new JSONObject();
                o.put("img", "data:image/jpeg;base64," + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP));
                o.put("ar", w / (double) h);
                o.put("texto", t.getText());
                JSONArray lineas = new JSONArray();
                for (Text.TextBlock bl : t.getTextBlocks()) {
                    for (Text.Line ln : bl.getLines()) {
                        Rect r = ln.getBoundingBox();
                        if (r == null) continue;
                        JSONObject L = new JSONObject();
                        L.put("t", ln.getText());
                        L.put("b", caja(r, w, h));
                        JSONArray es = new JSONArray();
                        for (Text.Element el : ln.getElements()) {
                            Rect e = el.getBoundingBox();
                            if (e == null) continue;
                            JSONObject E = new JSONObject();
                            E.put("t", el.getText());
                            E.put("b", caja(e, w, h));
                            es.put(E);
                        }
                        L.put("e", es);
                        lineas.put(L);
                    }
                }
                o.put("lineas", lineas);
                enviar("foto", o.toString());
            } catch (Exception e) {
                enviar("texto", t.getText());
            }
        }).start();
    }

    private Bitmap cargarFoto(Uri u, int max) throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(u)) {
            BitmapFactory.decodeStream(in, null, o);
        }
        int s = 1;
        while (Math.max(o.outWidth, o.outHeight) / s > max) s *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = s;
        try (InputStream in = getContentResolver().openInputStream(u)) {
            Bitmap b = BitmapFactory.decodeStream(in, null, o2);
            if (b == null) throw new Exception("foto vacia");
            return b;
        }
    }

    private int rotacion(Uri u) {
        try (InputStream in = getContentResolver().openInputStream(u)) {
            ExifInterface ex = new ExifInterface(in);
            int or = ex.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (or == ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (or == ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (or == ExifInterface.ORIENTATION_ROTATE_270) return 270;
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void escanearCodigo() {
        try {
            GmsBarcodeScannerOptions op = new GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build();
            GmsBarcodeScanner sc = GmsBarcodeScanning.getClient(this, op);
            ModuleInstallRequest req = ModuleInstallRequest.newBuilder().addApi(sc).build();
            ModuleInstall.getClient(this).installModules(req)
                    .addOnSuccessListener(r -> {
                        if (r.areModulesAlreadyInstalled()) iniciarEscaneo(sc);
                        else enviar("error", "Se está preparando el lector de códigos (solo pasa la primera vez y necesita internet). Espera un minuto y vuelve a intentar.");
                    })
                    .addOnFailureListener(e -> iniciarEscaneo(sc));
        } catch (Exception e) {
            enviar("error", "No pude abrir el lector de códigos.");
        }
    }

    private void iniciarEscaneo(GmsBarcodeScanner sc) {
        sc.startScan()
                .addOnSuccessListener(b -> {
                    String v = b.getRawValue();
                    if (v == null) v = b.getDisplayValue();
                    enviar("codigo", v == null ? "" : v);
                })
                .addOnCanceledListener(() -> enviar("cancel", ""))
                .addOnFailureListener(e -> enviar("error", "No pude leer el código. Revisa que tengas internet la primera vez y vuelve a intentar."));
    }


    private SharedPreferences prefs() {
        return getSharedPreferences("buscador", MODE_PRIVATE);
    }

    private int versionApp() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return (int) PackageInfoCompat.getLongVersionCode(pi);
        } catch (Exception e) {
            return 0;
        }
    }

    private File htmlLocal() {
        return new File(new File(getFilesDir(), "web"), "index.html");
    }

    private String urlInicial() {
        File f = htmlLocal();
        if (f.exists() && prefs().getInt("htmlV", 0) > versionApp()) return Uri.fromFile(f).toString();
        return "file:///android_asset/index.html";
    }

    private String bajar(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(20000);
        c.setUseCaches(false);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) > 0) bo.write(b, 0, n);
            return new String(bo.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }

    private void revisarActualizacion(boolean manual) {
        if (buscando) return;
        buscando = true;
        new Thread(() -> {
            try {
                JSONObject v = new JSONObject(bajar(BASE + "version.json?t=" + System.currentTimeMillis()));
                int nueva = v.optInt("v", 0);
                int min = v.optInt("min", 0);
                int actual = Math.max(versionApp(), prefs().getInt("htmlV", 0));
                if (min > versionApp()) {
                    enviarUpd("apk", v.optString("apk", BASE));
                } else if (nueva > actual) {
                    String html = bajar(BASE + "app.html?t=" + System.currentTimeMillis());
                    if (html.length() > 100000 && html.contains("Creado por Thomas Ogaz") && html.contains("</html>")) {
                        File f = htmlLocal();
                        File dir = f.getParentFile();
                        if (!dir.exists()) dir.mkdirs();
                        File tmp = new File(dir, "nuevo.html");
                        try (FileOutputStream fo = new FileOutputStream(tmp)) {
                            fo.write(html.getBytes(StandardCharsets.UTF_8));
                        }
                        if (f.exists()) f.delete();
                        tmp.renameTo(f);
                        prefs().edit().putInt("htmlV", nueva).apply();
                        enviarUpd("listo", v.optString("notas", ""));
                    } else if (manual) {
                        enviarUpd("error", "");
                    }
                } else if (manual) {
                    enviarUpd("aldia", "");
                }
            } catch (Exception e) {
                if (manual) enviarUpd("sinred", "");
            } finally {
                buscando = false;
            }
        }).start();
    }

    private void enviarUpd(String tipo, String texto) {
        String js = "window.onUpdate&&window.onUpdate(" + JSONObject.quote(tipo) + "," + JSONObject.quote(texto) + ")";
        runOnUiThread(() -> {
            if (web != null && paginaLista) web.evaluateJavascript(js, null);
            else pendiente = (pendiente == null ? "" : pendiente + ";") + js;
        });
    }

    private void enviar(String tipo, String texto) {
        String js = "window.onCamara&&window.onCamara(" + JSONObject.quote(tipo) + "," + JSONObject.quote(texto) + ")";
        runOnUiThread(() -> {
            if (web != null && paginaLista) web.evaluateJavascript(js, null);
            else pendiente = (pendiente == null ? "" : pendiente + ";") + js;
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try {
                    Intent i = u.getScheme() != null && u.getScheme().equals("mailto")
                        ? new Intent(Intent.ACTION_SENDTO, u) : new Intent(Intent.ACTION_VIEW, u);
                    startActivity(i);
                } catch (Exception e) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                paginaLista = true;
                if (pendiente != null) {
                    String js = pendiente;
                    pendiente = null;
                    v.evaluateJavascript(js, null);
                }
            }
        });
        web.addJavascriptInterface(new Puente(), "Android");
        setContentView(web);
        web.loadUrl(urlInicial());
        revisarActualizacion(false);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
