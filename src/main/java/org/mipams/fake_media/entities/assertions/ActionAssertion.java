package org.mipams.fake_media.entities.assertions;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@ToString
public class ActionAssertion extends AbstractAssertion {
    private @Getter @Setter String action;
    private @Getter @Setter String date;
    private @Getter @Setter String changed;
    private @Getter @Setter String parameters;
    private @Getter @Setter String metadata;
    private @Getter @Setter String softwareAgent;
}
