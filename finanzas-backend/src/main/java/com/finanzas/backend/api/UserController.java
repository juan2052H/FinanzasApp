package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.AuthDtos;
import com.finanzas.backend.api.dto.UserSettingsDtos;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.repo.UserRepository;
import com.finanzas.backend.service.AuthApplicationService;
import com.finanzas.backend.service.AvatarStorageService;
import com.finanzas.backend.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final UserRepository users;
    private final AvatarStorageService avatarStorage;
    private final UserSettingsService userSettings;

    public UserController(UserRepository users, AvatarStorageService avatarStorage, UserSettingsService userSettings) {
        this.users = users;
        this.avatarStorage = avatarStorage;
        this.userSettings = userSettings;
    }

    @GetMapping
    public AuthDtos.UserResponse me(Authentication authentication) {
        return AuthApplicationService.toUserResponse(requireUser(authentication));
    }

    @PatchMapping
    @Transactional
    public AuthDtos.UserResponse update(Authentication authentication, @Valid @RequestBody AuthDtos.UserPatchRequest request) {
        UserEntity user = requireUser(authentication);
        String normalizedEmail = UserEntity.normalizeEmail(request.email());
        users.findByEmail(normalizedEmail).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese correo ya esta en uso.");
            }
        });
        user.updateProfile(request.nombre(), request.apellido(), normalizedEmail,
                request.ciudad(), request.pais(), request.moneda(), request.locale());
        return AuthApplicationService.toUserResponse(user);
    }

    @GetMapping("/settings")
    public UserSettingsDtos.UserSettingsResponse settings(Authentication authentication) {
        return userSettings.get(requireUser(authentication));
    }

    @PatchMapping("/settings")
    public UserSettingsDtos.UserSettingsResponse updateSettings(
            Authentication authentication,
            @RequestBody UserSettingsDtos.UserSettingsPatchRequest request) {
        return userSettings.update(requireUser(authentication), request);
    }

    @GetMapping(value = "/avatar", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> avatar(
            Authentication authentication,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) throws IOException {
        UserEntity user = requireUser(authentication);
        AvatarStorageService.AvatarResource avatar = avatarStorage.load(user.getId(), ifNoneMatch);
        if (avatar == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no tiene avatar.");
        }
        ResponseEntity.BodyBuilder response = avatar.notModified()
                ? ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                : ResponseEntity.ok();
        response.contentType(MediaType.IMAGE_PNG)
                .eTag(avatar.etag())
                .lastModified(avatar.lastModified())
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePrivate());
        return avatar.notModified() ? response.build() : response.body(avatar.content());
    }

    @PostMapping("/avatar")
    @Transactional
    public AuthDtos.UserResponse avatar(Authentication authentication, @RequestPart("file") MultipartFile file) throws IOException {
        UserEntity user = requireUser(authentication);
        user.setAvatarRef(avatarStorage.store(user.getId(), file));
        return AuthApplicationService.toUserResponse(user);
    }

    @DeleteMapping("/avatar")
    @Transactional
    public ResponseEntity<Void> deleteAvatar(Authentication authentication) throws IOException {
        UserEntity user = requireUser(authentication);
        avatarStorage.delete(user.getId());
        user.setAvatarRef(null);
        return ResponseEntity.noContent().build();
    }

    private UserEntity requireUser(Authentication authentication) {
        return users.findById(CurrentUser.id(authentication))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));
    }
}
