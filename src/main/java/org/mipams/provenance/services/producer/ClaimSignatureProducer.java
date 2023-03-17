package org.mipams.provenance.services.producer;

import java.io.FileOutputStream;
import java.io.IOException;

import java.security.cert.CertificateEncodingException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.entities.CborBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.services.boxes.JumbfBoxService;
import org.mipams.jumbf.util.CoreUtils;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.jumbf.entities.JumbfBoxBuilder;
import org.mipams.provenance.crypto.CryptoException;
import org.mipams.provenance.crypto.CryptoRequest;
import org.mipams.provenance.crypto.CryptoService;
import org.mipams.provenance.entities.ClaimSignature;
import org.mipams.provenance.entities.ProvenanceErrorMessages;
import org.mipams.provenance.entities.ProvenanceMetadata;
import org.mipams.provenance.entities.ProvenanceSigner;
import org.mipams.provenance.services.content_types.ClaimSignatureContentType;
import org.mipams.provenance.utils.ProvenanceUtils;
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

        claimSignature.setDate(ProvenanceUtils.getCurrentTimeUTC());

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

        try {
            byte[] cborData = mapper.writeValueAsBytes(claimSignature);

            CborBox cborBox = new CborBox();
            cborBox.setContent(cborData);

            ClaimSignatureContentType service = new ClaimSignatureContentType();
            JumbfBoxBuilder builder = new JumbfBoxBuilder(service);

            builder.setJumbfBoxAsRequestable();
            builder.setLabel(service.getLabel());
            builder.appendContentBox(cborBox);

            return builder.getResult();
        } catch (JsonProcessingException e) {
            throw new MipamsException(
                    String.format(ProvenanceErrorMessages.SERIALIZATION_ERROR, "Claim Signature", "CBOR"), e);
        }
    }

}
