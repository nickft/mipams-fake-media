package org.mipams.fake_media.services.content_types;

import org.springframework.stereotype.Service;

@Service
public class StandardManifestContentType extends ManifestContentType {
    @Override
    public String getContentTypeUuid() {
        return "6D70736D-C65D-11EC-9D64-0242AC120002";
    }
}
