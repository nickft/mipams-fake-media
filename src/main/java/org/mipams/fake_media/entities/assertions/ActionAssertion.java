package org.mipams.fake_media.entities.assertions;

public class ActionAssertion implements NonRedactableAssertion {
    private String action;
    private String date;
    private String metadata;
    private String softwareAgent;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getSoftwareAgent() {
        return softwareAgent;
    }

    public void setSoftwareAgent(String softwareAgent) {
        this.softwareAgent = softwareAgent;
    }
}
