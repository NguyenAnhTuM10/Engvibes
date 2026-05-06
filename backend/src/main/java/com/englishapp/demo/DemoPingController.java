package com.englishapp.demo;

import com.englishapp.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoPingController {

    private final DemoPingService service;

    @GetMapping("/pings")
    public ApiResponse<List<DemoPingDto.PingResponse>> list() {
        return ApiResponse.ok(service.listRecent());
    }

    @PostMapping("/pings")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DemoPingDto.PingResponse> create(
            @Valid @RequestBody DemoPingDto.CreatePingRequest request) {
        return ApiResponse.ok(service.create(request.message()), "Ping created");
    }
}
