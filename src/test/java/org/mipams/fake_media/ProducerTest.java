package org.mipams.fake_media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.Properties;
import org.mipams.jumbf.crypto.services.CredentialsReaderService;
import org.mipams.fake_media.entities.ClaimGenerator;
import org.mipams.fake_media.entities.ProvenanceMetadata;
import org.mipams.fake_media.entities.ProvenanceSigner;
import org.mipams.fake_media.entities.assertions.ActionAssertion;
import org.mipams.fake_media.entities.assertions.Assertion;
import org.mipams.fake_media.entities.assertions.ThumbnailAssertion;
import org.mipams.fake_media.entities.requests.ProducerRequestBuilder;
import org.mipams.fake_media.services.AssertionFactory;
import org.mipams.fake_media.services.consumer.ManifestConsumer;
import org.mipams.fake_media.services.content_types.ManifestStoreContentType;
import org.mipams.fake_media.services.producer.ManifestProducer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.entities.JumbfBoxBuilder;
import org.mipams.jumbf.core.entities.ParseMetadata;
import org.mipams.jumbf.core.services.CoreGeneratorService;
import org.mipams.jumbf.core.services.boxes.JumbfBoxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.ResourceUtils;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public class ProducerTest {

    public final static String PROVENANCE_FILE_NAME = "provenance_manifest.jumbf";

    Logger logger = LoggerFactory.getLogger(ProducerTest.class);

    @Autowired
    CredentialsReaderService credentialsReaderService;

    @Autowired
    Properties properties;

    @Autowired
    ManifestProducer producer;

    @Autowired
    ManifestConsumer consumer;

    @Autowired
    CoreGeneratorService coreGeneratorService;

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Autowired
    AssertionFactory assertionFactory;

    @Test
    void testManifestProduction() throws Exception {
        Certificate cert = null;
        try (FileInputStream fis = new FileInputStream(
                ResourceUtils.getFile("classpath:test.public.crt").getAbsolutePath())) {

            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            while (fis.available() > 0) {
                cert = cf.generateCertificate(fis);
                System.out.println(cert.toString());
            }
        }

        PublicKey pubKey = cert.getPublicKey();
        PrivateKey privKey = credentialsReaderService
                .getPrivateKey(ResourceUtils.getFile("classpath:test.private.key").getAbsolutePath());

        KeyPair kp = new KeyPair(pubKey, privKey);

        ProvenanceSigner signer = new ProvenanceSigner();
        signer.setSigningScheme("SHA1withRSA");
        signer.setSigningCredentials(kp);
        signer.setSigningCertificate((X509Certificate) cert);

        // Create Assertions
        ActionAssertion assertion1 = new ActionAssertion();
        assertion1.setAction("mpms.prov.cropped");
        assertion1.setSoftwareAgent("Adobe Photoshop");
        assertion1.setDate("22/1/22 10:12:32");
        // assertion1.setParameters("blur: 10");

        ActionAssertion assertion2 = new ActionAssertion();
        assertion2.setAction("mpms.prov.filtered");
        assertion2.setSoftwareAgent("Adobe Photoshop");
        assertion2.setDate("22/1/22 10:15:32");
        // assertion2.setParameters("colourBefore: blue, colourAfter: green");

        ThumbnailAssertion assertion3 = new ThumbnailAssertion();
        assertion3.setFileName("image.jpeg");
        assertion3.setMediaType("application/jpeg");

        List<Assertion> assertionList = List.of(assertion1, assertion2, assertion3);

        String assetFileUrl = ResourceUtils.getFile("classpath:sample.jpeg").getAbsolutePath();
        ProducerRequestBuilder builder = new ProducerRequestBuilder(assetFileUrl);

        ClaimGenerator claimGen = new ClaimGenerator();
        claimGen.setDescription("Mipams Generator 2.0 (Desktop)");

        List<JumbfBox> assertionJumbfBoxList = new ArrayList<>();

        ProvenanceMetadata provenanceMetadata = new ProvenanceMetadata();
        provenanceMetadata.setParentDirectory(properties.getFileDirectory() + "/tmp");
        File f = new File(properties.getFileDirectory() + "/tmp");
        f.mkdir();

        for (Assertion assertion : assertionList) {
            assertionJumbfBoxList.add(assertionFactory.convertAssertionToJumbfBox(assertion, provenanceMetadata));
        }

        builder.setAssertionList(assertionJumbfBoxList);
        builder.setSigner(signer);
        builder.setClaimGenerator(claimGen);

        JumbfBox manifestJumbfBox = producer.produceManifestJumbfBox(builder.getResult());
        String outputFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), PROVENANCE_FILE_NAME);

        JumbfBoxBuilder manifestStoreBuilder = new JumbfBoxBuilder();

        ManifestStoreContentType service = new ManifestStoreContentType();
        manifestStoreBuilder.setContentType(service);
        manifestStoreBuilder.setJumbfBoxAsRequestable();
        manifestStoreBuilder.setLabel(service.getLabel());
        manifestStoreBuilder.appendContentBox(manifestJumbfBox);

        coreGeneratorService.generateJumbfMetadataToFile(List.of(manifestStoreBuilder.getResult()), outputFilePath);
        CoreUtils.deleteDir(
                CoreUtils.getFullPath(properties.getFileDirectory(), manifestJumbfBox.getDescriptionBox().getLabel()));
        CoreUtils.deleteDir(properties.getFileDirectory() + "/tmp");

        logger.info("Manifest box is stored in file " + outputFilePath);
    }

    @Test
    void testManifestConsumption() throws Exception {
        String inputFilePath = CoreUtils.getFullPath(properties.getFileDirectory(), PROVENANCE_FILE_NAME);

        String manifestDirectory = CoreUtils.createSubdirectory(properties.getFileDirectory(),
                CoreUtils.randomStringGenerator());

        JumbfBox manifestStoreJumbfBox;
        ParseMetadata parseMetadata = new ParseMetadata();
        parseMetadata.setParentDirectory(manifestDirectory);

        try (InputStream input = new FileInputStream(inputFilePath)) {
            manifestStoreJumbfBox = jumbfBoxService.parseFromJumbfFile(input, parseMetadata);
        }

        String assetFileUrl = ResourceUtils.getFile("classpath:sample.jpeg").getAbsolutePath();

        consumer.verifyManifestIntegrityAndContentBinding((JumbfBox) manifestStoreJumbfBox.getContentBoxList().get(0),
                assetFileUrl);

        CoreUtils.deleteDir(manifestDirectory);
    }
}
