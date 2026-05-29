package com.jzargo.productservice.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface ImageDriver {
    String saveFile(byte[] image) throws IOException;

    default List<String> saveFiles(byte[][] images) throws IOException{

        List<String> names = new ArrayList<>();

        for(byte[] image: images) {
            names.add(
                    saveFile(image)
            );
        }

        return names;
    }

    default List<byte[]> getImages(List<String> names) throws IOException{
        List<byte[]>  images = new ArrayList<>();

        for (String name: names){
            images.add(
                    getImage(name)
            );

        }

        return images;
    }

    byte[] getImage(String name) throws IOException;
}
