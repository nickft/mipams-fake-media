package org.mipams.fake_media.services.producer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.CborBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.util.Properties;
import org.mipams.jumbf.crypto.services.CryptoService;
import org.mipams.jumbf.privacy_security.services.content_types.ProtectionContentType;
import org.mipams.fake_media.entities.Claim;
import org.mipams.jumbf.core.entities.JumbfBoxBuilder;
import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.entities.assertions.AssertionRef;
import org.mipams.fake_media.entities.requests.ProducerRequest;
import org.mipams.fake_media.services.content_types.ClaimContentType;
import org.mipams.fake_media.services.content_types.ClaimSignatureContentType;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClaimProducer {

    @Autowired
    Properties properties;

    @Autowired
    CryptoService cryptoService;

    @Autowired
    ProtectionContentType protectionContentType;

    @Autowired
    AssertionRefProducer assertionRefProducer;

    @Autowired
    AssertionStoreProducer assertionStoreProducer;

    public JumbfBox produce(String manifestId, ProducerRequest producerRequest, JumbfBox assertionStore)
            throws MipamsException {

        List<AssertionRef> assertionHashedUriList = assertionRefProducer
                .getAssertionReferenceListFromAssertionStore(manifestId, assertionStore);

        List<String> redactedAssertionLabelList = assertionStoreProducer
                .getRedactedAssertionsReferenceList(producerRequest.getRedactedAssertionList());

        List<String> encryptedJumbfBoxUriList = getEncryptedAssertionUriList(manifestId, assertionStore);

        Claim claim = new Claim();
        claim.setAssertionReferenceList(assertionHashedUriList);
        claim.setRedactedAssertionsUriList(redactedAssertionLabelList);
        claim.setEncryptedAssertionUriList(encryptedJumbfBoxUriList);
        claim.setClaimGeneratorDescription(producerRequest.getClaimGenerator().getDescription());

        String claimSignatureLabel = (new ClaimSignatureContentType()).getLabel();

        String claimSignatureReference = ProvenanceUtils.getProvenanceJumbfURL(manifestId, claimSignatureLabel);
        claim.setClaimSignatureReference(claimSignatureReference);

        return convertClaimToJumbfBox(claim);
    }

    public List<String> getEncryptedAssertionUriList(String manifestId, JumbfBox assertionStore) {

        List<String> encryptedAssertionBoxLabelList = new ArrayList<>();

        for (BmffBox contentBox : assertionStore.getContentBoxList()) {
            JumbfBox jumbfBox = (JumbfBox) contentBox;

            if (jumbfBox.getDescriptionBox().getType().equals(protectionContentType.getContentTypeUuid())) {

                String assertionStoreLabel = assertionStore.getDescriptionBox().getLabel();
                String assertionLabel = jumbfBox.getDescriptionBox().getLabel();
                String uri = ProvenanceUtils.getProvenanceJumbfURL(manifestId, assertionStoreLabel, assertionLabel);

                encryptedAssertionBoxLabelList.add(uri);
            }
        }

        return encryptedAssertionBoxLabelList;
    }

    private JumbfBox convertClaimToJumbfBox(Claim claim) throws MipamsException {
        ObjectMapper mapper = new CBORMapper();

        String claimFileName = CoreUtils.randomStringGenerator();
        String claimFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), claimFileName);

        try (OutputStream os = new FileOutputStream(claimFilePath)) {
            byte[] cborData = mapper.writeValueAsBytes(claim);

            CoreUtils.writeByteArrayToOutputStream(cborData, os);

            JumbfBoxBuilder builder = new JumbfBoxBuilder();

            ClaimContentType service = new ClaimContentType();
            builder.setContentType(service);
            builder.setJumbfBoxAsRequestable();
            builder.setLabel(service.getLabel());

            CborBox cborBox = new CborBox();
            cborBox.setFileUrl(claimFilePath);
            builder.appendContentBox(cborBox);

            return builder.getResult();
        } catch (JsonProcessingException e) {
            throw new MipamsException(String.format(ProvenanceErrorMessages.SERIALIZATION_ERROR, "Claim", "CBOR"), e);
        } catch (IOException e) {
            throw new MipamsException(
                    String.format(ProvenanceErrorMessages.CONVERTION_ERROR, "Claim", "CBOR JUMBF box"), e);
        }
    }
}
