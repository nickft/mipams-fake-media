package org.mipams.provenance.services.consumer;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.entities.CborBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.services.boxes.JumbfBoxService;
import org.mipams.jumbf.util.CoreUtils;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.provenance.crypto.CryptoException;
import org.mipams.provenance.crypto.CryptoRequest;
import org.mipams.provenance.crypto.CryptoService;
import org.mipams.provenance.entities.Claim;
import org.mipams.provenance.entities.ClaimSignature;
import org.mipams.provenance.entities.ProvenanceErrorMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClaimSignatureConsumer {

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Autowired
    ClaimConsumer claimConsumer;

    @Autowired
    CryptoService cryptoService;

    public Claim validateClaimSignature(String manifestId, JumbfBox claimJumbfBox, JumbfBox claimSignatureJumbfBox)
            throws MipamsException {

        ClaimSignature claimSignature = deserializeClaimSignatureJumbfBox(claimSignatureJumbfBox);

        String claimJumbfFilePath = CoreUtils.createTempFile(CoreUtils.randomStringGenerator(),
                CoreUtils.JUMBF_FILENAME_SUFFIX);

        try (FileOutputStream fos = new FileOutputStream(claimJumbfFilePath)) {
            jumbfBoxService.writeToJumbfFile(claimJumbfBox, fos);

            if (isSignatureValid(claimSignature, claimJumbfFilePath)) {
                return claimConsumer.deserializeClaimJumbfBox(claimJumbfBox);
            } else {
                throw new MipamsException(String.format(ProvenanceErrorMessages.INVALID_SIGNATURE, manifestId));
            }
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.JUMBF_BOX_CREATION_ERROR, e);
        } finally {
            CoreUtils.deleteFile(claimJumbfFilePath);
        }
    }

    public ClaimSignature deserializeClaimSignatureJumbfBox(JumbfBox claimSignatureJumbfBox) throws MipamsException {

        CborBox claimSignatureCborBox = (CborBox) claimSignatureJumbfBox.getContentBoxList().get(0);

        ObjectMapper mapper = new CBORMapper();
        try {
            return mapper.readValue(new ByteArrayInputStream(claimSignatureCborBox.getContent()), ClaimSignature.class);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.CBOR_DESERIALIZE_ERROR, e);
        }
    }

    private boolean isSignatureValid(ClaimSignature claimSignature, String claimJumbfFilePath) throws MipamsException {

        CryptoRequest cryptoRequest = new CryptoRequest();

        cryptoRequest.setContentFileUrl(claimJumbfFilePath);
        cryptoRequest.setCryptoMethod(claimSignature.getAlgorithm());

        String signatureInHex = CoreUtils.convertByteArrayToHex(claimSignature.getSignature());
        cryptoRequest.setSignatureHexEncoded(signatureInHex);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(claimSignature.getPublicKey());

        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");

            KeyPair kp = new KeyPair(kf.generatePublic(spec), null);

            try {
                cryptoService.verifySignatureOfDocument(kp, cryptoRequest);
                return true;
            } catch (CryptoException e) {
                return false;
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new MipamsException(e);
        }
    }
}
