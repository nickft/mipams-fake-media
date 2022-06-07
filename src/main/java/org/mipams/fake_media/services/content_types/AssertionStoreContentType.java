package org.mipams.fake_media.services.content_types;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.JumbfBox;
import org.mipams.jumbf.core.entities.ParseMetadata;
import org.mipams.jumbf.core.services.boxes.JumbfBoxService;
import org.mipams.jumbf.core.util.CoreUtils;
import org.mipams.jumbf.core.util.MipamsException;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

@Service
public class AssertionStoreContentType implements ProvenanceContentType {

    @Autowired
    JumbfBoxService jumbfBoxService;

    @Override
    public String getContentTypeUuid() {
        return "6D706173-C65D-11EC-9D64-0242AC120002";
    }

    @Override
    public String getLabel() {
        return "mipams.provenance.assertions";
    }

    @Override
    public List<BmffBox> parseContentBoxesFromJumbfFile(InputStream input, ParseMetadata parseMetadata)
            throws MipamsException {

        List<BmffBox> contentBoxList = new ArrayList<>();

        long remainingBytes = parseMetadata.getAvailableBytesForBox();

        String assertionStoreDir = CoreUtils.createSubdirectory(parseMetadata.getParentDirectory(), getLabel());
        ParseMetadata assertionParseMetadata;

        while (remainingBytes > 0) {

            assertionParseMetadata = new ParseMetadata();
            assertionParseMetadata.setAvailableBytesForBox(remainingBytes);
            assertionParseMetadata.setParentDirectory(assertionStoreDir);

            JumbfBox assertionBox = jumbfBoxService.parseFromJumbfFile(input, assertionParseMetadata);
            contentBoxList.add(assertionBox);

            remainingBytes -= assertionBox.getBoxSize();
        }

        return contentBoxList;
    }

    @Override
    public void writeContentBoxesToJumbfFile(List<BmffBox> contentBoxList, OutputStream outputStream)
            throws MipamsException {

        for (BmffBox bmffBox : contentBoxList) {
            JumbfBox assertionBox = (JumbfBox) bmffBox;

            jumbfBoxService.writeToJumbfFile(assertionBox, outputStream);
        }
    }
}
