package org.mipams.fake_media.services.producer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mipams.fake_media.entities.ProvenanceErrorMessages;
import org.mipams.fake_media.entities.UriReference;
import org.mipams.fake_media.entities.requests.ProducerRequest;
import org.mipams.fake_media.entities.responses.ManifestStoreResponse;
import org.mipams.fake_media.services.UriReferenceService;
import org.mipams.fake_media.services.consumer.ManifestStoreConsumer;
import org.mipams.fake_media.services.content_types.ManifestStoreContentType;
import org.mipams.fake_media.utils.ProvenanceUtils;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.entities.JumbfBoxBuilder;
import org.mipams.jumbf.core.util.MipamsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManifestStoreProducer {

    private static final Logger logger = LoggerFactory.getLogger(ManifestStoreProducer.class);

    @Autowired
    ManifestStoreContentType manifestStoreContentType;

    @Autowired
    ManifestStoreConsumer manifestStoreConsumer;

    @Autowired
    ManifestProducer manifestProducer;

    @Autowired
    UriReferenceService uriReferenceService;

    public final JumbfBox createManifestStore(ProducerRequest producerRequest) throws MipamsException {

        JumbfBox currentManifestStoreJumbfBox = producerRequest.getManifestStoreJumbfBox();

        JumbfBoxBuilder manifestStoreJumbfBoxBuilder;
        if (currentManifestStoreJumbfBox != null) {

            manifestStoreJumbfBoxBuilder = new JumbfBoxBuilder(currentManifestStoreJumbfBox);

            logger.debug("There is already manifest store in the digital asset");

            ManifestStoreResponse response = manifestStoreConsumer.consumeActiveManifest(currentManifestStoreJumbfBox,
                    producerRequest.getAssetUrl());

            String activeManifestId = response.getManifestResponseMap().keySet().iterator().next();
            String activeManifestUri = ProvenanceUtils.getProvenanceJumbfURL(activeManifestId);

            logger.debug("Active manifest: " + activeManifestUri);

            JumbfBox activeManifestJumbfBox = ProvenanceUtils.locateManifestFromUri(currentManifestStoreJumbfBox,
                    activeManifestUri);

            validateDigestForParentIngredientRelationship(activeManifestJumbfBox, producerRequest.getAssertionList());

            manifestStoreJumbfBoxBuilder = new JumbfBoxBuilder(currentManifestStoreJumbfBox);
        } else {
            manifestStoreJumbfBoxBuilder = createEmptyManifestStoreJumbfBoxBuilder();
            logger.debug("Creating new Manifest Store");
        }

        JumbfBox manifestJumbfBox = manifestProducer.produceManifestJumbfBox(producerRequest);

        if (producerRequest.getComponentManifestJumbfBoxList() != null) {
            logger.debug("Embedding Component Ingredients in Manifest Store");

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

        UriReference manifestReference = null;

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
        JumbfBoxBuilder builder = new JumbfBoxBuilder();

        builder.setJumbfBoxAsRequestable();
        builder.setContentType(manifestStoreContentType);
        builder.setLabel(manifestStoreContentType.getLabel());

        return builder;
    }

    private void validateComponentIngredientReferenceDigests(JumbfBox manifestJumbfBox,
            List<JumbfBox> componentManifestJumbfBoxList) throws MipamsException {

        List<UriReference> ingredientRefList = manifestStoreConsumer
                .getIngredientManifestIdReferenceList(manifestJumbfBox);

        Map<String, UriReference> uriToReferenceMap = new HashMap<>();

        for (UriReference uriReference : ingredientRefList) {
            uriToReferenceMap.put(uriReference.getUri(), uriReference);
        }

        String ingredientManifestId, ingredientManifestUri;
        UriReference ingredientReference;
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
