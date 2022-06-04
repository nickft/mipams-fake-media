package org.mipams.fake_media.services.content_types;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.List;

import org.mipams.fake_media.utils.ProvenanceUtils;
import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.CborBox;
import org.mipams.jumbf.core.entities.ParseMetadata;
import org.mipams.jumbf.core.services.boxes.CborBoxService;
import org.mipams.jumbf.core.util.MipamsException;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

@Service
public class ClaimContentType implements ProvenanceContentType {

    @Autowired
    CborBoxService cborBoxService;

    @Override
    public String getContentTypeUuid() {
        return "6D70636C-0011-0010-8000-00AA00389B71";
    }

    @Override
    public String getLabel() {
        return "mipams.provenance.claim";
    }

    @Override
    public List<BmffBox> parseContentBoxesFromJumbfFile(InputStream input, ParseMetadata parseMetadata)
            throws MipamsException {

        String claimDir = ProvenanceUtils.createSubdirectory(parseMetadata.getParentDirectory(), getLabel());

        ParseMetadata claimParseMetadata = new ParseMetadata();
        claimParseMetadata.setAvailableBytesForBox(parseMetadata.getAvailableBytesForBox());
        claimParseMetadata.setParentDirectory(claimDir);

        return List.of(cborBoxService.parseFromJumbfFile(input, claimParseMetadata));
    }

    @Override
    public void writeContentBoxesToJumbfFile(List<BmffBox> contentBoxList, OutputStream outputStream)
            throws MipamsException {
        CborBox cborBox = (CborBox) contentBoxList.get(0);
        cborBoxService.writeToJumbfFile(cborBox, outputStream);
    }
}
