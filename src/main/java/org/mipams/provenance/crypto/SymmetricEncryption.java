package org.mipams.provenance.crypto;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

import org.mipams.jumbf.util.CoreUtils;

public class SymmetricEncryption {

    private static final Logger logger = Logger.getLogger(SymmetricEncryption.class.getName());

    private SecretKey symmetricKey;

    public SecretKey getSymmetricKey() {
        return this.symmetricKey;
    }

    public void setSymmetricKey(SecretKey symmetricKey) {
        this.symmetricKey = symmetricKey;
    }

    public SymmetricEncryption(SecretKey symmetricKey) {
        setSymmetricKey(symmetricKey);
    }

    public String encrypt(CryptoRequest request) throws CryptoException {

        logger.log(Level.FINE, "Encryption request instantiated");

        Cipher cipher = getCipherInstance(Cipher.ENCRYPT_MODE, request.getIv());

        AlgorithmParameters algParams = cipher.getParameters();

        logger.info(algParams.toString());

        String outputFileUrl = request.getContentFileUrl() + ".enc";

        performCipherAndWriteToFileUsing(cipher, request.getContentFileUrl(), outputFileUrl);

        logger.log(Level.FINE, "Encryption request finished");

        return outputFileUrl;
    }

    public String decrypt(CryptoRequest request) throws CryptoException {

        logger.log(Level.FINE, "Decryption request instantiated");

        Cipher cipher = getCipherInstance(Cipher.DECRYPT_MODE, request.getIv());

        String outputFileUrl = request.getContentFileUrl() + ".dec";

        performCipherAndWriteToFileUsing(cipher, request.getContentFileUrl(), outputFileUrl);

        logger.log(Level.FINE, "Decryption request finished");

        return outputFileUrl;
    }

    private Cipher getCipherInstance(int opMode, String ivAsString) throws CryptoException {

        Cipher cipher;

        if (ivAsString == null) {
            throw new CryptoException("IV must be specified");
        }

        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            byte[] iv = CoreUtils.convertHexToByteArray(ivAsString);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("AES");

            algorithmParameters.init(ivSpec);

            cipher.init(opMode, getSymmetricKey(), algorithmParameters);
            return cipher;

        } catch (NoSuchPaddingException e) {
            throw new CryptoException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (InvalidKeyException e) {
            throw new CryptoException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new CryptoException(e);
        } catch (InvalidParameterSpecException e) {
            throw new CryptoException(e);
        }
    }

    private void performCipherAndWriteToFileUsing(Cipher cipher, String inputFile,
            String outputFilePath) throws CryptoException {

        try (FileOutputStream fos = new FileOutputStream(outputFilePath);
                CipherInputStream cis = new CipherInputStream(new FileInputStream(inputFile), cipher);) {
            byte[] b = new byte[64];
            int i = cis.read(b);
            while (i != -1) {
                fos.write(b, 0, i);
                i = cis.read(b);
            }
        } catch (IOException e) {
            throw new CryptoException(e);
        }
    }

}