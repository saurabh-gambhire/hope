package com.hope.master_service.modules.organization;

import com.hope.master_service.controller.AppController;
import com.hope.master_service.dto.organization.Referrer;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/master/orgs/{orgUuid}/referrers")
@RequiredArgsConstructor
public class ReferrerController extends AppController {

    private final ReferrerService referrerService;

    @GetMapping
    public ResponseEntity<Response> getAll(
            @PathVariable UUID orgUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String search) throws HopeException {
        Page<Referrer> referrers = referrerService.search(orgUuid, search,
                PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy));
        return data(referrers);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Response> getByUuid(@PathVariable UUID orgUuid,
                                               @PathVariable UUID uuid) throws HopeException {
        return data(referrerService.getByUuid(uuid));
    }

    @PostMapping
    public ResponseEntity<Response> create(@PathVariable UUID orgUuid,
                                           @Valid @RequestBody Referrer referrer) throws HopeException {
        Referrer created = referrerService.create(orgUuid, referrer);
        return data(ResponseCode.REFERRER_CREATED,
                messageService.getMessage(ResponseCode.REFERRER_CREATED), created);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<Response> update(@PathVariable UUID orgUuid,
                                           @PathVariable UUID uuid,
                                           @Valid @RequestBody Referrer referrer) throws HopeException {
        Referrer updated = referrerService.update(uuid, referrer);
        return data(ResponseCode.REFERRER_UPDATED,
                messageService.getMessage(ResponseCode.REFERRER_UPDATED), updated);
    }

    @PatchMapping("/{uuid}/archive")
    public ResponseEntity<Response> updateArchiveStatus(@PathVariable UUID orgUuid,
                                                        @PathVariable UUID uuid,
                                                        @RequestParam boolean archive) throws HopeException {
        referrerService.updateArchiveStatus(uuid, archive);
        return success(archive ? ResponseCode.REFERRER_ARCHIVED : ResponseCode.REFERRER_UNARCHIVED);
    }
}
