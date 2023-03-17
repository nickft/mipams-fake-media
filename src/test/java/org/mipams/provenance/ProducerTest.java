package org.mipams.provenance;

import org.mipams.jumbf.util.CoreUtils;
import org.mipams.privsec.config.PrivsecConfig;
import org.mipams.provenance.config.FakeMediaConfig;
import org.mipams.provenance.crypto.CredentialsReaderService;
import org.mipams.provenance.entities.ClaimGenerator;
import org.mipams.provenance.entities.ProvenanceMetadata;
import org.mipams.provenance.entities.ProvenanceSigner;
import org.mipams.provenance.entities.assertions.ActionAssertion;
import org.mipams.provenance.entities.assertions.Assertion;
import org.mipams.provenance.entities.requests.ProducerRequestBuilder;
import org.mipams.provenance.services.AssertionFactory;
import org.mipams.provenance.services.consumer.ManifestConsumer;
import org.mipams.provenance.services.content_types.ManifestStoreContentType;
import org.mipams.provenance.services.producer.ManifestProducer;

import java.io.BufferedInputStream;
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
import java.util.logging.Logger;

import org.mipams.jumbf.config.JumbfConfig;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.entities.JumbfBoxBuilder;
import org.mipams.jumbf.entities.ParseMetadata;
import org.mipams.jumbf.services.CoreGeneratorService;
import org.mipams.jumbf.services.boxes.JumbfBoxService;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ResourceUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { JumbfConfig.class, PrivsecConfig.class, FakeMediaConfig.class })
@ActiveProfiles("test")
public class ProducerTest {

    public final static String PROVENANCE_FILE_NAME = "provenance_manifest";

    private static final Logger logger = Logger.getLogger(ProducerTest.class.getName());

    @Autowired
    CredentialsReaderService credentialsReaderService;

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
    @Order(1)
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

        ActionAssertion assertion2 = new ActionAssertion();
        assertion2.setAction("mpms.prov.filtered");
        assertion2.setSoftwareAgent("Adobe Photoshop");
        assertion2.setDate("22/1/22 10:15:32");

        List<Assertion> assertionList = List.of(assertion1, assertion2);

        String assetFileUrl = ResourceUtils.getFile("classpath:sample.jpeg").getAbsolutePath();
        ProducerRequestBuilder builder = new ProducerRequestBuilder(assetFileUrl);

        ClaimGenerator claimGen = new ClaimGenerator();
        claimGen.setDescription("Mipams Generator 2.0 (Desktop)");

        List<JumbfBox> assertionJumbfBoxList = new ArrayList<>();

        final String randomFile = CoreUtils.createTempFile(PROVENANCE_FILE_NAME, null);

        final String outputFilePath = CoreUtils.getFullPath(CoreUtils.getParentDirectory(randomFile),
                PROVENANCE_FILE_NAME + CoreUtils.JUMBF_FILENAME_SUFFIX);

        ProvenanceMetadata provenanceMetadata = new ProvenanceMetadata();
        provenanceMetadata.setParentDirectory("/tmp/tmp");
        File f = new File("/tmp/tmp");
        f.mkdir();

        for (Assertion assertion : assertionList) {
            assertionJumbfBoxList.add(assertionFactory.convertAssertionToJumbfBox(assertion, provenanceMetadata));
        }

        builder.setAssertionList(assertionJumbfBoxList);
        builder.setSigner(signer);
        builder.setClaimGenerator(claimGen);

        JumbfBox manifestJumbfBox = producer.produceManifestJumbfBox(builder.getResult());

        ManifestStoreContentType service = new ManifestStoreContentType();
        JumbfBoxBuilder manifestStoreBuilder = new JumbfBoxBuilder(service);

        manifestStoreBuilder.setJumbfBoxAsRequestable();
        manifestStoreBuilder.setLabel(service.getLabel());
        manifestStoreBuilder.appendContentBox(manifestJumbfBox);

        coreGeneratorService.generateJumbfMetadataToFile(List.of(manifestStoreBuilder.getResult()), outputFilePath);
        CoreUtils.deleteDir(
                CoreUtils.getFullPath(CoreUtils.getParentDirectory(outputFilePath),
                        manifestJumbfBox.getDescriptionBox().getLabel()));
        CoreUtils.deleteDir(CoreUtils.getParentDirectory(outputFilePath) + "/tmp");
        CoreUtils.deleteFile(randomFile);

        logger.info("Manifest box is stored in file " + outputFilePath);
    }

    @Test
    @Order(2)
    void testManifestConsumption() throws Exception {
        final String randomFile = CoreUtils.createTempFile(PROVENANCE_FILE_NAME, null);

        final String inputFilePath = CoreUtils.getFullPath(CoreUtils.getParentDirectory(randomFile),
                PROVENANCE_FILE_NAME + CoreUtils.JUMBF_FILENAME_SUFFIX);

        String manifestDirectory = CoreUtils.createSubdirectory(CoreUtils.getParentDirectory(inputFilePath),
                CoreUtils.randomStringGenerator());

        JumbfBox manifestStoreJumbfBox;
        ParseMetadata parseMetadata = new ParseMetadata();
        parseMetadata.setParentDirectory(manifestDirectory);

        try (InputStream input = new BufferedInputStream(new FileInputStream(inputFilePath), 8)) {
            manifestStoreJumbfBox = jumbfBoxService.parseFromJumbfFile(input, parseMetadata);
        }

        final String assetFileUrl = ResourceUtils.getFile("classpath:sample.jpeg").getAbsolutePath();
        consumer.verifyManifestIntegrityAndContentBinding((JumbfBox) manifestStoreJumbfBox.getContentBoxList().get(0),
                assetFileUrl);

        CoreUtils.deleteDir(manifestDirectory);
        CoreUtils.deleteFile(randomFile);
    }
}
