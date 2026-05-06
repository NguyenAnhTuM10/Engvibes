package com.englishapp.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DemoPingService {

    private final DemoPingRepository repository;

    public List<DemoPingDto.PingResponse> listRecent() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(p -> new DemoPingDto.PingResponse(p.getId(), p.getMessage(), p.getCreatedAt()))
                .toList();
    }

    public DemoPingDto.PingResponse create(String message) {
        DemoPing ping = new DemoPing();
        ping.setMessage(message);
        DemoPing saved = repository.save(ping);
        return new DemoPingDto.PingResponse(saved.getId(), saved.getMessage(), saved.getCreatedAt());
    }
}
