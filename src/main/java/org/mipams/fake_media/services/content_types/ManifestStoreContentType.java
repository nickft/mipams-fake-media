package org.mipams.fake_media.services.content_types;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.entities.ParseMetadata;
import org.mipams.jumbf.core.services.boxes.JumbfBoxService;
import org.mipams.jumbf.core.util.MipamsException;
import org.mipams.jumbf.core.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ManifestStoreContentType implements ProvenanceContentType {

    private static final Logger logger = LoggerFactory.getLogger(ManifestStoreContentType.class);

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Autowired
    Properties properties;

    @Override
    public String getContentTypeUuid() {
        return "6D707374-C65D-11EC-9D64-0242AC120002";
    }

    @Override
    public String getLabel() {
        return "mpms.provenance";
    }

    @Override
    public List<BmffBox> parseContentBoxesFromJumbfFile(InputStream input, ParseMetadata parseMetadata)
            throws MipamsException {

        logger.debug("Start parsing a new Manifest Store");

        List<BmffBox> contentBoxList = new ArrayList<>();

        long remainingBytes = parseMetadata.getAvailableBytesForBox();

        while (remainingBytes > 0) {

            ParseMetadata manifestParseMetadata = new ParseMetadata();
            manifestParseMetadata.setAvailableBytesForBox(remainingBytes);
            manifestParseMetadata.setParentDirectory(parseMetadata.getParentDirectory());

            JumbfBox manifest = jumbfBoxService.parseFromJumbfFile(input, manifestParseMetadata);
            contentBoxList.add(manifest);

            logger.debug("A new Manifest has been discovered with type " + manifest.getDescriptionBox().getUuid());

            remainingBytes -= manifest.getBoxSize();
        }

        return contentBoxList;
    }

    @Override
    public void writeContentBoxesToJumbfFile(List<BmffBox> contentBoxList, OutputStream outputStream)
            throws MipamsException {

        for (BmffBox bmffBox : contentBoxList) {

            JumbfBox jumbfBox = (JumbfBox) bmffBox;
            jumbfBoxService.writeToJumbfFile(jumbfBox, outputStream);
        }
    }
}
