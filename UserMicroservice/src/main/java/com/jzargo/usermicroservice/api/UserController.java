package com.jzargo.usermicroservice.api;

import com.jzargo.usermicroservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<byte[]> setAvatar(@RequestBody MultipartFile avatar,
                                            @AuthenticationPrincipal Jwt jwt){
        long userId = Long.parseLong(jwt.getClaimAsString("user_id"));
        try{
            userService.changeAvatar(avatar.getBytes(), userId);
            return ResponseEntity.ok(avatar.getBytes());
        } catch (Exception e){
            log.error("Image Cannot be saved because {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Void> registerUser(){

    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse>  getUserById(@PathVariable Long id){

    }
}
