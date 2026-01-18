package com.jzargo.usermicroservice.api;

import com.jzargo.messaging.UserRegisterRequest;
import com.jzargo.usermicroservice.config.properties.ApplicationPropertiesStorage;
import com.jzargo.usermicroservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/users")
public class SecurityUserController {
    private final ApplicationPropertiesStorage applicationPropertiesStorage;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Void> registerUser(
            @Header("#{@applicationPropertiesStorage.headers.keycloakConnection.name}") String keycloakValue,
            UserRegisterRequest urr
            ){
        if(
                keycloakValue != null &&
                !applicationPropertiesStorage.getHeaders().getKeycloakConnection().getValue().contentEquals(keycloakValue)
        ) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build();
        }
        try{
            userService.register(urr);
            return  ResponseEntity.ok().build();
        } catch (Exception e) {
            return  ResponseEntity.badRequest().build();
        }
    }

    @PutMapping
    public ResponseEntity<String> updateUser(
            @Header("#{@applicationPropertiesStorage.headers.keycloakConnection.name}") String keycloakValue,
            @RequestBody UserRegisterRequest request
    ){
        if(
                keycloakValue != null &&
                ! applicationPropertiesStorage.getHeaders().getKeycloakConnection().getValue().contentEquals(keycloakValue)
        ){
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
            @Header("#{@applicationPropertiesStorage.headers.keycloakConnection.name}") String keycloakValue,
            @Header("#{@applicationPropertiesStorage.headers.userId}") Long id
    ){
        if(!applicationPropertiesStorage.getHeaders().getKeycloakConnection()
                .getValue().equals(keycloakValue)){
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
