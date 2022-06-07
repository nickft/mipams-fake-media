package org.mipams.fake_media.entities.assertions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BindingAssertion implements NonRedactableAssertion {
    private @Getter @Setter String algorithm;
    private @Getter @Setter String padding;
    private @Getter @Setter byte[] digest;
    private @Getter @Setter String description;
}
