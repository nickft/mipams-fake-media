package org.mipams.fake_media.entities.assertions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.core.entities.BinaryDataBox;
import org.mipams.jumbf.core.entities.CborBox;
import org.mipams.jumbf.core.entities.EmbeddedFileDescriptionBox;
import org.mipams.jumbf.core.entities.JsonBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.entities.JumbfBoxBuilder;
import org.mipams.jumbf.core.services.content_types.CborContentType;
import org.mipams.jumbf.core.services.content_types.EmbeddedFileContentType;
import org.mipams.jumbf.core.services.content_types.JsonContentType;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.util.Properties;
import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.entities.ProvenanceMetadata;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;

@Service
public class AssertionFactory {

    public enum MipamsAssertion {

        CONTENT_BINDING("mp.binding", false),
        ACTION("mp.actions", false),
        INGREDIENT("mp.ingredient", false),
        THUMBNAIL("mp.thumbnail", true);

        private @Getter String label;
        private @Getter boolean redactable;

        MipamsAssertion(String field, boolean redactable) {
            this.label = field;
            this.redactable = redactable;
        }

        public static MipamsAssertion getTypeFromLabel(String label) {
            MipamsAssertion result = null;

            for (MipamsAssertion type : values()) {

                if (type.getLabel().equals(label)) {
                    result = type;
                    break;
                }
            }

            return result;
        }

    }

    @Autowired
    CborContentType cborContentType;

    @Autowired
    JsonContentType jsonContentType;

    @Autowired
    Properties properties;

    @Autowired
    EmbeddedFileContentType embeddedFileContentType;

    public String getBaseLabel(Assertion assertion) throws MipamsException {
        MipamsAssertion type = getMipamsAssertionType(assertion);
        return type.getLabel();
    }

    public boolean isRedactable(Assertion assertion) throws MipamsException {
        MipamsAssertion type = getMipamsAssertionType(assertion);
        return type.isRedactable();
    }

    public boolean isIngredientAssertion(Assertion assertion) throws MipamsException {
        MipamsAssertion type = getMipamsAssertionType(assertion);
        return type.equals(MipamsAssertion.INGREDIENT);
    }

    public boolean labelReferencesContentBindingAssertion(String label) {
        return MipamsAssertion.CONTENT_BINDING.getLabel().equals(label);
    }

    public Assertion convertJumbfBoxToAssertion(JumbfBox assertionJumbfBox) throws MipamsException {

        Assertion result;

        String label = assertionJumbfBox.getDescriptionBox().getLabel();

        MipamsAssertion type = MipamsAssertion.getTypeFromLabel(label);

        switch (type) {
            case THUMBNAIL:
                result = deserializeThumbnailAssertion(assertionJumbfBox);
                break;
            case CONTENT_BINDING:
                result = ProvenanceUtils.deserializeCborJumbfBox(assertionJumbfBox, BindingAssertion.class);
                break;
            case ACTION:
                result = ProvenanceUtils.deserializeCborJumbfBox(assertionJumbfBox, ActionAssertion.class);
                break;
            case INGREDIENT:
                result = ProvenanceUtils.deserializeCborJumbfBox(assertionJumbfBox, IngredientAssertion.class);
                break;
            default:
                result = null;
                break;
        }

        return result;

    }

    private Assertion deserializeThumbnailAssertion(JumbfBox assertionJumbfBox) {

        EmbeddedFileDescriptionBox edBox = (EmbeddedFileDescriptionBox) assertionJumbfBox.getContentBoxList().get(0);

        ThumbnailAssertion result = new ThumbnailAssertion();
        result.setFileName(edBox.getFileName());
        result.setMediaType(edBox.getMediaType().toString());

        return result;
    }

    public JumbfBox convertAssertionToJumbfBox(Assertion assertion, ProvenanceMetadata metadata)
            throws MipamsException {

        JumbfBox result;

        MipamsAssertion type = getMipamsAssertionType(assertion);

        switch (type) {
            case THUMBNAIL:
                result = createThumbnailAssertionJumbfBox(type.getLabel(), (ThumbnailAssertion) assertion, metadata);
                break;
            case CONTENT_BINDING:
            case ACTION:
            case INGREDIENT:
            default:
                result = createCborContentTypeJumbfBox(type.getLabel(), assertion, metadata);
                break;
        }

        return result;
    }

