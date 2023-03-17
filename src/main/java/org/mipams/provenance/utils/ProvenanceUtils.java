package org.mipams.provenance.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.entities.BinaryDataBox;
import org.mipams.jumbf.entities.BmffBox;
import org.mipams.jumbf.entities.CborBox;
import org.mipams.jumbf.entities.JsonBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.util.CoreUtils;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.provenance.entities.ProvenanceErrorMessages;
import org.mipams.provenance.entities.assertions.Assertion;
import org.mipams.provenance.services.AssertionFactory.MipamsAssertion;
import org.mipams.provenance.services.content_types.ProvenanceContentType;

public class ProvenanceUtils {

    public final static String ASSERTION_LABEL_MULTIPLE_INSTANCE_FORMAT = "%s__%d";

    public static String getProvenanceJumbfURL(String manifestId, String... childFieldList) {

        String[] manifestIdParts = manifestId.split(":");
        manifestIdParts[2] = manifestIdParts[2].toUpperCase();
        StringBuilder result = new StringBuilder("self#jumbf=mipams/").append(String.join(":", manifestIdParts));

        for (String childField : childFieldList) {
            result.append("/").append(childField);
        }

        return result.toString();
    }

    public static byte[] computeSha256DigestOfFileContents(String filePath) throws MipamsException {

        try (InputStream fis = new FileInputStream(filePath)) {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[128];

            while (fis.available() > 0) {
                fis.read(buffer);
                sha.update(buffer);
            }

            return sha.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new MipamsException(ProvenanceErrorMessages.CRYPTO_SCHEMA_LOAD_ERROR, e);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.ASSET_FILE_BINDING_ERROR, e);
        }
    }

    public static Assertion deserializeCborJumbfBox(JumbfBox assertionJumbfBox,
            Class<? extends Assertion> assertionClass)
            throws MipamsException {

        CborBox assertionCborBox = (CborBox) assertionJumbfBox.getContentBoxList().get(0);

        ObjectMapper mapper = new CBORMapper();
        try {
            return mapper.readValue(new ByteArrayInputStream(assertionCborBox.getContent()), assertionClass);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.CBOR_DESERIALIZE_ERROR, e);
        }
    }

    public static Assertion deserializeJsonJumbfBox(JumbfBox assertionJumbfBox,
            Class<? extends Assertion> assertionClass)
            throws MipamsException {

        JsonBox assertionJsonBox = (JsonBox) assertionJumbfBox.getContentBoxList().get(0);

        ObjectMapper mapper = new JsonMapper();
        try {
            return mapper.readValue(new ByteArrayInputStream(assertionJsonBox.getContent()), assertionClass);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.JSON_DESERIALIZE_ERROR, e);
        }
    }

    public static JumbfBox getProvenanceJumbfBox(JumbfBox manifestJumbfBox, ProvenanceContentType contentType)
            throws MipamsException {

        JumbfBox result = null;

        for (BmffBox contentBox : manifestJumbfBox.getContentBoxList()) {
            JumbfBox jumbfBox = (JumbfBox) contentBox;

            if (contentType.getContentTypeUuid().equals(jumbfBox.getDescriptionBox().getUuid())) {
                result = jumbfBox;
            }
        }

        if (result == null) {
            throw new MipamsException(
                    String.format(ProvenanceErrorMessages.MANIFEST_CONTENT_BOX_NOT_FOUND, contentType.getLabel()));
        }

        return result;
    }

    public static JumbfBox locateActiveManifest(JumbfBox manifestStoreJumbfBox) {
        int manifestStoreSize = manifestStoreJumbfBox.getContentBoxList().size();
        return (JumbfBox) manifestStoreJumbfBox.getContentBoxList().get(manifestStoreSize - 1);
    }

    public static JumbfBox locateManifestFromUri(JumbfBox manifestStoreJumbfBox, String targetManifestUri)
            throws MipamsException {

        JumbfBox manifestResult = null;

        if (targetManifestUri == null) {
            return manifestResult;
        }

        String manifestId;
        JumbfBox manifestJumbfBox;

        for (BmffBox contentBox : manifestStoreJumbfBox.getContentBoxList()) {
            manifestJumbfBox = (JumbfBox) contentBox;
            manifestId = manifestJumbfBox.getDescriptionBox().getLabel();

            if (manifestId == null) {
                throw new MipamsException(String.format(ProvenanceErrorMessages.EMPTY_LABEL, "Manifest JUMBF Box"));
            }

            String manifestUri = getProvenanceJumbfURL(manifestId);

            if (targetManifestUri.startsWith(manifestUri)) {
                manifestResult = manifestJumbfBox;
                break;
            }
        }

        return manifestResult;
    }

    public static boolean containsRedactableAssertionsOnly(List<JumbfBox> assertionList) throws MipamsException {

        boolean result = true;
        String label;
        MipamsAssertion type;

        for (JumbfBox assertion : assertionList) {
            label = assertion.getDescriptionBox().getLabel();
            type = MipamsAssertion.getTypeFromLabel(label);

            result = result && type.isRedactable();
        }

        return result;
    }

    public static String issueNewManifestId() {
        return "urn:uuid:" + CoreUtils.randomStringGenerator();
    }

    public static String getCurrentTimeUTC() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    public static BinaryDataBox getBinaryDataBoxWithEntropy() {
        return null;
    }
}