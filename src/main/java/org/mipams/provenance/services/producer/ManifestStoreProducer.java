package org.mipams.provenance.services.producer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.entities.JumbfBoxBuilder;
import org.mipams.jumbf.util.MipamsException;
import org.mipams.provenance.entities.HashedUriReference;
import org.mipams.provenance.entities.ProvenanceErrorMessages;
import org.mipams.provenance.entities.requests.ProducerRequest;
import org.mipams.provenance.services.UriReferenceService;
import org.mipams.provenance.services.consumer.ManifestConsumer;
import org.mipams.provenance.services.consumer.ManifestStoreConsumer;
import org.mipams.provenance.services.content_types.ManifestStoreContentType;
import org.mipams.provenance.utils.ProvenanceUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManifestStoreProducer {

    private static final Logger logger = Logger.getLogger(ManifestStoreProducer.class.getName());

    @Autowired
    ManifestStoreContentType manifestStoreContentType;

    @Autowired
    ManifestStoreConsumer manifestStoreConsumer;

    @Autowired
    ManifestProducer manifestProducer;

    @Autowired
    ManifestConsumer manifestConsumer;

    @Autowired
    UriReferenceService uriReferenceService;

    public final JumbfBox createManifestStore(ProducerRequest producerRequest) throws MipamsException {

        JumbfBox currentManifestStoreJumbfBox = producerRequest.getManifestStoreJumbfBox();

        JumbfBoxBuilder manifestStoreJumbfBoxBuilder;
        if (currentManifestStoreJumbfBox != null) {

            manifestStoreJumbfBoxBuilder = new JumbfBoxBuilder(currentManifestStoreJumbfBox);

            logger.log(Level.FINE, "There is already manifest store in the digital asset");

            JumbfBox manifestJumbfBox = ProvenanceUtils.locateActiveManifest(currentManifestStoreJumbfBox);
            manifestConsumer.verifyManifestIntegrity(manifestJumbfBox);

            String activeManifestId = manifestJumbfBox.getDescriptionBox().getLabel();
            String activeManifestUri = ProvenanceUtils.getProvenanceJumbfURL(activeManifestId);

            logger.log(Level.FINE, "Active manifest: " + activeManifestUri);

            JumbfBox activeManifestJumbfBox = ProvenanceUtils.locateManifestFromUri(currentManifestStoreJumbfBox,
                    activeManifestUri);

            validateDigestForParentIngredientRelationship(activeManifestJumbfBox, producerRequest.getAssertionList());

            manifestStoreJumbfBoxBuilder = new JumbfBoxBuilder(currentManifestStoreJumbfBox);
        } else {
            manifestStoreJumbfBoxBuilder = createEmptyManifestStoreJumbfBoxBuilder();
            logger.log(Level.FINE, "Creating new Manifest Store");
        }

        JumbfBox manifestJumbfBox = manifestProducer.produceManifestJumbfBox(producerRequest);

        if (producerRequest.getComponentManifestJumbfBoxList() != null) {
            logger.log(Level.FINE, "Embedding Component Ingredients in Manifest Store");

            validateComponentIngredientReferenceDigests(manifestJumbfBox,
                    producerRequest.getComponentManifestJumbfBoxList());

            manifestStoreJumbfBoxBuilder.appendAllContentBoxes(producerRequest.getComponentManifestJumbfBoxList());
        }

        manifestStoreJumbfBoxBuilder.appendContentBox(manifestJumbfBox);

        return manifestStoreJumbfBoxBuilder.getResult();
    }

    private void validateDigestForParentIngredientRelationship(JumbfBox activeManifestJumbfBox,
            List<JumbfBox> assertionJumbfBoxList) throws MipamsException {

        String manifestId = activeManifestJumbfBox.getDescriptionBox().getLabel();

        if (manifestId == null) {
            throw new MipamsException(String.format(ProvenanceErrorMessages.EMPTY_LABEL, "Manifet JUMBF Box"));
        }

        final String targetUriReference = ProvenanceUtils.getProvenanceJumbfURL(manifestId);

        HashedUriReference manifestReference = null;

        for (JumbfBox assertionJumbfBox : assertionJumbfBoxList) {
            manifestReference = manifestStoreConsumer
                    .checkIfAssertionIsParentIngredientAndGetUriReference(assertionJumbfBox);

            if (manifestReference != null && manifestReference.getUri().equals(targetUriReference)) {
                uriReferenceService.verifyManifestUriReference(activeManifestJumbfBox, manifestReference);
                return;
            }
        }

        throw new MipamsException(
                String.format(ProvenanceErrorMessages.PARENT_INGREDIENT_NOT_FOUND, targetUriReference));

    }

    private JumbfBoxBuilder createEmptyManifestStoreJumbfBoxBuilder() throws MipamsException {
        JumbfBoxBuilder builder = new JumbfBoxBuilder(manifestStoreContentType);
        builder.setJumbfBoxAsRequestable();
        builder.setLabel(manifestStoreContentType.getLabel());

        return builder;
    }

    private void validateComponentIngredientReferenceDigests(JumbfBox manifestJumbfBox,
            List<JumbfBox> componentManifestJumbfBoxList) throws MipamsException {

        List<HashedUriReference> ingredientRefList = manifestStoreConsumer
                .getIngredientManifestIdReferenceList(manifestJumbfBox);

        Map<String, HashedUriReference> uriToReferenceMap = new HashMap<>();

        for (HashedUriReference uriReference : ingredientRefList) {
            uriToReferenceMap.put(uriReference.getUri(), uriReference);
        }

        String ingredientManifestId, ingredientManifestUri;
        HashedUriReference ingredientReference;
        for (JumbfBox ingredientManifest : componentManifestJumbfBoxList) {

            ingredientManifestId = ingredientManifest.getDescriptionBox().getLabel();

            if (ingredientManifestId == null) {
                throw new MipamsException(String.format(ProvenanceErrorMessages.EMPTY_LABEL, "Manifest JUMBF Box"));
            }

            ingredientManifestUri = ProvenanceUtils.getProvenanceJumbfURL(ingredientManifestId);

            ingredientReference = uriToReferenceMap.get(ingredientManifestUri);

            if (ingredientReference != null) {
                uriReferenceService.verifyManifestUriReference(ingredientManifest, ingredientReference);
            }
        }
    }
}
