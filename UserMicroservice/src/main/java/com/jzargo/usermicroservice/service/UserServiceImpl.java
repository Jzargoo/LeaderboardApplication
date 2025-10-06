package com.jzargo.usermicroservice.service;

import com.jzargo.usermicroservice.entity.User;
import com.jzargo.usermicroservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class UserServiceImpl implements UserService{
    private final ImageService imageService;
    private final UserRepository userRepository;

    public UserServiceImpl(ImageService imageService, UserRepository userRepository) {
        this.imageService = imageService;
        this.userRepository = userRepository;
    }

    @Override
    public byte[] changeAvatar(byte[] avatar, long userId) {
        User byId = userRepository.findById(userId)
                .orElseThrow();
        String newAvatar = imageService.saveImage(avatar);
        byId.setAvatar(newAvatar);
        imageService.deleteImage(byId.getAvatar());
        userRepository.save(byId);
        return avatar;
    }


}
