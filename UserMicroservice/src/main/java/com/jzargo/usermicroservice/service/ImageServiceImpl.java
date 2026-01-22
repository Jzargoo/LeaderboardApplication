package com.jzargo.usermicroservice.service;

import com.jzargo.usermicroservice.config.properties.ApplicationPropertiesStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService{

    private final ApplicationPropertiesStorage applicationPropertiesStorage;

    @Override
    public String saveImage(byte[] image) {
        try {

            Files.createDirectories(Path.of(
                    applicationPropertiesStorage.getImages().getDirPath().getUser()
            ));
            log.debug("created directories if needed");

            String filename = UUID.randomUUID() + ".png";
            Files.write(
                    Path.of(
                            applicationPropertiesStorage.getImages().getDirPath().getUser()
                                    + "/" + filename
                    ),
                    image
            );

            log.info("Image saved successfully");
            return filename;
        } catch (IOException e) {
            log.error("Occurred error while processing image {}", e.toString());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteImage(String avatar) {
        if(!avatar.equalsIgnoreCase(
                applicationPropertiesStorage.getImages().getDump().getUser()
        )){
            try {
                Files.deleteIfExists(
                        Path.of(
                                applicationPropertiesStorage.getImages().getDirPath().getUser() +
                                        "/" +
                                        avatar
                        )
                );
                log.info("Image was deleted successful");
            } catch (IOException e) {
                log.error("Occurred error while processing image {}", e.toString());
                throw new RuntimeException(e);
            }
        }
    }
}
