package org.mipams.fake_media.services.content_types;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.List;

import org.mipams.fake_media.utils.ProvenanceUtils;
import org.mipams.jumbf.core.entities.BmffBox;
import org.mipams.jumbf.core.entities.JsonBox;
import org.mipams.jumbf.core.entities.ParseMetadata;
import org.mipams.jumbf.core.services.boxes.JsonBoxService;
import org.mipams.jumbf.core.util.MipamsException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CredentialStoreContentType implements ProvenanceContentType {

    @Autowired
    JsonBoxService jsonBoxService;

    @Override
    public String getContentTypeUuid() {
        return "6D707663-0011-0010-8000-00AA00389B71";
    }

    @Override
    public String getLabel() {
        return "mipams.provenance.credentials";
    }

    @Override
    public List<BmffBox> parseContentBoxesFromJumbfFile(InputStream input, ParseMetadata parseMetadata)
            throws MipamsException {

        String credentialStoreDir = ProvenanceUtils.createSubdirectory(parseMetadata.getParentDirectory(), getLabel());

        ParseMetadata credentialStoreParseMetadata = new ParseMetadata();
        credentialStoreParseMetadata.setAvailableBytesForBox(parseMetadata.getAvailableBytesForBox());
        credentialStoreParseMetadata.setParentDirectory(credentialStoreDir);

        return List.of(jsonBoxService.parseFromJumbfFile(input, credentialStoreParseMetadata));
    }

    @Override
    public void writeContentBoxesToJumbfFile(List<BmffBox> contentBoxList, OutputStream outputStream)
            throws MipamsException {

        JsonBox jsonBox = (JsonBox) contentBoxList.get(0);
        jsonBoxService.writeToJumbfFile(jsonBox, outputStream);
    }
}
