package org.mipams.provenance.entities.assertions;

import org.mipams.provenance.entities.HashedUriReference;

public class IngredientAssertion implements NonRedactableAssertion {

    public final static String RELATIONSHIP_PARENT_OF = "parentOf";
    public final static String RELATIONSHIP_COMPONENT_OF = "componentOf";

    private String title;
    private String format;
    private String thumbnailURL;
    private String instanceId;
    private String relationship;
    private HashedUriReference manifestReference;
    private String metadata;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public HashedUriReference getManifestReference() {
        return manifestReference;
    }

    public void setManifestReference(HashedUriReference manifestReference) {
        this.manifestReference = manifestReference;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

}
