package org.mipams.provenance.services;

import java.util.List;

import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.provenance.entities.assertions.IngredientAssertion;
import org.mipams.provenance.services.AssertionFactory.MipamsAssertion;
import org.mipams.provenance.services.content_types.ManifestContentType;
import org.mipams.provenance.services.content_types.StandardManifestContentType;
import org.mipams.provenance.services.content_types.UpdateManifestContentType;
import org.mipams.provenance.utils.ProvenanceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManifestDiscovery {

    @Autowired
    StandardManifestContentType standardManifestContentType;

    @Autowired
    UpdateManifestContentType updateManifestContentType;

    @Autowired
    AssertionFactory assertionFactory;

    public ManifestContentType discoverManifestType(JumbfBox manifestJumbfBox)
            throws MipamsException {

        ManifestContentType manifestContentType;

        String descriptionUuid = manifestJumbfBox.getDescriptionBox().getUuid();

        if (updateManifestContentType.getContentTypeUuid().equals(descriptionUuid)) {
            manifestContentType = updateManifestContentType;
        } else {
            manifestContentType = standardManifestContentType;
        }

        return manifestContentType;
    }

    public ManifestContentType discoverManifestType(List<JumbfBox> assertionList) throws MipamsException {

        ManifestContentType manifestContentType;

        if (ProvenanceUtils.containsRedactableAssertionsOnly(assertionList)
                && containsIngredientAssertionReferencingParentManifest(assertionList)) {
            manifestContentType = updateManifestContentType;
        } else {
            manifestContentType = standardManifestContentType;
        }

        return manifestContentType;
    }

    private boolean containsIngredientAssertionReferencingParentManifest(List<JumbfBox> assertionList)
            throws MipamsException {

        IngredientAssertion ingredientAssertion = null;
        String label;
        for (JumbfBox assertion : assertionList) {

            label = assertion.getDescriptionBox().getLabel();

            if (MipamsAssertion.getTypeFromLabel(label).equals(MipamsAssertion.INGREDIENT)) {
                ingredientAssertion = (IngredientAssertion) assertionFactory.convertJumbfBoxToAssertion(assertion);

                if (!"parentOf".equals(ingredientAssertion.getRelationship())) {
                    ingredientAssertion = null;
                }
            }
        }

        return ingredientAssertion != null;
    }

    public boolean isUpdateManifestRequest(ManifestContentType manifestContentType) {
        return UpdateManifestContentType.class.equals(manifestContentType.getClass());
    }

    public boolean isStandardManifestRequest(ManifestContentType manifestContentType) {
        return StandardManifestContentType.class.equals(manifestContentType.getClass());
    }
}
