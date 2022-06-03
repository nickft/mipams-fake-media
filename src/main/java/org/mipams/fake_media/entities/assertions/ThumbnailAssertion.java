package org.mipams.fake_media.entities.assertions;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@ToString
public class ThumbnailAssertion extends AbstractAssertion implements RedactableAssertion {
    private @Getter @Setter String fileName;
    private @Getter @Setter String mediaType;
}
