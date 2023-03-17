package org.mipams.provenance.entities;

import java.util.List;

public class Claim implements ProvenanceEntity {
    private List<HashedUriReference> assertionReferenceList;
    private List<String> redactedAssertionsUriList;
    private List<String> encryptedAssertionUriList;
    private String claimSignatureReference;
    private String claimGeneratorDescription;

    public List<HashedUriReference> getAssertionReferenceList() {
        return assertionReferenceList;
    }

    public void setAssertionReferenceList(List<HashedUriReference> assertionReferenceList) {
        this.assertionReferenceList = assertionReferenceList;
    }

    public List<String> getRedactedAssertionsUriList() {
        return redactedAssertionsUriList;
    }

    public void setRedactedAssertionsUriList(List<String> redactedAssertionsUriList) {
        this.redactedAssertionsUriList = redactedAssertionsUriList;
    }

    public List<String> getEncryptedAssertionUriList() {
        return encryptedAssertionUriList;
    }

    public void setEncryptedAssertionUriList(List<String> encryptedAssertionUriList) {
        this.encryptedAssertionUriList = encryptedAssertionUriList;
    }

    public String getClaimSignatureReference() {
        return claimSignatureReference;
    }

    public void setClaimSignatureReference(String claimSignatureReference) {
        this.claimSignatureReference = claimSignatureReference;
    }

    public String getClaimGeneratorDescription() {
        return claimGeneratorDescription;
    }

    public void setClaimGeneratorDescription(String claimGeneratorDescription) {
        this.claimGeneratorDescription = claimGeneratorDescription;
    }

}
