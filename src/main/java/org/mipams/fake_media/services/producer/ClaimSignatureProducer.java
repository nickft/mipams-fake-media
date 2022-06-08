package org.mipams.fake_media.services.producer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.security.cert.CertificateEncodingException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.core.entities.CborBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.services.boxes.JumbfBoxService;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.crypto.entities.CryptoException;
import org.mipams.jumbf.crypto.entities.request.CryptoRequest;
import org.mipams.jumbf.crypto.services.CryptoService;
import org.mipams.fake_media.entities.ClaimSignature;
import org.mipams.jumbf.core.entities.JumbfBoxBuilder;
import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.entities.ProvenanceMetadata;
import org.mipams.fake_media.entities.ProvenanceSigner;
import org.mipams.fake_media.services.content_types.ClaimSignatureContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClaimSignatureProducer {

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Autowired
    CryptoService cryptoService;

    public JumbfBox produce(ProvenanceSigner signer, JumbfBox claimJumbfBox, ProvenanceMetadata provenanceMetadata)
            throws MipamsException {

        ClaimSignature claimSignature = new ClaimSignature();

        claimSignature.setAlgorithm(signer.getSigningScheme());

        try {
            claimSignature.setCertificate(signer.getSigningCertificate().getEncoded());
        } catch (CertificateEncodingException e) {
            throw new MipamsException(ProvenanceErrorMessages.CERTIFICATE_ENCODING_ERROR, e);
        }

        claimSignature.setPublicKey(signer.getSigningCredentials().getPublic().getEncoded());

        byte[] signature = signClaimJumbfBox(signer, claimJumbfBox, provenanceMetadata);

        claimSignature.setSignature(signature);

        return convertClaimSignatureToJumbfBox(claimSignature, provenanceMetadata);
    }

    private byte[] signClaimJumbfBox(ProvenanceSigner signer, JumbfBox claimJumbfBox,
            ProvenanceMetadata provenanceMetadata) throws MipamsException {

        String claimJumbfFileName = CoreUtils.randomStringGenerator();
        String claimJumbfFilePath = CoreUtils.getFullPath(provenanceMetadata.getParentDirectory(), claimJumbfFileName);

        try (FileOutputStream fos = new FileOutputStream(claimJumbfFilePath)) {
            jumbfBoxService.writeToJumbfFile(claimJumbfBox, fos);

            return signDocument(signer, claimJumbfFilePath);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.JUMBF_BOX_CREATION_ERROR, e);
        } finally {
            CoreUtils.deleteFile(claimJumbfFilePath);
        }
    }

    private byte[] signDocument(ProvenanceSigner signer, String claimJumbfFilePath) throws MipamsException {
        if (!signer.getSigningScheme().equals("SHA1withRSA")) {
            throw new MipamsException(ProvenanceErrorMessages.UNSUPPORTED_SIGNATURE);
        }

        CryptoRequest signRequest = new CryptoRequest();

        signRequest.setCryptoMethod(signer.getSigningScheme());
        signRequest.setContentFileUrl(claimJumbfFilePath);

        try {
            return cryptoService.signDocument(signer.getSigningCredentials(), signRequest);
        } catch (CryptoException e) {
            throw new MipamsException(ProvenanceErrorMessages.CLAIM_SIGNING_ERROR, e);
        }
    }

    private JumbfBox convertClaimSignatureToJumbfBox(ClaimSignature claimSignature,
            ProvenanceMetadata provenanceMetadata) throws MipamsException {
        ObjectMapper mapper = new CBORMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);

        String claimSignatureFileName = CoreUtils.randomStringGenerator();
        String claimSignatureFilePath = CoreUtils.getFullPath(provenanceMetadata.getParentDirectory(),
                claimSignatureFileName);

        try (OutputStream os = new FileOutputStream(claimSignatureFilePath)) {
            byte[] cborData = mapper.writeValueAsBytes(claimSignature);

            CoreUtils.writeByteArrayToOutputStream(cborData, os);

            JumbfBoxBuilder builder = new JumbfBoxBuilder();

            ClaimSignatureContentType service = new ClaimSignatureContentType();
            builder.setContentType(service);
            builder.setJumbfBoxAsRequestable();
            builder.setLabel(service.getLabel());

            CborBox cborBox = new CborBox();
            cborBox.setFileUrl(claimSignatureFilePath);
            builder.appendContentBox(cborBox);

            return builder.getResult();
        } catch (JsonProcessingException e) {
            throw new MipamsException(
                    String.format(ProvenanceErrorMessages.SERIALIZATION_ERROR, "Claim Signature", "CBOR"), e);
        } catch (IOException e) {
            throw new MipamsException(
                    String.format(ProvenanceErrorMessages.SERIALIZATION_ERROR, "Claim Signature", "CBOR JUMBF box"), e);
        }
    }

}
