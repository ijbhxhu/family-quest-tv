package tw.familyquest.tv;

import android.security.keystore.*;
import java.net.Socket;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.cert.X509Certificate;
import java.math.BigInteger;
import java.util.Date;
import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;

final class LocalTls {
    private static final String ALIAS = "family-quest-local-https-ec";
    static X509Certificate certificate;
    static SSLContext create() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
            generator.initialize(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                    // Conscrypt hashes the TLS transcript, then uses NONEwithECDSA to sign the digest.
                    .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
                    .setCertificateSubject(new X500Principal("CN=Family Quest Local TV"))
                    .setCertificateSerialNumber(new BigInteger(120, new SecureRandom()).add(BigInteger.ONE))
                    .setCertificateNotBefore(new Date(System.currentTimeMillis() - 86_400_000L))
                    .setCertificateNotAfter(new Date(System.currentTimeMillis() + 3650L * 86_400_000L)).build());
            generator.generateKeyPair();
        }
        PrivateKey key = (PrivateKey) store.getKey(ALIAS, null);
        certificate = (X509Certificate) store.getCertificate(ALIAS);
        X509KeyManager manager = new X509KeyManager() {
            @Override public String[] getClientAliases(String t, Principal[] p) { return null; }
            @Override public String chooseClientAlias(String[] t, Principal[] p, Socket s) { return null; }
            @Override public String[] getServerAliases(String t, Principal[] p) { return t.equals("EC") ? new String[]{ALIAS} : null; }
            @Override public String chooseServerAlias(String t, Principal[] p, Socket s) { return t.equals("EC") ? ALIAS : null; }
            @Override public X509Certificate[] getCertificateChain(String a) { return ALIAS.equals(a) ? new X509Certificate[]{certificate} : null; }
            @Override public PrivateKey getPrivateKey(String a) { return ALIAS.equals(a) ? key : null; }
        };
        SSLContext ssl = SSLContext.getInstance("TLS"); ssl.init(new KeyManager[]{manager}, null, new SecureRandom()); return ssl;
    }
    static String fingerprint() throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
        StringBuilder s = new StringBuilder(); for (byte b : bytes) s.append(String.format(java.util.Locale.ROOT, "%02X", b)); return s.toString();
    }
}
