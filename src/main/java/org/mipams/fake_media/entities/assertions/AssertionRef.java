package org.mipams.fake_media.entities.assertions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
public class AssertionRef {
    private @Getter @Setter byte[] digest;
    private @Getter @Setter String uri;
    private @Getter @Setter String algorithm;
}