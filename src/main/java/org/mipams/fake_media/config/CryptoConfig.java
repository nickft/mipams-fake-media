package org.mipams.fake_media.config;

import org.mipams.jumbf.crypto.services.CryptoService;
import org.mipams.jumbf.crypto.services.KeyReaderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {
    @Bean
    public CryptoService cryptoService() {
        return new CryptoService();
    }

    @Bean
    public KeyReaderService keyReaderService() {
        return new KeyReaderService();
    }
}
