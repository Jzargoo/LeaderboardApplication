package com.jzargo.productservice.service;

import com.jzargo.productservice.config.ApplicationPropertyStorage;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ImageDriverNative implements ImageDriver{
    private final ApplicationPropertyStorage applicationPropertyStorage;

    public ImageDriverNative(ApplicationPropertyStorage applicationPropertyStorage) {
        this.applicationPropertyStorage = applicationPropertyStorage;
    }

    @Override
    public String saveFile(byte[] image) throws IOException  {
        String path = applicationPropertyStorage.getImage().getPath();

        Files.createDirectories(Path.of(path));

        var image_name = "image" + UUID.randomUUID() + ".png";
        var pathToFile = Path.of(path + "/" + image_name);

        Files.createFile(pathToFile);

        Files.write(pathToFile, image);

        return image_name;
    }



    @Override
    public byte[] getImage(String name)
            throws IOException {

        Path path = Path.of(
                applicationPropertyStorage.getImage().getPath() + "/" +
                        name
        );

        return Files.readAllBytes(path);
    }
}
