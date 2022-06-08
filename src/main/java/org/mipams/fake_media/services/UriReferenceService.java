package org.mipams.fake_media.services;

import java.util.Arrays;
import java.util.List;

import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.entities.UriReference;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.services.CoreGeneratorService;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UriReferenceService {

    @Autowired
    Properties properties;

    @Autowired
    CoreGeneratorService coreGeneratorService;

    public void verifyManifestUriReference(JumbfBox jumbfBox, UriReference targetUriReference)
            throws MipamsException {

        String label = jumbfBox.getDescriptionBox().getLabel();

        if (label == null) {
            throw new MipamsException(String.format(ProvenanceErrorMessages.EMPTY_LABEL, "Jumbf Box"));
        }

        if (targetUriReference.getAlgorithm() != UriReference.SUPPORTED_HASH_ALGORITHM) {
            throw new MipamsException(ProvenanceErrorMessages.UNSUPPORTED_HASH_METHOD);
        }

        byte[] computedDigest = getManifestSha256Digest(jumbfBox);

        if (!Arrays.equals(targetUriReference.getDigest(), computedDigest)) {
            throw new MipamsException(
                    String.format(ProvenanceErrorMessages.URI_REFERENCE_DIGEST_MISMATCH,
                            targetUriReference.getUri()));
        }

    }

    public byte[] getManifestSha256Digest(JumbfBox manifestJumbfBox) throws MipamsException {
        String tempFile = CoreUtils.randomStringGenerator();
        String tempFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), tempFile);
        try {
            coreGeneratorService.generateJumbfMetadataToFile(List.of(manifestJumbfBox), tempFilePath);
            return ProvenanceUtils.computeSha256DigestOfFileContents(tempFilePath);
        } finally {
            CoreUtils.deleteFile(tempFilePath);
        }
    }

}
