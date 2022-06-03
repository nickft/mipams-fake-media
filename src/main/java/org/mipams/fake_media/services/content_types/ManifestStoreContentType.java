package org.mipams.fake_media.services.content_types;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.services.boxes.JumbfBoxService;
import org.mipams.jumbf.core.util.MipamsException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ManifestStoreContentType implements ProvenanceContentType {

    private static final Logger logger = LoggerFactory.getLogger(ManifestStoreContentType.class);

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Override
    public String getContentTypeUuid() {
        return "6D707374-C65D-11EC-9D64-0242AC120002";
    }

    @Override
    public String getLabel() {
        return "mipams.provenance";
    }

    @Override
    public List<BmffBox> parseContentBoxesFromJumbfFile(InputStream input, long availableBytesForBox)
            throws MipamsException {

        logger.debug("Start parsing a new Manifest Store");

        List<BmffBox> contentBoxList = new ArrayList<>();

        long remainingBytes = availableBytesForBox;

        while (remainingBytes > 0) {

            JumbfBox manifest = jumbfBoxService.parseFromJumbfFile(input, remainingBytes);
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
