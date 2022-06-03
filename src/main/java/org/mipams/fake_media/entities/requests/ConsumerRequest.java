package org.mipams.fake_media.entities.requests;

import javax.crypto.SecretKey;

import org.mipams.jumbf.core.entities.JumbfBox;

import lombok.Getter;
import lombok.Setter;

public class ConsumerRequest {
    private @Getter @Setter String assetUrl;
    private @Getter @Setter JumbfBox manifestContentTypeJumbfBox;
    private @Getter @Setter SecretKey encryptionKey;
    private @Getter @Setter String encryptionScheme;
}