package org.mipams.provenance.services;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import org.mipams.jumbf.entities.BinaryDataBox;
import org.mipams.jumbf.entities.CborBox;
import org.mipams.jumbf.entities.EmbeddedFileDescriptionBox;
import org.mipams.jumbf.entities.JsonBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.entities.JumbfBoxBuilder;
import org.mipams.jumbf.services.content_types.CborContentType;
import org.mipams.jumbf.services.content_types.EmbeddedFileContentType;
import org.mipams.jumbf.services.content_types.JsonContentType;
import org.mipams.jumbf.util.CoreUtils;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.provenance.entities.ProvenanceErrorMessages;
import org.mipams.provenance.entities.ProvenanceMetadata;
import org.mipams.provenance.entities.assertions.ActionAssertion;
import org.mipams.provenance.entities.assertions.Assertion;
import org.mipams.provenance.entities.assertions.BindingAssertion;
import org.mipams.provenance.entities.assertions.ExifMetadataAssertion;
import org.mipams.provenance.entities.assertions.IngredientAssertion;
import org.mipams.provenance.entities.assertions.ThumbnailAssertion;
import org.mipams.provenance.utils.ProvenanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AssertionFactory {

    public enum MipamsAssertion {

        CONTENT_BINDING("mp.binding", false),
        ACTION("mp.actions", false),
        INGREDIENT("mp.ingredient", false),
        THUMBNAIL("mp.thumbnail", false),
        EXIF("stds.exif", true);

        private String label;
        private boolean redactable;

        MipamsAssertion(String field, boolean redactable) {
            this.label = field;
            this.redactable = redactable;
        }

        public static MipamsAssertion getTypeFromLabel(String label) {
            MipamsAssertion result = null;

            if (label == null) {
                return null;
            }

            for (MipamsAssertion type : values()) {

                if (label.startsWith(type.getLabel())) {
                    result = type;
                    break;
                }
            }

            return result;
        }

        public String getLabel() {
            return label;
        }

        public boolean isRedactable() {
            return redactable;
        }

    }

    @Autowired
    CborContentType cborContentType;

    @Autowired
    JsonContentType jsonContentType;

    @Autowired
    EmbeddedFileContentType embeddedFileContentType;

    public String getBaseLabel(Assertion assertion) throws MipamsException {
        MipamsAssertion type = getMipamsAssertionType(assertion);
        return type.getLabel();
    }

    public boolean labelReferencesContentBindingAssertion(String label) {
        return MipamsAssertion.CONTENT_BINDING.getLabel().equals(label);
    }

    public MipamsAssertion getAssertionTypeFromJumbfBox(JumbfBox assertionJumbfBox) throws MipamsException {
        String label = assertionJumbfBox.getDescriptionBox().getLabel();
        MipamsAssertion type = MipamsAssertion.getTypeFromLabel(label);

        return type;
    }

    public boolean isJumbfBoxAnAssertion(JumbfBox assertionJumbfBox) throws MipamsException {
        return getAssertionTypeFromJumbfBox(assertionJumbfBox) != null;
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
            case EXIF:
                result = ProvenanceUtils.deserializeJsonJumbfBox(assertionJumbfBox, ExifMetadataAssertion.class);
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
            case EXIF:
                result = createJsonContentTypeJumbfBox(type.getLabel(), assertion, metadata);
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
        } else if (ExifMetadataAssertion.class.getName().equals(assertionClass)) {
            result = MipamsAssertion.EXIF;
        } else {
            throw new MipamsException(ProvenanceErrorMessages.UNSUPPORTED_ASSERTION);
        }

        return result;
    }

    private JumbfBox createCborContentTypeJumbfBox(String label, Assertion assertion, ProvenanceMetadata metadata)
            throws MipamsException {

        ObjectMapper mapper = new CBORMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);

        try {
            byte[] cborData = mapper.writeValueAsBytes(assertion);
            CborBox cborBox = new CborBox();
            cborBox.setContent(cborData);

            JumbfBoxBuilder builder = new JumbfBoxBuilder(cborContentType);
            builder.setJumbfBoxAsRequestable();
            builder.setLabel(label);
            builder.appendContentBox(cborBox);

            return builder.getResult();
        } catch (JsonProcessingException e) {
            throw new MipamsException(String.format(ProvenanceErrorMessages.SERIALIZATION_ERROR, "Assertion", "CBOR"),
                    e);
        }
    }

    private JumbfBox createJsonContentTypeJumbfBox(String label, Assertion assertion, ProvenanceMetadata metadata)
            throws MipamsException {

        ObjectMapper mapper = new JsonMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);

        try {
            byte[] jsonData = mapper.writeValueAsBytes(assertion);
            JsonBox jsonBox = new JsonBox();
            jsonBox.setContent(jsonData);

            JumbfBoxBuilder builder = new JumbfBoxBuilder(jsonContentType);
            builder.setJumbfBoxAsRequestable();
            builder.setLabel(label);
            builder.appendContentBox(jsonBox);

            return builder.getResult();
        } catch (JsonProcessingException e) {
            throw new MipamsException(String.format(ProvenanceErrorMessages.CONVERTION_ERROR, "Assertion", "JSON"), e);
        }
    }

    private JumbfBox createThumbnailAssertionJumbfBox(String label, ThumbnailAssertion assertion,
            ProvenanceMetadata metadata) throws MipamsException {

        String assertionFilePath = CoreUtils.getFullPath(metadata.getParentDirectory(), assertion.getFileName());

        EmbeddedFileDescriptionBox edbox = new EmbeddedFileDescriptionBox();
        edbox.setFileName(assertion.getFileName());
        edbox.setMediaTypeFromString(assertion.getMediaType());

        BinaryDataBox bdBox = new BinaryDataBox();
        bdBox.setReferencedExternally(false);
        bdBox.setFileUrl(assertionFilePath);

        JumbfBoxBuilder builder = new JumbfBoxBuilder(embeddedFileContentType);
        builder.setJumbfBoxAsRequestable();
        builder.setLabel(label);
        builder.appendContentBox(edbox);
        builder.appendContentBox(bdBox);

        return builder.getResult();
    }
}
