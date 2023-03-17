package org.mipams.provenance.services.content_types;

import org.mipams.jumbf.util.MipamsException;

public class InvalidProvenanceFormatException extends MipamsException {

    public InvalidProvenanceFormatException() {
        super();
    }

    public InvalidProvenanceFormatException(String message) {
        super(message);
    }

    public InvalidProvenanceFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidProvenanceFormatException(Throwable cause) {
        super(cause);
    }
}
