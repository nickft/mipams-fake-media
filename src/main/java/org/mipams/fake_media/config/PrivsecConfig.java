package org.mipams.fake_media.config;

import org.mipams.jumbf.privacy_security.services.boxes.ProtectionDescriptionBoxService;
import org.mipams.jumbf.privacy_security.services.boxes.ReplacementDescriptionBoxService;
import org.mipams.jumbf.privacy_security.services.boxes.replacement.AppReplacementHandler;
import org.mipams.jumbf.privacy_security.services.boxes.replacement.BoxReplacementHandler;
import org.mipams.jumbf.privacy_security.services.boxes.replacement.DataBoxHandlerFactory;
import org.mipams.jumbf.privacy_security.services.boxes.replacement.ParamHandlerFactory;
import org.mipams.jumbf.privacy_security.services.boxes.replacement.RoiReplacementHandler;
import org.mipams.jumbf.privacy_security.services.content_types.ProtectionContentType;
import org.mipams.jumbf.privacy_security.services.content_types.ReplacementContentType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrivsecConfig {

    @Bean
    public ParamHandlerFactory paramHandlerFactory() {
        return new ParamHandlerFactory();
    }

    @Bean
    public ProtectionDescriptionBoxService protectionDescriptionBoxService() {
        return new ProtectionDescriptionBoxService();
    }

    @Bean
    public ProtectionContentType protectionContentType() {
        return new ProtectionContentType();
    }

    @Bean
    public DataBoxHandlerFactory dataBoxHandlerFactory() {
        return new DataBoxHandlerFactory();
    }

    @Bean
    public BoxReplacementHandler boxReplacementHandler() {
        return new BoxReplacementHandler();
    }

    @Bean
    public AppReplacementHandler appReplacementHandler() {
        return new AppReplacementHandler();
    }

    @Bean
    public RoiReplacementHandler roiReplacementHandler() {
        return new RoiReplacementHandler();
    }

    @Bean
    public ReplacementDescriptionBoxService replacementDescriptionBoxService() {
        return new ReplacementDescriptionBoxService();
    }

    @Bean
    public ReplacementContentType replacementContentType() {
        return new ReplacementContentType();
    }
}
