package se.bitsplz.closebeacon.request;

import android.util.Base64;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * @author jonnakollin
 * @author j0na5L
 */
public final class Encryption {

    private byte[] publicKey;

    public Encryption(String pubKey) {
        this.publicKey = Base64.decode(pubKey, Base64.DEFAULT);
    }

    public String encrypt(byte[] input) throws Exception {
        X509EncodedKeySpec bleKey = new X509EncodedKeySpec(publicKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        PublicKey pubKey = keyFactory.generatePublic(bleKey);
        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);

        return Base64.encodeToString(cipher.doFinal(input), Base64.DEFAULT);
    }
}
