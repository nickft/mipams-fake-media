package org.mipams.fake_media.entities.assertions;

import org.mipams.fake_media.entities.UriReference;

import lombok.Getter;
import lombok.Setter;

public class IngredientAssertion implements NonRedactableAssertion {

    public final static String RELATIONSHIP_PARENT_OF = "parentOf";
    public final static String RELATIONSHIP_COMPONENT_OF = "componentOf";

    private @Getter @Setter String title;
    private @Getter @Setter String format;
    private @Getter @Setter String thumbnailURL;
    private @Getter @Setter String instanceId;
    private @Getter @Setter String relationship;
    private @Getter @Setter UriReference manifestReference;
    private @Getter @Setter String metadata;
}
