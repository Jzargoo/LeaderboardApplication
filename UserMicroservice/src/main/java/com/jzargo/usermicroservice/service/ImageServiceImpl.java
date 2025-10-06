package com.jzargo.usermicroservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Slf4j
public class ImageServiceImpl implements ImageService{
    @Value("${images.dir-path.user}")
    private String dirUserPath;
    @Value("${images.dump.user}")
    private String dumpUserAvatar;

    @Override
    public String saveImage(byte[] image) {
        try {
            Files.createDirectories(Path.of(dirUserPath));
            log.debug("created directories if needed");
            String filename = UUID.randomUUID().toString();
            Files.write(Path.of(STR."\{dirUserPath}/\{filename}.png"), image);
            log.info("Image saved successfully");
            return filename;
        } catch (IOException e) {
            log.error("Occurred error while processing image {}", e.toString());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteImage(String avatar) {
        if(!avatar.equalsIgnoreCase(dumpUserAvatar)){
            try {
                Files.deleteIfExists(Path.of(dirUserPath + avatar));
                log.info("Image was deleted successful");
            } catch (IOException e) {
                log.error("Occurred error while processing image {}", e.toString());
                throw new RuntimeException(e);
            }
        }
    }
}
