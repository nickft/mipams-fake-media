package org.mipams.fake_media.entities.assertions;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExifMetadataAssertion implements RedactableAssertion {
    private @Getter @Setter String exifMetadata;
}
