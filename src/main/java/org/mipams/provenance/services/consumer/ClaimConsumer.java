package org.mipams.provenance.services.consumer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.entities.CborBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.provenance.entities.Claim;
import org.mipams.provenance.entities.ProvenanceErrorMessages;
import org.springframework.stereotype.Service;

@Service
public class ClaimConsumer {
    public Claim deserializeClaimJumbfBox(JumbfBox claimJumbfBox) throws MipamsException {

        CborBox claimCborBox = (CborBox) claimJumbfBox.getContentBoxList().get(0);

        ObjectMapper mapper = new CBORMapper();
        try {
            return mapper.readValue(new ByteArrayInputStream(claimCborBox.getContent()), Claim.class);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.CBOR_DESERIALIZE_ERROR, e);
        }
    }
}