    private MipamsAssertion getMipamsAssertionType(Assertion assertion) throws MipamsException {

        final String assertionClass = assertion.getClass().getName();

        MipamsAssertion result;

        if (BindingAssertion.class.getName().equals(assertionClass)) {
            result = MipamsAssertion.CONTENT_BINDING;
        } else if (ActionAssertion.class.getName().equals(assertionClass)) {
            result = MipamsAssertion.ACTION;
        } else if (IngredientAssertion.class.getName().equals(assertionClass)) {
            result = MipamsAssertion.INGREDIENT;
        } else if (ThumbnailAssertion.class.getName().equals(assertionClass)) {
            result = MipamsAssertion.THUMBNAIL;
        } else {
            throw new MipamsException(ProvenanceErrorMessages.UNSUPPORTED_ASSERTION);
        }

        return result;
    }

    private JumbfBox createCborContentTypeJumbfBox(String label, Assertion assertion, ProvenanceMetadata metadata)
            throws MipamsException {

        ObjectMapper mapper = new CBORMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);

        String assertionFileName = CoreUtils.randomStringGenerator();
        String assertionFilePath = CoreUtils.getFullPath(metadata.getParentDirectory(), assertionFileName);

        try (OutputStream os = new FileOutputStream(assertionFilePath)) {
            byte[] cborData = mapper.writeValueAsBytes(assertion);

            CoreUtils.writeByteArrayToOutputStream(cborData, os);

            JumbfBoxBuilder builder = new JumbfBoxBuilder();

            builder.setContentType(cborContentType);
            builder.setJumbfBoxAsRequestable();
            builder.setLabel(label);

            CborBox cborBox = new CborBox();
            cborBox.setFileUrl(assertionFilePath);
            builder.appendContentBox(cborBox);

            return builder.getResult();
        } catch (JsonProcessingException e) {
            throw new MipamsException(String.format(ProvenanceErrorMessages.SERIALIZATION_ERROR, "Assertion", "CBOR"),
                    e);
        } catch (IOException e) {
            throw new MipamsException(
                    String.format(ProvenanceErrorMessages.CONVERTION_ERROR, "Assertion", "Claim JUMBF box"), e);
        }
    }

    @SuppressWarnings("unused")
    private JumbfBox createJsonContentTypeJumbfBox(String label, Assertion assertion, ProvenanceMetadata metadata)
            throws MipamsException {

        ObjectMapper mapper = new JsonMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);

        String assertionFileName = CoreUtils.randomStringGenerator();
        String assertionFilePath = CoreUtils.getFullPath(metadata.getParentDirectory(), assertionFileName);

        try (OutputStream os = new FileOutputStream(assertionFilePath)) {
            byte[] jsonData = mapper.writeValueAsBytes(assertion);

            CoreUtils.writeByteArrayToOutputStream(jsonData, os);

            JumbfBoxBuilder builder = new JumbfBoxBuilder();

            builder.setContentType(jsonContentType);
            builder.setJumbfBoxAsRequestable();
            builder.setLabel(label);

            JsonBox jsonBox = new JsonBox();
            jsonBox.setFileUrl(assertionFilePath);
            builder.appendContentBox(jsonBox);

            return builder.getResult();
        } catch (JsonProcessingException e) {
            throw new MipamsException(String.format(ProvenanceErrorMessages.CONVERTION_ERROR, "Assertion", "JSON"), e);
        } catch (IOException e) {
            throw new MipamsException(
                    String.format(ProvenanceErrorMessages.CONVERTION_ERROR, "Assertion", "JSON JUMBF box"), e);
        }
    }

    private JumbfBox createThumbnailAssertionJumbfBox(String label, ThumbnailAssertion assertion,
            ProvenanceMetadata metadata) throws MipamsException {

        String assertionFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), assertion.getFileName());

        JumbfBoxBuilder builder = new JumbfBoxBuilder();

        builder.setContentType(embeddedFileContentType);
        builder.setJumbfBoxAsRequestable();
        builder.setLabel(label);

        EmbeddedFileDescriptionBox edbox = new EmbeddedFileDescriptionBox();

        edbox.setFileName(assertion.getFileName());
        edbox.setMediaTypeFromString(assertion.getMediaType());

        BinaryDataBox bdBox = new BinaryDataBox();
        bdBox.setReferencedExternally(false);
        bdBox.setFileUrl(assertionFilePath);

        builder.appendContentBox(edbox);
        builder.appendContentBox(bdBox);

        return builder.getResult();
    }
}
