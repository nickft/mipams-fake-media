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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ManifestContentType implements ProvenanceContentType {

    private static final Logger logger = LoggerFactory.getLogger(ManifestContentType.class);

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Override
    public String getLabel() {
        return "Digital asset id";
    }

    @Override
    public List<BmffBox> parseContentBoxesFromJumbfFile(InputStream input, long availableBytesForBox)
            throws MipamsException {

        logger.debug("Start parsing a new Manifest");

        List<BmffBox> contentBoxList = new ArrayList<>();

        long remainingBytes = availableBytesForBox;

        while (remainingBytes > 0) {

            JumbfBox jumbfBox = jumbfBoxService.parseFromJumbfFile(input, remainingBytes);

            logger.debug("Discovered a new jumbf box with label: " + jumbfBox.getDescriptionBox().getLabel());

            contentBoxList.add(jumbfBox);

            remainingBytes -= jumbfBox.getBoxSize();
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
