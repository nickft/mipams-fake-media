package org.mipams.provenance.services.content_types;

import java.io.OutputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mipams.jumbf.entities.BmffBox;
import org.mipams.jumbf.entities.JumbfBox;
import org.mipams.jumbf.entities.ParseMetadata;
import org.mipams.jumbf.services.boxes.JumbfBoxService;
import org.mipams.jumbf.util.MipamsException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ManifestContentType implements ProvenanceContentType {

    private static final Logger logger = Logger.getLogger(ManifestStoreContentType.class.getName());

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Override
    public String getLabel() {
        return "urn:uuid";
    }

    @Override
    public List<BmffBox> parseContentBoxesFromJumbfFile(InputStream input, ParseMetadata parseMetadata)
            throws MipamsException {

        logger.log(Level.FINE, "Start parsing a new Manifest");

        List<BmffBox> contentBoxList = new ArrayList<>();

        long remainingBytes = parseMetadata.getAvailableBytesForBox();

        while (remainingBytes > 0) {

            ParseMetadata manifestContentParseMetadata = new ParseMetadata();
            manifestContentParseMetadata.setAvailableBytesForBox(remainingBytes);
            manifestContentParseMetadata.setParentDirectory(parseMetadata.getParentDirectory());

            JumbfBox jumbfBox = jumbfBoxService.parseFromJumbfFile(input, manifestContentParseMetadata);

            logger.log(Level.FINE, "Discovered a new jumbf box with label: " + jumbfBox.getDescriptionBox().getLabel());

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
