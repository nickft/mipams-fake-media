package org.mipams.fake_media.services.consumer;

import java.util.ArrayList;
import java.util.List;

import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.entities.UriReference;
import org.mipams.fake_media.entities.assertions.IngredientAssertion;
import org.mipams.fake_media.entities.responses.ManifestStoreResponse;
import org.mipams.fake_media.services.AssertionFactory;
import org.mipams.fake_media.services.ManifestDiscovery;
import org.mipams.fake_media.services.UriReferenceService;
import org.mipams.fake_media.services.AssertionFactory.MipamsAssertion;
import org.mipams.fake_media.services.content_types.AssertionStoreContentType;
import org.mipams.fake_media.services.content_types.ManifestContentType;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.util.MipamsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManifestStoreConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ManifestStoreConsumer.class);

    @Autowired
    ManifestConsumer manifestConsumer;

    @Autowired
    UriReferenceService uriReferenceService;

    @Autowired
    ManifestDiscovery manifestDiscovery;

    @Autowired
    AssertionFactory assertionFactory;

    @Autowired
    AssertionStoreConsumer assertionStoreConsumer;

    public ManifestStoreResponse consumeFullManifestStore(JumbfBox manifestStoreJumbfBox, String assetUrl)
            throws MipamsException {

        logger.debug("Consuming Entire Manifest Store");

        ManifestStoreResponse manifestStoreResponse = consumeActiveManifest(manifestStoreJumbfBox, assetUrl);

        JumbfBox activeManifestJumbfBox = ProvenanceUtils.locateActiveManifest(manifestStoreJumbfBox);

        List<UriReference> manifestIdToBeChecked = getIngredientManifestIdReferenceList(activeManifestJumbfBox);

        UriReference currentManifestReference;
        JumbfBox currentManifestJumbfBox;

        while (manifestIdToBeChecked.size() > 0) {
            currentManifestReference = manifestIdToBeChecked.remove(0);
            logger.debug("Consuming manifest with Id " + currentManifestReference.getUri());

            currentManifestJumbfBox = ProvenanceUtils.locateManifestFromUri(manifestStoreJumbfBox,
                    currentManifestReference.getUri());

            uriReferenceService.verifyManifestUriReference(currentManifestJumbfBox, currentManifestReference);

            manifestConsumer.verifyManifestIntegrity(currentManifestJumbfBox);

            manifestStoreResponse.addManifestResponse(currentManifestJumbfBox);

            manifestIdToBeChecked.addAll(getIngredientManifestIdReferenceList(currentManifestJumbfBox));
        }

        return manifestStoreResponse;
    }

    public ManifestStoreResponse consumeActiveManifest(JumbfBox manifestStoreJumbfBox, String assetUrl)
            throws MipamsException {

        logger.debug("Consuming Active Manifest");

        ManifestStoreResponse manifestStoreResponse = new ManifestStoreResponse();

        JumbfBox activeManifestJumbfBox = ProvenanceUtils.locateActiveManifest(manifestStoreJumbfBox);
        ManifestContentType contentType = manifestDiscovery.discoverManifestType(activeManifestJumbfBox);

        if (manifestDiscovery.isStandardManifestRequest(contentType)) {
            manifestConsumer.verifyManifestIntegrityAndContentBinding(activeManifestJumbfBox, assetUrl);
        } else {
            manifestConsumer.verifyManifestIntegrity(activeManifestJumbfBox);

            UriReference parentUriReference = locateParentIngredientReferenceFromManifest(activeManifestJumbfBox);

            if (parentUriReference == null) {
                throw new MipamsException(ProvenanceErrorMessages.UPDATE_MANIFEST_CONTENT_BINDING);
            }

            JumbfBox parentStandardManifest = ProvenanceUtils.locateManifestFromUri(manifestStoreJumbfBox,
                    parentUriReference.getUri());

            uriReferenceService.verifyManifestUriReference(parentStandardManifest, parentUriReference);

            manifestConsumer.verifyManifestIntegrityAndContentBinding(parentStandardManifest, assetUrl);
        }

        manifestStoreResponse.addManifestResponse(activeManifestJumbfBox);

        return manifestStoreResponse;
    }

    private UriReference locateParentIngredientReferenceFromManifest(JumbfBox manifestJumbfBox) throws MipamsException {

        JumbfBox assertionStoreJumbfBox = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox,
                new AssertionStoreContentType());

        UriReference manifestReference = null;

        for (BmffBox contentBox : assertionStoreJumbfBox.getContentBoxList()) {

            manifestReference = checkIfAssertionIsParentIngredientAndGetUriReference((JumbfBox) contentBox);

            if (manifestReference != null) {
                break;
            }
        }

        return manifestReference;
    }

    public UriReference checkIfAssertionIsParentIngredientAndGetUriReference(JumbfBox assertionJumbfBox)
            throws MipamsException {

        MipamsAssertion type = MipamsAssertion.getTypeFromLabel(assertionJumbfBox.getDescriptionBox().getLabel());

        UriReference manifestReference = null;

        IngredientAssertion ingredient;

        if (MipamsAssertion.INGREDIENT.equals(type)) {
            ingredient = (IngredientAssertion) assertionFactory.convertJumbfBoxToAssertion(assertionJumbfBox);

            if (ingredient.getRelationship().equals(IngredientAssertion.RELATIONSHIP_PARENT_OF)) {
                manifestReference = ingredient.getManifestReference();
            }
        }

        return manifestReference;
    }

    public List<UriReference> getIngredientManifestIdReferenceList(JumbfBox manifestJumbfBox) throws MipamsException {
        JumbfBox assertionStoreJumbfBox = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox,
                new AssertionStoreContentType());

        List<UriReference> result = new ArrayList<>();

        IngredientAssertion ingredient;
        UriReference manifestReference;
        for (BmffBox contentBox : assertionStoreJumbfBox.getContentBoxList()) {
            JumbfBox assertionJumbfBox = (JumbfBox) contentBox;

            MipamsAssertion type = MipamsAssertion.getTypeFromLabel(assertionJumbfBox.getDescriptionBox().getLabel());

            if (MipamsAssertion.INGREDIENT.equals(type)) {
                ingredient = (IngredientAssertion) assertionFactory.convertJumbfBoxToAssertion(assertionJumbfBox);

                manifestReference = ingredient.getManifestReference();

                if (manifestReference != null) {
                    result.add(manifestReference);
                }
            }
        }

        return result;
    }

}
