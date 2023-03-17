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
import org.springframework.stereotype.Service;

@Service
public class ManifestStoreContentType implements ProvenanceContentType {

    private static final Logger logger = Logger.getLogger(ManifestStoreContentType.class.getName());

    @Autowired
    JumbfBoxService jumbfBoxService;

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

        logger.log(Level.FINE, "Start parsing a new Manifest Store");

        List<BmffBox> contentBoxList = new ArrayList<>();

        long remainingBytes = parseMetadata.getAvailableBytesForBox();

        while (remainingBytes > 0) {

            ParseMetadata manifestParseMetadata = new ParseMetadata();
            manifestParseMetadata.setAvailableBytesForBox(remainingBytes);
            manifestParseMetadata.setParentDirectory(parseMetadata.getParentDirectory());

            JumbfBox manifest = jumbfBoxService.parseFromJumbfFile(input, manifestParseMetadata);
            contentBoxList.add(manifest);

            logger.log(Level.FINE,
                    "A new Manifest has been discovered with type " + manifest.getDescriptionBox().getUuid());

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
