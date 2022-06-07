package org.mipams.fake_media.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
public class UriReference {

    public final static String SUPPORTED_HASH_ALGORITHM = "SHA-256";

    private @Getter @Setter byte[] digest;
    private @Getter @Setter String uri;
    private @Getter @Setter String algorithm;
}