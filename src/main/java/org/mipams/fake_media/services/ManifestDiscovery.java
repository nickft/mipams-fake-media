package org.mipams.fake_media.services;

import java.util.List;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.fake_media.entities.assertions.Assertion;
import org.mipams.fake_media.entities.assertions.AssertionFactory;
import org.mipams.fake_media.entities.assertions.IngredientAssertion;
import org.mipams.fake_media.services.content_types.ManifestContentType;
import org.mipams.fake_media.services.content_types.StandardManifestContentType;
import org.mipams.fake_media.services.content_types.UpdateManifestContentType;
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

    public ManifestContentType discoverManifestType(List<Assertion> assertionList) throws MipamsException {

        ManifestContentType manifestContentType;

        if (containsRedactableAssertionsOnly(assertionList)
                && containsIngredientAssertionReferencingParentManifest(assertionList)) {
            manifestContentType = updateManifestContentType;
        } else {
            manifestContentType = standardManifestContentType;
        }

        return manifestContentType;
    }

    private boolean containsRedactableAssertionsOnly(List<Assertion> assertionList) throws MipamsException {

        boolean result = true;

        for (Assertion assertion : assertionList) {
            result = result && assertionFactory.isRedactable(assertion);
        }

        return result;
    }

    private boolean containsIngredientAssertionReferencingParentManifest(List<Assertion> assertionList)
            throws MipamsException {

        IngredientAssertion ingredientAssertion = null;

        for (Assertion assertion : assertionList) {
            if (assertionFactory.isIngredientAssertion(assertion)) {
                ingredientAssertion = (IngredientAssertion) assertion;

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
