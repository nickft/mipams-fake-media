package org.mipams.provenance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.mipams.provenance.crypto.CredentialsReaderService;
import org.mipams.provenance.crypto.CryptoService;
import org.mipams.provenance.services.AssertionFactory;
import org.mipams.provenance.services.ManifestDiscovery;
import org.mipams.provenance.services.RedactionService;
import org.mipams.provenance.services.UriReferenceService;
import org.mipams.provenance.services.consumer.AssertionStoreConsumer;
import org.mipams.provenance.services.consumer.ClaimConsumer;
import org.mipams.provenance.services.consumer.ClaimSignatureConsumer;
import org.mipams.provenance.services.consumer.ManifestConsumer;
import org.mipams.provenance.services.consumer.ManifestStoreConsumer;
import org.mipams.provenance.services.content_types.AssertionStoreContentType;
import org.mipams.provenance.services.content_types.ClaimContentType;
import org.mipams.provenance.services.content_types.ClaimSignatureContentType;
import org.mipams.provenance.services.content_types.CredentialStoreContentType;
import org.mipams.provenance.services.content_types.ManifestStoreContentType;
import org.mipams.provenance.services.content_types.StandardManifestContentType;
import org.mipams.provenance.services.content_types.UpdateManifestContentType;
import org.mipams.provenance.services.producer.AssertionRefProducer;
import org.mipams.provenance.services.producer.AssertionStoreProducer;
import org.mipams.provenance.services.producer.ClaimProducer;
import org.mipams.provenance.services.producer.ClaimSignatureProducer;
import org.mipams.provenance.services.producer.ManifestProducer;
import org.mipams.provenance.services.producer.ManifestStoreProducer;

@Configuration
public class FakeMediaConfig {

    @Bean
    public AssertionStoreContentType assertionStoreContentType() {
        return new AssertionStoreContentType();
    }

    @Bean
    public ClaimContentType claimContentType() {
        return new ClaimContentType();
    }

    @Bean
    public ClaimSignatureContentType claimSignatureContentType() {
        return new ClaimSignatureContentType();
    }

    @Bean
    public CredentialStoreContentType credentialStoreContentType() {
        return new CredentialStoreContentType();
    }

    @Bean
    public ManifestStoreContentType manifestContentType() {
        return new ManifestStoreContentType();
    }

    @Bean
    public StandardManifestContentType standardManifestContentType() {
        return new StandardManifestContentType();
    }

    @Bean
    public UpdateManifestContentType updateManifestContentType() {
        return new UpdateManifestContentType();
    }

    @Bean
    public CredentialsReaderService credentialsReaderService() {
        return new CredentialsReaderService();
    }

    @Bean
    public CryptoService cryptoService() {
        return new CryptoService();
    }

    @Bean
    public AssertionRefProducer assertionRefProducer() {
        return new AssertionRefProducer();
    }

    @Bean
    public AssertionStoreProducer assertionStoreProducer() {
        return new AssertionStoreProducer();
    }

    @Bean
    public AssertionStoreConsumer assertionStoreConsumer() {
        return new AssertionStoreConsumer();
    }

    @Bean
    public ClaimProducer claimProducer() {
        return new ClaimProducer();
    }

    @Bean
    public ClaimConsumer claimConsumer() {
        return new ClaimConsumer();
    }

    @Bean
    public ClaimSignatureProducer claimSignatureProducer() {
        return new ClaimSignatureProducer();
    }

    @Bean
    public ClaimSignatureConsumer claimSignatureConsumer() {
        return new ClaimSignatureConsumer();
    }

    @Bean
    public ManifestDiscovery manifestDiscovery() {
        return new ManifestDiscovery();
    }

    @Bean
    public ManifestStoreConsumer manifestStoreConsumer() {
        return new ManifestStoreConsumer();
    }

    @Bean
    public ManifestStoreProducer manifestStoreProducer() {
        return new ManifestStoreProducer();
    }

    @Bean
    public ManifestConsumer manifestConsumer() {
        return new ManifestConsumer();
    }

    @Bean
    public ManifestProducer manifestProducer() {
        return new ManifestProducer();
    }

    @Bean
    public RedactionService redactionService() {
        return new RedactionService();
    }

    @Bean
    public UriReferenceService uriReferenceService() {
        return new UriReferenceService();
    }

    @Bean
    public AssertionFactory assertionFactory() {
        return new AssertionFactory();
    }
}
