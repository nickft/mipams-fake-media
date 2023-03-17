package org.mipams.provenance.crypto;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mipams.jumbf.util.CoreUtils;

// https://stackoverflow.com/questions/11410770/load-rsa-public-key-from-file

public class ShaSignature {

    private static final Logger logger = Logger.getLogger(ShaSignature.class.getName());

    private KeyPair credentials;

    public KeyPair getCredentials() {
        return this.credentials;
    }

    public void setCredentials(KeyPair keyPair) {
        this.credentials = keyPair;
    }

    public ShaSignature(KeyPair keyPair) {
        setCredentials(keyPair);
    }

    public byte[] sign(CryptoRequest request) throws CryptoException {
        try {
            logger.log(Level.FINE, "Start signing request");

            Signature rsa = Signature.getInstance("SHA1withRSA");

            rsa.initSign(getCredentials().getPrivate());

            byte[] signedPayload = signDigest(rsa, request.getContentFileUrl());

            logger.log(Level.FINE, "Signature request finished");

            return signedPayload;
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    private byte[] signDigest(Signature signature, String inputFile) throws CryptoException {
        try (InputStream inputStream = new FileInputStream(inputFile)) {
            byte[] buffer = new byte[64];

            while (inputStream.available() > 0) {
                inputStream.read(buffer);
                signature.update(buffer);
            }
            return signature.sign();
        } catch (IOException e) {
            throw new CryptoException(e);
        } catch (SignatureException e) {
            throw new CryptoException(e);
        }
    }

    public String verifySignature(CryptoRequest request) throws CryptoException {
        try {
            logger.log(Level.FINE, "Start verifying signature");

            Signature rsa = Signature.getInstance("SHA1withRSA");

            rsa.initVerify(getCredentials().getPublic());

            byte[] signature = CoreUtils.convertHexToByteArray(request.getSignatureHexEncoded());

            if (isSignatureValid(rsa, request.getContentFileUrl(), signature)) {
                logger.log(Level.FINE, "Signature verification finished successfully");
                return "Signature was verified successfully";
            } else {
                throw new CryptoException("Signature is invalid");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        } catch (InvalidKeyException e) {
            throw new CryptoException(e);
        }
    }

    private boolean isSignatureValid(Signature signatureAlgorithm, String inputFile, byte[] signature)
            throws CryptoException {
        try (InputStream inputStream = new FileInputStream(inputFile)) {
            byte[] buffer = new byte[64];

            while (inputStream.available() > 0) {
                inputStream.read(buffer);
                signatureAlgorithm.update(buffer);
            }

            return signatureAlgorithm.verify(signature);
        } catch (IOException e) {
            throw new CryptoException(e);
        } catch (SignatureException e) {
            throw new CryptoException(e);
        }
    }
}