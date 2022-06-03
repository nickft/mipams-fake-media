package org.mipams.fake_media.entities.assertions;

import lombok.Getter;
import lombok.Setter;

public class IngredientAssertion extends AbstractAssertion {
    private @Getter @Setter String title;
    private @Getter @Setter String format;
    private @Getter @Setter String thumbnailURL;
    private @Getter @Setter String instanceId;
    private @Getter @Setter String relationship;
    private @Getter @Setter String manifestStoreId;
    private @Getter @Setter String metadata;
}
