package org.mipams.provenance.services.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mipams.jumbf.entities.BmffBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.privsec.entities.ProtectionDescriptionBox;
import org.mipams.privsec.services.content_types.ProtectionContentType;
import org.mipams.provenance.entities.HashedUriReference;
import org.mipams.provenance.entities.ProvenanceErrorMessages;
import org.mipams.provenance.entities.assertions.IngredientAssertion;
import org.mipams.provenance.entities.responses.ManifestStoreResponse;
import org.mipams.provenance.entities.responses.ProtectedManifestResponse;
import org.mipams.provenance.services.AssertionFactory;
import org.mipams.provenance.services.ManifestDiscovery;
import org.mipams.provenance.services.UriReferenceService;
import org.mipams.provenance.services.AssertionFactory.MipamsAssertion;
import org.mipams.provenance.services.content_types.AssertionStoreContentType;
import org.mipams.provenance.services.content_types.ManifestContentType;
import org.mipams.provenance.utils.ProvenanceUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManifestStoreConsumer {

    private static final Logger logger = Logger.getLogger(AssertionStoreConsumer.class.getName());

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

        logger.log(Level.FINE, "Consuming Entire Manifest Store");

        ManifestStoreResponse manifestStoreResponse = consumeActiveManifest(manifestStoreJumbfBox, assetUrl);

        JumbfBox activeManifestJumbfBox = ProvenanceUtils.locateActiveManifest(manifestStoreJumbfBox);

        if ((new ProtectionContentType()).getContentTypeUuid().equals(activeManifestJumbfBox.getDescriptionBox().getUuid())) {
            return manifestStoreResponse;
        }

        List<HashedUriReference> manifestIdToBeChecked = getIngredientManifestIdReferenceList(activeManifestJumbfBox);

        HashedUriReference currentManifestReference;
        JumbfBox currentManifestJumbfBox;

        while (manifestIdToBeChecked.size() > 0) {
            currentManifestReference = manifestIdToBeChecked.remove(0);
            logger.info("Consuming manifest with Id " + currentManifestReference.getUri());

            currentManifestJumbfBox = ProvenanceUtils.locateManifestFromUri(manifestStoreJumbfBox,
                    currentManifestReference.getUri());

            if ((new ProtectionContentType()).getContentTypeUuid().equals(currentManifestJumbfBox.getDescriptionBox().getUuid())) {
                ProtectionDescriptionBox pdBox = (ProtectionDescriptionBox) currentManifestJumbfBox.getContentBoxList().get(0);

                boolean hasAccessRules = pdBox.getArLabel() != null;

                Optional<BmffBox> accessRulesJumbfBox = Optional.empty();

                if (hasAccessRules) {
                    try {
                        accessRulesJumbfBox = manifestStoreJumbfBox.getContentBoxList().stream().filter(c -> pdBox.getArLabel().equals(((JumbfBox) c).getDescriptionBox().getLabel())).findFirst();

                        if (accessRulesJumbfBox.isEmpty()) {
                            throw new MipamsException(
                                    "Could not find access rules JUMBF box with label: " + pdBox.getArLabel());
                        }
                    } catch (Exception e) {
                        logger.info("Integrity of a URI reference to a protected content is not achieved.");
                    }
                }

                ProtectedManifestResponse protectedResponse = new ProtectedManifestResponse(currentManifestJumbfBox.getDescriptionBox().getLabel(), accessRulesJumbfBox.isPresent() ? (JumbfBox) accessRulesJumbfBox.get() : null);
                manifestStoreResponse.getManifestResponseMap().put(currentManifestJumbfBox.getDescriptionBox().getLabel(), protectedResponse);

                try {
                    uriReferenceService.verifyManifestUriReference(accessRulesJumbfBox.isPresent() ? List.of(currentManifestJumbfBox, ((JumbfBox) accessRulesJumbfBox.get())) : List.of(currentManifestJumbfBox), currentManifestReference);
                    protectedResponse.setIntegrityStands(true);
                } catch (MipamsException e) {
                    logger.info(String.format("Integrity check does not stand for reference to Protected Manifest %s", currentManifestJumbfBox.getDescriptionBox().getLabel()));
                    protectedResponse.setIntegrityStands(false);
                }

            } else {
                uriReferenceService.verifyManifestUriReference(List.of(currentManifestJumbfBox), currentManifestReference);

                manifestConsumer.verifyManifestIntegrity(currentManifestJumbfBox);
    
                manifestStoreResponse.addManifestResponse(currentManifestJumbfBox);
    
                manifestIdToBeChecked.addAll(getIngredientManifestIdReferenceList(currentManifestJumbfBox));
            }
            
        }

        return manifestStoreResponse;
    }

    public ManifestStoreResponse consumeActiveManifest(JumbfBox manifestStoreJumbfBox, String assetUrl)
            throws MipamsException {

        logger.log(Level.FINE, "Consuming Active Manifest");

        ManifestStoreResponse manifestStoreResponse = new ManifestStoreResponse();

        JumbfBox activeManifestJumbfBox = ProvenanceUtils.locateActiveManifest(manifestStoreJumbfBox);

        if((new ProtectionContentType()).getContentTypeUuid().equals(activeManifestJumbfBox.getDescriptionBox().getUuid())){
            ProtectionDescriptionBox pdBox = (ProtectionDescriptionBox) activeManifestJumbfBox.getContentBoxList().get(0);
            boolean hasAccessRules = pdBox.getArLabel() != null;

            Optional<BmffBox> accessRulesJumbfBox = Optional.empty();

            if (hasAccessRules) {
                try {
                    accessRulesJumbfBox = manifestStoreJumbfBox.getContentBoxList().stream().filter(c -> pdBox.getArLabel().equals(((JumbfBox) c).getDescriptionBox().getLabel())).findFirst();

                    if (accessRulesJumbfBox.isEmpty()) {
                        throw new MipamsException(
                                "Could not find access rules JUMBF box with label: " + pdBox.getArLabel());
                    }
                } catch (Exception e) {
                    logger.info("Integrity of a URI reference to a protected content is not achieved.");
                }
            }
            ProtectedManifestResponse protectedResponse = new ProtectedManifestResponse(activeManifestJumbfBox.getDescriptionBox().getLabel(), accessRulesJumbfBox.isPresent() ? (JumbfBox) accessRulesJumbfBox.get() : null);
            manifestStoreResponse.getManifestResponseMap().put(activeManifestJumbfBox.getDescriptionBox().getLabel(), protectedResponse);
        } else {
            ManifestContentType contentType = manifestDiscovery.discoverManifestType(activeManifestJumbfBox);

            if (manifestDiscovery.isStandardManifestRequest(contentType)) {
                manifestConsumer.verifyManifestIntegrityAndContentBinding(activeManifestJumbfBox, assetUrl);
            } else {
                manifestConsumer.verifyManifestIntegrity(activeManifestJumbfBox);

                HashedUriReference parentUriReference = locateParentIngredientReferenceFromManifest(activeManifestJumbfBox);

                if (parentUriReference == null) {
                    throw new MipamsException(ProvenanceErrorMessages.UPDATE_MANIFEST_CONTENT_BINDING);
                }

                JumbfBox parentStandardManifest = ProvenanceUtils.locateManifestFromUri(manifestStoreJumbfBox,
                        parentUriReference.getUri());

                ArrayList<JumbfBox> jumbfBoxList = new ArrayList<>();
                jumbfBoxList.add(parentStandardManifest);

                if((new ProtectionContentType()).getContentTypeUuid().equals(parentStandardManifest.getDescriptionBox().getUuid())){
                    ProtectionDescriptionBox pdBox = (ProtectionDescriptionBox) parentStandardManifest.getContentBoxList().get(0);

                    try{
                        if(pdBox.getArLabel() != null) {
                            Optional<BmffBox> accessRulesJumbfBox = manifestStoreJumbfBox.getContentBoxList().stream().filter(c -> pdBox.getArLabel().equals(((JumbfBox) c).getDescriptionBox().getLabel())).findFirst();

                            if (accessRulesJumbfBox.isEmpty()) {
                                throw new MipamsException(
                                        "Could not find access rules JUMBF box with label: " + pdBox.getArLabel());
                            }
                            jumbfBoxList.add((JumbfBox) accessRulesJumbfBox.get());
                        } 

                    } catch (Exception e) {
                        logger.info("Integrity of a URI reference to a protected content is not achieved.");
                    }
                }
                
                uriReferenceService.verifyManifestUriReference(jumbfBoxList, parentUriReference);

                manifestConsumer.verifyManifestIntegrityAndContentBinding(parentStandardManifest, assetUrl);
            }

            manifestStoreResponse.addManifestResponse(activeManifestJumbfBox);
        }

        return manifestStoreResponse;
    }

    private HashedUriReference locateParentIngredientReferenceFromManifest(JumbfBox manifestJumbfBox)
            throws MipamsException {

        JumbfBox assertionStoreJumbfBox = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox,
                new AssertionStoreContentType());

        HashedUriReference manifestReference = null;

        for (BmffBox contentBox : assertionStoreJumbfBox.getContentBoxList()) {

            manifestReference = checkIfAssertionIsParentIngredientAndGetUriReference((JumbfBox) contentBox);

            if (manifestReference != null) {
                break;
            }
        }

        return manifestReference;
    }

    public HashedUriReference checkIfAssertionIsParentIngredientAndGetUriReference(JumbfBox assertionJumbfBox)
            throws MipamsException {

        MipamsAssertion type = MipamsAssertion.getTypeFromLabel(assertionJumbfBox.getDescriptionBox().getLabel());

        HashedUriReference manifestReference = null;

        IngredientAssertion ingredient;

        if (MipamsAssertion.INGREDIENT.equals(type)) {
            ingredient = (IngredientAssertion) assertionFactory.convertJumbfBoxToAssertion(assertionJumbfBox);

            if (ingredient.getRelationship().equals(IngredientAssertion.RELATIONSHIP_PARENT_OF)) {
                manifestReference = ingredient.getManifestReference();
            }
        }

        return manifestReference;
    }

    public List<HashedUriReference> getIngredientManifestIdReferenceList(JumbfBox manifestJumbfBox)
            throws MipamsException {
        JumbfBox assertionStoreJumbfBox = ProvenanceUtils.getProvenanceJumbfBox(manifestJumbfBox,
                new AssertionStoreContentType());

        List<HashedUriReference> result = new ArrayList<>();

        IngredientAssertion ingredient;
        HashedUriReference manifestReference;
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
