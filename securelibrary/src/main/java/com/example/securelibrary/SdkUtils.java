package com.example.securelibrary;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SdkUtils {

    private static final String TAG = "LICENSE";

    public static boolean isLicenseValid(Context context) {
        try {
            // 1. İmzayı oku (ilk satır)
            BufferedReader licenseReader = new BufferedReader(new InputStreamReader(context.getAssets().open("license.lic")));
            String base64Signature = licenseReader.readLine();

            // 2. Lisans içeriğini oku (geri kalan satırlar)
            StringBuilder licenseContentBuilder = new StringBuilder();
            String line;
            while ((line = licenseReader.readLine()) != null) {
                licenseContentBuilder.append(line).append("\n");
            }
            licenseReader.close();

            byte[] signatureBytes = Base64.decode(base64Signature, Base64.DEFAULT);
            String licenseMessage = licenseContentBuilder.toString().trim();

            // 3. Public key'i oku
            BufferedReader keyReader = new BufferedReader(new InputStreamReader(context.getAssets().open("public_key.pem")));
            StringBuilder keyBuilder = new StringBuilder();
            while ((line = keyReader.readLine()) != null) {
                if (!line.contains("BEGIN PUBLIC KEY") && !line.contains("END PUBLIC KEY")) {
                    keyBuilder.append(line);
                }
            }
            keyReader.close();
            byte[] publicKeyBytes = Base64.decode(keyBuilder.toString(), Base64.DEFAULT);

            // 4. PublicKey nesnesi oluştur
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // 5. İmzayı doğrula
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(licenseMessage.getBytes(StandardCharsets.UTF_8)); // ← Önemli düzeltme!
            boolean isValid = sig.verify(signatureBytes);

            if (!isValid) {
                Log.e(TAG, "İmza doğrulaması başarısız.");
                return false;
            }

            // 6. EXPIRES kontrolü
            Map<String, String> licenseMap = parseLicenseFields(licenseMessage);
            String expires = licenseMap.get("EXPIRES");
            if (expires == null) return false;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date expireDate = sdf.parse(expires);
            if (expireDate == null || expireDate.before(new Date())) {
                Log.e(TAG, "Lisans süresi dolmuş.");
                return false;
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Doğrulama hatası: " + e.getMessage(), e);
            return false;
        }
    }

    public static String getWelcomeMessage(Context context) {
        if (!isLicenseValid(context)) return "Lisans geçersiz!";
        return "Welcome from the Secure SDK!";
    }

    public static String getCurrentDate(Context context) {
        if (!isLicenseValid(context)) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    public static String getLicenseInfo(Context context) {
        if (!isLicenseValid(context)) return "Lisans geçersiz!";
        try {
            BufferedReader licenseReader = new BufferedReader(new InputStreamReader(context.getAssets().open("license.lic")));
            licenseReader.readLine(); // İlk satırı atla (imza)
            StringBuilder infoBuilder = new StringBuilder();
            String line;
            while ((line = licenseReader.readLine()) != null) {
                infoBuilder.append(line).append("\n");
            }
            licenseReader.close();
            return infoBuilder.toString().trim();
        } catch (Exception e) {
            Log.e("LICENSE", "Lisans bilgileri okunamadı: " + e.getMessage());
            return "Lisans bilgileri okunamadı.";
        }
    }

    private static Map<String, String> parseLicenseFields(String content) {
        Map<String, String> map = new HashMap<>();
        String[] lines = content.split(";\\s*");
        for (String line : lines) {
            if (line.contains("=")) {
                String[] parts = line.split("=", 2);
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
        return map;
    }
}
