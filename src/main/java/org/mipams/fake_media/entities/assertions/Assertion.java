package org.mipams.fake_media.entities.assertions;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.fake_media.entities.ProvenanceEntity;

public interface Assertion extends ProvenanceEntity {
    public JumbfBox getAccessRulesJumbfBoxOrNull();
}
