package com.hope.master_service.modules.provider;

import com.hope.master_service.controller.AppController;
import com.hope.master_service.dto.enums.ProviderType;
import com.hope.master_service.dto.enums.Roles;
import com.hope.master_service.dto.enums.UserStatus;
import com.hope.master_service.dto.provider.Provider;
import com.hope.master_service.dto.response.Response;
import com.hope.master_service.dto.response.ResponseCode;
import com.hope.master_service.exception.HopeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/master/providers")
@RequiredArgsConstructor
public class ProviderController extends AppController {

    private final ProviderService providerService;

    @GetMapping
    public ResponseEntity<Response> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ACTIVE") UserStatus status,
            @RequestParam(required = false) List<Roles> roles,
            @RequestParam(required = false) ProviderType providerType,
            @RequestParam(required = false) Instant lastLoginFrom,
            @RequestParam(required = false) Instant lastLoginTo,
            @RequestParam(required = false) Boolean neverLoggedIn) throws HopeException {
        Page<Provider> providers = providerService.search(
                search, status, roles, providerType, lastLoginFrom, lastLoginTo, neverLoggedIn,
                PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy));
        return data(providers);
    }

    @GetMapping("/status-counts")
    public ResponseEntity<Response> getStatusCounts() {
        return data(providerService.getStatusCounts());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Response> getByUuid(@PathVariable UUID uuid) throws HopeException {
        return data(providerService.getByUuid(uuid));
    }

    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody Provider provider) throws HopeException {
        Provider created = providerService.create(provider);
        return data(ResponseCode.PROVIDER_CREATED,
                messageService.getMessage(ResponseCode.PROVIDER_CREATED), created);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<Response> update(@PathVariable UUID uuid,
                                           @Valid @RequestBody Provider provider) throws HopeException {
        Provider updated = providerService.update(uuid, provider);
        return data(ResponseCode.PROVIDER_UPDATED,
                messageService.getMessage(ResponseCode.PROVIDER_UPDATED), updated);
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<Response> updateStatus(@PathVariable UUID uuid,
                                                 @RequestParam boolean active) throws HopeException {
        providerService.updateStatus(uuid, active);
        return success(active ? ResponseCode.PROVIDER_ENABLED : ResponseCode.PROVIDER_DISABLED);
    }

    @PatchMapping("/{uuid}/archive")
    public ResponseEntity<Response> updateArchiveStatus(@PathVariable UUID uuid,
                                                        @RequestParam boolean archive) throws HopeException {
        providerService.updateArchiveStatus(uuid, archive);
        return success(archive ? ResponseCode.PROVIDER_ARCHIVED : ResponseCode.PROVIDER_UNARCHIVED);
    }
}
