package org.mipams.provenance.services.content_types;

import org.mipams.jumbf.services.content_types.ContentTypeService;

public interface ProvenanceContentType extends ContentTypeService {
    public String getLabel();
}
