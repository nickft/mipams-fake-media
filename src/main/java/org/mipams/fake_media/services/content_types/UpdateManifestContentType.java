package org.mipams.fake_media.services.content_types;

import org.springframework.stereotype.Service;

@Service
public class UpdateManifestContentType extends ManifestContentType {

    @Override
    public String getContentTypeUuid() {
        return "6D70756D-C65D-11EC-9D64-0242AC120002";
    }
}