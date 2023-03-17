package org.mipams.provenance.services.producer;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.entities.BmffBox;
import org.mipams.jumbf.entities.CborBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.jumbf.entities.JumbfBoxBuilder;
import org.mipams.privsec.services.content_types.ProtectionContentType;
import org.mipams.provenance.crypto.CryptoService;
import org.mipams.provenance.entities.Claim;
import org.mipams.provenance.entities.HashedUriReference;
import org.mipams.provenance.entities.ProvenanceErrorMessages;
import org.mipams.provenance.entities.ProvenanceMetadata;
import org.mipams.provenance.entities.requests.ProducerRequest;
import org.mipams.provenance.services.content_types.ClaimContentType;
import org.mipams.provenance.services.content_types.ClaimSignatureContentType;
import org.mipams.provenance.utils.ProvenanceUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClaimProducer {

    @Autowired
    CryptoService cryptoService;

    @Autowired
    ProtectionContentType protectionContentType;

    @Autowired
    AssertionRefProducer assertionRefProducer;

    @Autowired
    AssertionStoreProducer assertionStoreProducer;

    public JumbfBox produce(String manifestId, ProducerRequest producerRequest, JumbfBox assertionStore,
            ProvenanceMetadata provenanceMetadata) throws MipamsException {

        List<HashedUriReference> assertionHashedUriList = assertionRefProducer
                .getAssertionReferenceListFromAssertionStore(manifestId, assertionStore);

        List<String> encryptedJumbfBoxUriList = getEncryptedAssertionUriList(manifestId, assertionStore);

        Claim claim = new Claim();
        claim.setAssertionReferenceList(assertionHashedUriList);
        claim.setRedactedAssertionsUriList(producerRequest.getRedactedAssertionUriList());
        claim.setEncryptedAssertionUriList(encryptedJumbfBoxUriList);
        claim.setClaimGeneratorDescription(producerRequest.getClaimGenerator().getDescription());

        String claimSignatureLabel = (new ClaimSignatureContentType()).getLabel();

        String claimSignatureReference = ProvenanceUtils.getProvenanceJumbfURL(manifestId, claimSignatureLabel);
        claim.setClaimSignatureReference(claimSignatureReference);

        return convertClaimToJumbfBox(claim, provenanceMetadata);
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

    private JumbfBox convertClaimToJumbfBox(Claim claim, ProvenanceMetadata provenanceMetadata) throws MipamsException {
        ObjectMapper mapper = new CBORMapper();

        try {
            byte[] cborData = mapper.writeValueAsBytes(claim);

            CborBox cborBox = new CborBox();
            cborBox.setContent(cborData);

            ClaimContentType service = new ClaimContentType();
            JumbfBoxBuilder builder = new JumbfBoxBuilder(service);

            builder.setJumbfBoxAsRequestable();
            builder.setLabel(service.getLabel());
            builder.appendContentBox(cborBox);

            return builder.getResult();
        } catch (JsonProcessingException e) {
            throw new MipamsException(String.format(ProvenanceErrorMessages.SERIALIZATION_ERROR, "Claim", "CBOR"), e);
        }
    }
}
