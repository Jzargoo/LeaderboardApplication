package com.jzargo.usermicroservice.service;

import com.jzargo.messaging.ActiveLeaderboardEvent;
import com.jzargo.messaging.DiedLeaderboardEvent;
import com.jzargo.usermicroservice.api.UserRegisterRequest;
import com.jzargo.usermicroservice.api.UserResponse;
import com.jzargo.usermicroservice.entity.User;
import com.jzargo.usermicroservice.mapper.CreateRegisterUserMapper;
import com.jzargo.usermicroservice.mapper.ReadUserMapper;
import com.jzargo.usermicroservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class UserServiceImpl implements UserService{
    private final ImageService imageService;
    private final UserRepository userRepository;
    private final CreateRegisterUserMapper createRegisterUserMapper;
    private final ReadUserMapper readUserMapper;

    public UserServiceImpl(ImageService imageService, UserRepository userRepository,
                           CreateRegisterUserMapper createRegisterUserMapper,
                           ReadUserMapper readUserMapper) {
        this.imageService = imageService;
        this.userRepository = userRepository;
        this.createRegisterUserMapper = createRegisterUserMapper;
        this.readUserMapper = readUserMapper;
    }

    @Override
    @Transactional
    public void changeAvatar(byte[] avatar, long userId) {
        User byId = userRepository.findById(userId)
                .orElseThrow();
        String newAvatar = imageService.saveImage(avatar);
        byId.setAvatar(newAvatar);
        imageService.deleteImage(byId.getAvatar());
        userRepository.save(byId);
    }

    @Override
    public void register(UserRegisterRequest request) {
        User map = createRegisterUserMapper.map(request);
        userRepository.save(map);
    }

    @Override
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(readUserMapper::map)
                .orElseThrow();
    }

    @Override
    public void addActiveLeaderboard(ActiveLeaderboardEvent event) {
        User user = userRepository
                .findById(event.getUserId())
                .orElseThrow();

        user.addActiveLeaderboard(event.getLeaderboardName());
        userRepository.save(user);
    }

    @Override
    public void removeLeaderboard(DiedLeaderboardEvent event) {
        for(Long userId : event.getUserIds()){
            User user = userRepository.findById(userId)
                    .orElseThrow();

            user.removeActiveLeaderboard(event.getLeaderboardName());
            userRepository.save(user);
        }
    }
}
