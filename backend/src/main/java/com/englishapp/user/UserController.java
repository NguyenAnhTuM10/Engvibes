package com.englishapp.user;

import com.englishapp.common.ApiResponse;
import com.englishapp.user.dto.UpdateUserRequest;
import com.englishapp.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "User profile")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser() {
        return ApiResponse.ok(userService.getCurrentUser());
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateCurrentUser(@Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userService.updateUser(request));
    }
}
