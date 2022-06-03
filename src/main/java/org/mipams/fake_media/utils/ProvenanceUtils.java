package org.mipams.fake_media.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.core.entities.CborBox;
import org.mipams.jumbf.core.entities.JsonBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.entities.assertions.Assertion;

public class ProvenanceUtils {
    public static String getProvenanceJumbfURL(String manifestId, String... childFieldList) {

        StringBuilder result = new StringBuilder("self#jumbf=mipams/urn:uuid:").append(manifestId.toUpperCase());

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

    public static void deleteFile(String filePath) {
        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }
    }

    public static Assertion deserializeCborJumbfBox(JumbfBox assertionJumbfBox,
            Class<? extends Assertion> assertionClass)
            throws MipamsException {

        CborBox assertionCborBox = (CborBox) assertionJumbfBox.getContentBoxList().get(0);

        String cborFilePath = assertionCborBox.getFileUrl();

        ObjectMapper mapper = new CBORMapper();
        try {
            return mapper.readValue(new File(cborFilePath), assertionClass);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.CBOR_DESERIALIZE_ERROR, e);
        }
    }

    public static Assertion deserializeJsonJumbfBox(JumbfBox assertionJumbfBox,
            Class<? extends Assertion> assertionClass)
            throws MipamsException {

        JsonBox assertionJsonBox = (JsonBox) assertionJumbfBox.getContentBoxList().get(0);

        String jsonFilePath = assertionJsonBox.getFileUrl();

        ObjectMapper mapper = new JsonMapper();
        try {
            return mapper.readValue(new File(jsonFilePath), assertionClass);
        } catch (IOException e) {
            throw new MipamsException(ProvenanceErrorMessages.JSON_DESERIALIZE_ERROR, e);
        }
    }

}