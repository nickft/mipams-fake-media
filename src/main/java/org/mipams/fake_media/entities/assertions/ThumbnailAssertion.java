package org.mipams.fake_media.entities.assertions;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@ToString
public class ThumbnailAssertion implements NonRedactableAssertion {
    private @Getter @Setter String fileName;
    private @Getter @Setter String mediaType;
}
