package org.mipams.fake_media.entities.assertions;

import org.mipams.jumbf.core.entities.JumbfBox;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public abstract class AbstractAssertion implements Assertion {
    protected @Getter @Setter JumbfBox accessRulesJumbfBox;

    @Override
    public JumbfBox getAccessRulesJumbfBoxOrNull() {
        return getAccessRulesJumbfBox();
    }
}
