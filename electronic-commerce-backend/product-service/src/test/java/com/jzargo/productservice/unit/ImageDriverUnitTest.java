package com.jzargo.productservice.unit;

import com.jzargo.productservice.config.ApplicationPropertyStorage;
import com.jzargo.productservice.service.ImageDriverNative;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// COMPLETED UNIT TEST

@ExtendWith(MockitoExtension.class)
public class ImageDriverUnitTest {

    @Mock
    private ApplicationPropertyStorage applicationPropertyStorage;

    @InjectMocks
    private ImageDriverNative imageDriverNative;

    @TempDir
    private Path sharedTempDir;

    @BeforeEach
    public void setup() {
        var image = new ApplicationPropertyStorage.Image();

        image.setPath(
                sharedTempDir.toAbsolutePath().toString()
        );

        Mockito.when(
                applicationPropertyStorage.getImage()
        ).thenReturn(image);
    }

    @Test
    public void saveFiles_test_success() {

        byte[][] images = {
                {1, 1, 1},
                {2, 2, 2},
                {3, 3, 3}
        };

        try {

            List<String> imageNames = imageDriverNative.saveFiles(images);

            for (int i = 0; i < imageNames.size(); i++) {

                byte[] content = Files.readAllBytes(
                        Path.of(
                                applicationPropertyStorage.getImage()
                                        .getPath() + "/" + imageNames.get(i)
                        )
                );

                Assertions.assertArrayEquals(images[i], content, "content does not match");
            }

        } catch (Exception e) {
            Assertions.fail("driver threw an error" + e.getLocalizedMessage());
        }


    }

    @Test
    public void saveFile_test_success() {

        byte[] content = {1, 1, 1, 1, 1, 1};

        try {

            String name = imageDriverNative.saveFile(content);

            Assertions.assertTrue(
                    Files.exists(
                            Path.of(applicationPropertyStorage.getImage()
                                    .getPath() + "/" + name)
                    )
                    , "The saved file does not exist");

            Assertions.assertArrayEquals(content, Files.readAllBytes(
                    Path.of(applicationPropertyStorage.getImage()
                            .getPath() + "/" + name)
            ), "content does not match with actual file");

        } catch (IOException e) {
            Assertions.fail("some operations threw an io exception either save file method or read all bytes");
        }
    }

    @Test
    public void getImage_test_success() {

        String sPath = applicationPropertyStorage
                .getImage().getPath() + "/";

        String name = "image.png";

        byte[] content = {1,2,3,4};

        try {

            Path  path= Path.of(sPath + name);

            Files.createFile(path);

            Files.write(path, content);

            byte[] actualContent = imageDriverNative.getImage(name);

            Assertions.assertArrayEquals(content, actualContent, "The content did not match");

        } catch (IOException e) {
            Assertions.fail("Test threw unexpected io exception");
        }
    }

    @Test
    public void getImages_test_success() {

        String folder = applicationPropertyStorage
                .getImage().getPath() + "/";

        String name1 = "image1.png";
        String name2 = "image2.png";

        byte[] content = {1,2,3,4};

        try {

            Path  path1 = Path.of(folder + name1);
            Path path2 = Path.of(folder + name2);

            Files.createFile(path1);
            Files.createFile(path2);

            Files.write(path1, content);
            Files.write(path2, content);


            byte[] actualContent1 = imageDriverNative.getImage(name1);
            byte[] actualContent2 = imageDriverNative.getImage(name2);

            Assertions.assertArrayEquals(content, actualContent1, "The content of image 1 did not match");
            Assertions.assertArrayEquals(content, actualContent2, "The content of image 2 did not match");

        } catch (IOException e) {
            Assertions.fail("Test threw unexpected io exception");
        }

    }
}