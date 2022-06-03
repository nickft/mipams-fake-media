package org.mipams.fake_media.entities;

public class ProvenanceErrorMessages {

    public static final String NON_REQUESTABLE = "JUMBF Box with label %s needs to be requestable";
    public static final String INVALID_LABEL = "Label mismatch. The correct label is %s";
    public static final String UNLABELED = "JUMBF Box with no label in the Description box";
    public static final String UNSUPPORTED_UUID = "JUMBF Box with content type UUID: %s is not a supported.";
    public static final String EMPTY_MANIFEST_STORE = "Manifest store cannot be empty";
    public static final String EMPTY_LABEL = "%s shall contain a non-empty label";

    public static final String STANDARD_MANIFEST_CONTENT_BINDING = "A Standard Manifest must contain at least one Content Binding Assertion";
    public static final String UPDATE_MANIFEST_CONTENT_BINDING = "An Update Manifest must contain exactly one assertion which is an Ingredient Assertion";
    public static final String CLAIM_INVALID_CONTENT = "Only a CBOR box is allowed in a Claim Content type JUMBF box";
    public static final String MANIFEST_UNIQUE_UUID = "Only one JUMBF Box with content type %s is supported in a Manifest";

    public static final String ASSET_FILE_BINDING_ERROR = "Could not open jumbf file to calculate digest";
    public static final String CLAIM_CBOR_FILE_ERROR = "Failed to create file to store cbor data for %s Jumbf Box";
    public static final String CONVERTION_ERROR = "Failed to convert %s to %s";
    public static final String SERIALIZATION_ERROR = "Failed to serialize %s to %s";
    public static final String UNSUPPORTED_ASSERTION = "Assertion is not supported";
    public static final String UNSUPPORTED_ENCRYPTION = "Unsupported encryption scheme. AES-256 is the only supported encryption scheme for the MIPAMS generator";
    public static final String UNSUPPORTED_SIGNATURE = "Unsupported signature scheme. SHA1withRSA is the only supported encryption scheme for the MIPAMS generator";
    public static final String CRYPTO_SCHEMA_LOAD_ERROR = "Could not load digest schema";
    public static final String JUMBF_BOX_CREATION_ERROR = "Could not create JUMBF file";
    public static final String ENCRYPTION_ERROR = "Failed to encrypt assertion";
    public static final String CERTIFICATE_ENCODING_ERROR = "Could not encode certificate";
    public static final String CLAIM_SIGNING_ERROR = "Could not sign Claim Jumbf Box";
    public static final String MANIFEST_CONTENT_BOX_NOT_FOUND = "Could not locate %s inside the Manifest JUMBF box";
    public static final String INVALID_SIGNATURE = "Signature for Manifest with id %s is invalid";
    public static final String CBOR_DESERIALIZE_ERROR = "Could not desirialize CBOR box";
    public static final String JSON_DESERIALIZE_ERROR = "Could not desirialize JSON box";
    public static final String ASSERTION_STORE_NOT_FOUND = "Assertion Store Jumbf box is not found in Manifest Jumbf Box";
    public static final String CONTENT_BINDING_ASSERTION_NOT_FOUND = "Content Binding Assertion Jumbf box is not found in Manifest Jumbf Box";
    public static final String UNSUPPORTED_HASH_METHOD = "Unsupported Hash function. Only SHA-256 scheme is supported";
    public static final String CONTENT_BINDING_MISMATCH = "Content Binding Mismatch. Asset digest does not match with the asserted digest.";
    public static final String UNREFERENCED_ASSERTION = "Found an Assertion JUMBF box that is not included in the Claim";
    public static final String ASSERTION_DIGEST_MISMATCH = "Assertion with label %s is corrupted. Expected digest in the Claim does not match with the digest of the assertion in the Assertion Store.";

}
