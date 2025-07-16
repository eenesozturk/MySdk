package com.example.securelibrary;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public class SdkUtils {

    private static final String LICENSE_TEXT = "valid-license";

    public static boolean isLicenseValid(Context context) {
        try {
            // 1. Lisans imzasını oku (license.lic)
            BufferedReader licenseReader = new BufferedReader(new InputStreamReader(context.getAssets().open("license.lic")));
            String base64Signature = licenseReader.readLine();
            licenseReader.close();
            byte[] signatureBytes = Base64.decode(base64Signature, Base64.DEFAULT);

            // 2. Public key'i oku (public_key.pem)
            BufferedReader keyReader = new BufferedReader(new InputStreamReader(context.getAssets().open("public_key.pem")));
            StringBuilder keyBuilder = new StringBuilder();
            String line;
            while ((line = keyReader.readLine()) != null) {
                if (!line.contains("BEGIN PUBLIC KEY") && !line.contains("END PUBLIC KEY")) {
                    keyBuilder.append(line);
                }
            }
            keyReader.close();
            byte[] publicKeyBytes = Base64.decode(keyBuilder.toString(), Base64.DEFAULT);

            // 3. PublicKey nesnesini oluştur
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // 4. İmzayı doğrula
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(LICENSE_TEXT.getBytes());

            boolean isValid = sig.verify(signatureBytes);
            Log.d("LICENSE", "Lisans doğrulama sonucu: " + isValid);
            return isValid;

        } catch (Exception e) {
            Log.e("LICENSE", "Doğrulama hatası: " + e.getMessage(), e);
            return false;
        }
    }

    public static String getWelcomeMessage(Context context) {
        if (!isLicenseValid(context)) return "Lisans geçersiz!";
        return "Welcome from the Secure SDK!";
    }

    public static String getCurrentDate(Context context) {
        if (!isLicenseValid(context)) return "";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    public static String getAppVersion(Context context) {
        if (!isLicenseValid(context)) return "";
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
