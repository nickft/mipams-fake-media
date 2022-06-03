package org.mipams.fake_media.entities.assertions;

import org.mipams.jumbf.core.entities.JumbfBox;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BindingAssertion extends AbstractAssertion {
    private @Getter @Setter String algorithm;
    private @Getter @Setter String padding;
    private @Getter @Setter byte[] digest;
    private @Getter @Setter String description;

    @Override
    public JumbfBox getAccessRulesJumbfBoxOrNull() {
        return null;
    }
}
