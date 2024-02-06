package org.mipams.provenance.crypto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

@Service
public class CryptoService {
    public String encryptDocument(SecretKey secretKey, CryptoRequest encryptionRequest) throws CryptoException {

        if (encryptionRequest.getContentFileUrl() == null) {
            throw new CryptoException("Content is not specified in the request");
        }

        SymmetricEncryption encScheme = new SymmetricEncryption(secretKey);
        return encScheme.encrypt(encryptionRequest);
    }

    public String decryptDocument(SecretKey secretKey, CryptoRequest encryptionRequest) throws CryptoException {

        if (encryptionRequest.getContentFileUrl() == null) {
            throw new CryptoException("Content is not specified in the request");
        }

        SymmetricEncryption encScheme = new SymmetricEncryption(secretKey);
        return encScheme.decrypt(encryptionRequest);
    }

    public byte[] signDocument(KeyPair credentials, CryptoRequest signRequest) throws CryptoException {

        if (signRequest.getContentFileUrl() == null) {
            throw new CryptoException("Content is not specified in the request");
        }

        ShaSignature signScheme = new ShaSignature(credentials);
        return signScheme.sign(signRequest);
    }

    public String verifySignatureOfDocument(KeyPair credentials, CryptoRequest signRequest) throws CryptoException {

        if (signRequest.getContentFileUrl() == null) {
            throw new CryptoException("Content is not specified in the request");
        }

        if (signRequest.getSignatureHexEncoded() == null) {
            throw new CryptoException("Signature is not provided");
        }

        ShaSignature signScheme = new ShaSignature(credentials);
        return signScheme.verifySignature(signRequest);
    }

    public byte[] getRandomNumber(int numOfBytes) throws CryptoException {
        RandomNumberGenerator rng = new RandomNumberGenerator();
        return rng.getByteArray(numOfBytes);
    }

    public String generatePolicy(String... parameters) throws CryptoException {


    try{    
        try(BufferedReader reader = new BufferedReader(
            new InputStreamReader(CryptoService.class.getClassLoader().getResourceAsStream("policy-template.xml"),
                    StandardCharsets.UTF_8))){
        String policy_template = reader.lines().collect(Collectors.joining("\n"));
        return policy_template;
        }
    } catch (IOException e) {
        throw new CryptoException(e);
    }
}

}