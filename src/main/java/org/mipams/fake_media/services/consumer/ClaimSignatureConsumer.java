package org.mipams.fake_media.services.consumer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.core.entities.CborBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.services.boxes.JumbfBoxService;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.util.Properties;
import org.mipams.jumbf.crypto.entities.CryptoException;
import org.mipams.jumbf.crypto.entities.request.CryptoRequest;
import org.mipams.jumbf.crypto.services.CryptoService;
import org.mipams.fake_media.entities.Claim;
import org.mipams.fake_media.entities.ClaimSignature;
import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClaimSignatureConsumer {

    @Autowired
    Properties properties;

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Autowired
    ClaimConsumer claimConsumer;

    @Autowired
    CryptoService cryptoService;

    public Claim validateClaimSignature(String manifestId, JumbfBox claimJumbfBox, JumbfBox claimSignatureJumbfBox)
            throws MipamsException {

        ClaimSignature claimSignature = deserializeClaimSignatureJumbfBox(claimSignatureJumbfBox);

        String claimJumbfFileName = CoreUtils.randomStringGenerator();
        String claimJumbfFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), claimJumbfFileName);

        try (FileOutputStream fos = new FileOutputStream(claimJumbfFilePath)) {
            jumbfBoxService.writeToJumbfFile(claimJumbfBox, fos);

            if (isSignatureValid(claimSignature, claimJumbfFilePath)) {
                return claimConsumer.desirializeClaimJumbfBox(claimJumbfBox);
            } else {
                throw new MipamsException(String.format(ProvenanceErrorMessages.INVALID_SIGNATURE, manifestId));
            }
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.JUMBF_BOX_CREATION_ERROR, e);
        } finally {
            ProvenanceUtils.deleteFile(claimJumbfFilePath);
        }
    }

    private ClaimSignature deserializeClaimSignatureJumbfBox(JumbfBox claimSignatureJumbfBox) throws MipamsException {

        CborBox claimSignatureCborBox = (CborBox) claimSignatureJumbfBox.getContentBoxList().get(0);

        String cborFilePath = claimSignatureCborBox.getFileUrl();

        ObjectMapper mapper = new CBORMapper();
        try {
            return mapper.readValue(new File(cborFilePath), ClaimSignature.class);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.CBOR_DESERIALIZE_ERROR, e);
        }
    }

    private boolean isSignatureValid(ClaimSignature claimSignature, String claimJumbfFilePath) throws MipamsException {

        CryptoRequest cryptoRequest = new CryptoRequest();

        cryptoRequest.setContentFileUrl(claimJumbfFilePath);
        cryptoRequest.setCryptoMethod(claimSignature.getAlgorithm());

        String signatureInHex = DatatypeConverter.printHexBinary(claimSignature.getSignature());
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
