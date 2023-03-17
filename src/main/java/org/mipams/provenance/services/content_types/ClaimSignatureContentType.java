package org.mipams.provenance.services.content_types;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.List;

import org.mipams.jumbf.entities.BmffBox;
import org.mipams.jumbf.entities.CborBox;
import org.mipams.jumbf.entities.ParseMetadata;
import org.mipams.jumbf.services.boxes.CborBoxService;
import org.mipams.jumbf.util.CoreUtils;
import org.mipams.jumbf.util.MipamsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClaimSignatureContentType implements ProvenanceContentType {

    @Autowired
    CborBoxService cborBoxService;

    @Override
    public String getContentTypeUuid() {
        return "6D706373-0011-0010-8000-00AA00389B71";
    }

    @Override
    public String getLabel() {
        return "mpms.provenance.signature";
    }

    @Override
    public List<BmffBox> parseContentBoxesFromJumbfFile(InputStream input, ParseMetadata parseMetadata)
            throws MipamsException {

        String claimSignatureDir = CoreUtils.createSubdirectory(parseMetadata.getParentDirectory(), getLabel());

        ParseMetadata claimSignatureParseMetadata = new ParseMetadata();
        claimSignatureParseMetadata.setAvailableBytesForBox(parseMetadata.getAvailableBytesForBox());
        claimSignatureParseMetadata.setParentDirectory(claimSignatureDir);

        return List.of(cborBoxService.parseFromJumbfFile(input, claimSignatureParseMetadata));
    }

    @Override
    public void writeContentBoxesToJumbfFile(List<BmffBox> contentBoxList, OutputStream outputStream)
            throws MipamsException {

        CborBox cborBox = (CborBox) contentBoxList.get(0);
        cborBoxService.writeToJumbfFile(cborBox, outputStream);
    }
}
