package org.mipams.fake_media.config;

import org.mipams.jumbf.core.ContentTypeDiscoveryManager;
import org.mipams.jumbf.core.services.CoreGeneratorService;
import org.mipams.jumbf.core.services.CoreParserService;
import org.mipams.jumbf.core.services.boxes.BinaryDataBoxService;
import org.mipams.jumbf.core.services.boxes.CborBoxService;
import org.mipams.jumbf.core.services.boxes.ContiguousCodestreamBoxService;
import org.mipams.jumbf.core.services.boxes.DescriptionBoxService;
import org.mipams.jumbf.core.services.boxes.EmbeddedFileDescriptionBoxService;
import org.mipams.jumbf.core.services.boxes.JsonBoxService;
import org.mipams.jumbf.core.services.boxes.JumbfBoxService;
import org.mipams.jumbf.core.services.boxes.PaddingBoxService;
import org.mipams.jumbf.core.services.boxes.UuidBoxService;
import org.mipams.jumbf.core.services.boxes.XmlBoxService;
import org.mipams.jumbf.core.services.content_types.CborContentType;
import org.mipams.jumbf.core.services.content_types.ContiguousCodestreamContentType;
import org.mipams.jumbf.core.services.content_types.EmbeddedFileContentType;
import org.mipams.jumbf.core.services.content_types.JsonContentType;
import org.mipams.jumbf.core.services.content_types.UuidContentType;
import org.mipams.jumbf.core.services.content_types.XmlContentType;
import org.mipams.jumbf.core.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JumbfConfig {

    @Bean
    public CoreParserService coreParserService() {
        return new CoreParserService();
    }

    @Bean
    public CoreGeneratorService coreGeneratorService() {
        return new CoreGeneratorService();
    }

    @Bean
    public Properties properties() {
        return new Properties();
    }

    @Bean
    public ContentTypeDiscoveryManager contentTypeDiscoveryManager() {
        return new ContentTypeDiscoveryManager();
    }

    @Bean
    public JsonBoxService jsonBoxService() {
        return new JsonBoxService();
    }

    @Bean
    public JsonContentType jsonContentType() {
        return new JsonContentType();
    }

    @Bean
    public XmlBoxService xmlBoxService() {
        return new XmlBoxService();
    }

    @Bean
    public XmlContentType xmlContentType() {
        return new XmlContentType();
    }

    @Bean
    public UuidBoxService uuidBoxService() {
        return new UuidBoxService();
    }

    @Bean
    public UuidContentType uuidContentType() {
        return new UuidContentType();
    }

    @Bean
    public CborBoxService cborBoxService() {
        return new CborBoxService();
    }

    @Bean
    public CborContentType cborContentType() {
        return new CborContentType();
    }

    @Bean
    public ContiguousCodestreamBoxService contiguousCodestreamBoxService() {
        return new ContiguousCodestreamBoxService();
    }

    @Bean
    public ContiguousCodestreamContentType contiguousCodestreamContentType() {
        return new ContiguousCodestreamContentType();
    }

    @Bean
    public EmbeddedFileDescriptionBoxService embeddedFileDescriptionBoxService() {
        return new EmbeddedFileDescriptionBoxService();
    }

    @Bean
    public BinaryDataBoxService binaryDataBoxService() {
        return new BinaryDataBoxService();
    }

    @Bean
    public EmbeddedFileContentType embeddedFileContentType() {
        return new EmbeddedFileContentType();
    }

    @Bean
    public DescriptionBoxService descriptionBoxService() {
        return new DescriptionBoxService();
    }

    @Bean
    public JumbfBoxService jumbfBoxService() {
        return new JumbfBoxService();
    }

    @Bean
    public PaddingBoxService paddingBoxService() {
        return new PaddingBoxService();
    }

}
