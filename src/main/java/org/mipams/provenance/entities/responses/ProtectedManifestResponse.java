package org.mipams.provenance.entities.responses;

import org.mipams.jumbf.entities.JumbfBox;

public class ProtectedManifestResponse implements ManifestResponse{
    private String label;
    private JumbfBox accompaniedAccessRulesBox;
    private boolean integrityStands = false;

    public ProtectedManifestResponse() {

    }

    public ProtectedManifestResponse(String label, JumbfBox accompaniedAccessRulesBox) {
        setAccompaniedAccessRulesBox(accompaniedAccessRulesBox);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public JumbfBox getAccompaniedAccessRulesBox() {
        return accompaniedAccessRulesBox;
    }

    public void setAccompaniedAccessRulesBox(JumbfBox accompaniedAccessRulesBox) {
        this.accompaniedAccessRulesBox = accompaniedAccessRulesBox;
    }

    public boolean integrityStands() {
        return integrityStands;
    }

    public void setIntegrityStands(boolean integrityStands) {
        this.integrityStands = integrityStands;
    }
}
