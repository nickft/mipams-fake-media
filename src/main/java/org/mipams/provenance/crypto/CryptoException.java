package org.mipams.provenance.crypto;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CryptoException extends Exception {

    private static final Logger logger = Logger.getLogger(CryptoException.class.getName());

    public CryptoException(String message) {
        super(message);
        logger.log(Level.WARNING, message);
    }

    public CryptoException(String message, Throwable e) {
        super(message, e);
        logger.log(Level.WARNING, message, e);
    }

    public CryptoException(Throwable e) {
        super(e);
        logger.log(Level.WARNING, "Failed to perform crypto operation: ", e);
    }

}