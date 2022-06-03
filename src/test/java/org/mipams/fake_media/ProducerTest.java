package org.mipams.fake_media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.Properties;
import org.mipams.jumbf.crypto.services.KeyReaderService;
import org.mipams.fake_media.entities.ClaimGenerator;
import org.mipams.fake_media.entities.ProvenanceSigner;
import org.mipams.fake_media.entities.assertions.ActionAssertion;
import org.mipams.fake_media.entities.assertions.Assertion;
import org.mipams.fake_media.entities.assertions.ThumbnailAssertion;
import org.mipams.fake_media.entities.requests.ConsumerRequest;
import org.mipams.fake_media.entities.requests.ProducerRequestBuilder;
import org.mipams.fake_media.services.ProvenanceConsumer;
import org.mipams.fake_media.services.ProvenanceProducer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.services.boxes.JumbfBoxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public class ProducerTest {

    public final static String PROVENANCE_FILE_NAME = "provenance_manifest.jumbf";

    Logger logger = LoggerFactory.getLogger(ProducerTest.class);

    @Autowired
    KeyReaderService keyReaderService;

    @Autowired
    Properties properties;

    @Autowired
    ProvenanceProducer producer;

    @Autowired
    ProvenanceConsumer consumer;

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Test
    void testManifestProduction() throws Exception {
        Certificate cert = null;
        try (FileInputStream fis = new FileInputStream(properties.getFileDirectory() + "/example/server.public.crt")) {

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            while (fis.available() > 0) {
                cert = cf.generateCertificate(fis);
                System.out.println(cert.toString());
            }
        }

        PublicKey pubKey = cert.getPublicKey();
        PrivateKey privKey = keyReaderService
                .getPrivateKey(properties.getFileDirectory() + "/example/server.private.key");

        KeyPair kp = new KeyPair(pubKey, privKey);

        ProvenanceSigner signer = new ProvenanceSigner();
        signer.setSigningScheme("SHA1withRSA");
        signer.setSigningCredentials(kp);
        signer.setSigningCertificate(cert);

        // Create Assertions
        ActionAssertion assertion1 = new ActionAssertion();
        assertion1.setAction("mpms.prov.cropped");
        assertion1.setSoftwareAgent("Adobe Photoshop");
        assertion1.setDate("22/1/22 10:12:32");
        assertion1.setParameters("blur: 10");

        ActionAssertion assertion2 = new ActionAssertion();
        assertion2.setAction("mpms.prov.filtered");
        assertion2.setSoftwareAgent("Adobe Photoshop");
        assertion2.setDate("22/1/22 10:15:32");
        assertion2.setParameters("colourBefore: blue, colourAfter: green");

        ThumbnailAssertion assertion3 = new ThumbnailAssertion();
        assertion3.setFileName("image.jpeg");
        assertion3.setMediaType("application/jpeg");

        List<Assertion> assertionList = List.of(assertion1, assertion2, assertion3);

        ProducerRequestBuilder builder = new ProducerRequestBuilder(properties.getFileDirectory() + "/campnou2.jpeg");

        ClaimGenerator claimGen = new ClaimGenerator();
        claimGen.setDescription("Mipams Generator 2.0 (Desktop)");

        builder.setAssertionList(assertionList);
        builder.setSigner(signer);
        builder.setClaimGenerator(claimGen);

        JumbfBox manifestJumbfBox = producer.produceManifestJumbfBox(builder.getResult());

        String outputFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), PROVENANCE_FILE_NAME);

        try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
            jumbfBoxService.writeToJumbfFile(manifestJumbfBox, fos);
        }

        logger.info("Manifest box is stored in file " + outputFilePath);
    }

    @Test
    void testManifestConsumption() throws Exception {

        String inputFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), PROVENANCE_FILE_NAME);

        try (FileInputStream fis = new FileInputStream(inputFilePath)) {
            JumbfBox manifestJumbfBox = jumbfBoxService.parseSuperBox(fis);

            ConsumerRequest consumerRequest = new ConsumerRequest();
            consumerRequest.setAssetUrl(properties.getFileDirectory() + "/campnou2.jpeg");
            consumerRequest.setManifestContentTypeJumbfBox(manifestJumbfBox);

            consumer.verifyIntegrityOfManifestJumbfBox(consumerRequest);
        }
    }
}
