package com.jzargo.productservice.unit;

import com.jzargo.productservice.config.ApplicationPropertyStorage;
import com.jzargo.productservice.service.ImageDriverNative;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageDriverUnitTest {

    @MockitoBean
    private ApplicationPropertyStorage applicationPropertyStorage;

    @InjectMocks
    private ImageDriverNative imageDriverNative;

    @TempDir
    private Path sharedTempDir;

    @Test
    public void saveFile_test_success(){

        var image = new ApplicationPropertyStorage.Image();

        image.setPath(
                sharedTempDir.toAbsolutePath().toString()
        );

        byte[] content = {1,1,1,1,1,1};

        Mockito.when(
                applicationPropertyStorage.getImage()
        ).thenReturn(image);

        try {

            String name = imageDriverNative.saveFile(content);

            Assertions.assertTrue(
                    Files.exists(
                            Path.of(image.getPath() + "\\" + name)
                    )
                    , "The saved file does not exist");

            Assertions.assertEquals(content, Files.readAllBytes(
                    Path.of(image.getPath() + "\\" + name)
            ), "content does not match with actual file");

        } catch (IOException e) {
            Assertions.fail("some operations threw an io exception either save file method or read all bytes");
        }
    }
}
