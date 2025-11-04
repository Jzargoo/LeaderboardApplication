package com.jzargo.usermicroservice.api;

import com.jzargo.messaging.UserRegisterRequest;
import com.jzargo.usermicroservice.api.model.UserResponse;
import com.jzargo.usermicroservice.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    private final UserService userService;
    @Value("${api.keycloak-connection-header}")
    private String keycloakHeader;
    @Value("${api.secret-keycloak-connection}")
    private String keycloakValue;


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

    @PostMapping("/internal")
    public ResponseEntity<Void> registerUser(
            HttpHeaders headers,
            @RequestBody UserRegisterRequest request
    ){
        String secret = headers.getFirst(keycloakHeader);
        if(secret == null || secret.equals(keycloakValue)){
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }
        try{
            userService.register(request);
            return  ResponseEntity.ok().build();
        } catch (Exception e) {
            return  ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse>  getUserById(@PathVariable Long id){
        try{
            UserResponse response = userService.findById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping
    public ResponseEntity<String> updateUser(
            HttpHeaders headers,
            @RequestBody UserRegisterRequest request
    ){
        String secret = headers.getFirst(keycloakHeader);
        if(secret == null || secret.equals(keycloakValue)){
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }
        try{
            userService.updateUser(request.getUserId(), request);
            return ResponseEntity.ok("User updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntity<String> deleteUser(
            HttpHeaders headers,
            @RequestParam Long id
    ){
        String secret = headers.getFirst(keycloakHeader);
        if(secret == null || secret.equals(keycloakValue)){
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }
        try{
            userService.deleteUser(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
