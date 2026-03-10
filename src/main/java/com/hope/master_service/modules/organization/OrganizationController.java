package com.hope.master_service.modules.organization;

import com.hope.master_service.controller.AppController;
import com.hope.master_service.dto.organization.Organization;
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
@RequestMapping("/api/master/orgs")
@RequiredArgsConstructor
public class OrganizationController extends AppController {

    private final OrganizationService organizationService;

    @GetMapping
    public ResponseEntity<Response> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String search) {
        Page<Organization> organizations = organizationService.search(search,
                PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy));
        return data(organizations);
    }

    @GetMapping("/archived")
    public ResponseEntity<Response> getArchived(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        Page<Organization> organizations = organizationService.getArchived(
                PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy));
        return data(organizations);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Response> getByUuid(@PathVariable UUID uuid) throws HopeException {
        return data(organizationService.getByUuid(uuid));
    }

    @GetMapping("/{uuid}/overview")
    public ResponseEntity<Response> getOverview(@PathVariable UUID uuid) throws HopeException {
        return data(organizationService.getOverview(uuid));
    }

    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody Organization organization) throws HopeException {
        Organization created = organizationService.create(organization);
        return data(ResponseCode.ORGANIZATION_CREATED,
                messageService.getMessage(ResponseCode.ORGANIZATION_CREATED), created);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<Response> update(@PathVariable UUID uuid,
                                           @Valid @RequestBody Organization organization) throws HopeException {
        Organization updated = organizationService.update(uuid, organization);
        return data(ResponseCode.ORGANIZATION_UPDATED,
                messageService.getMessage(ResponseCode.ORGANIZATION_UPDATED), updated);
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<Response> updateStatus(@PathVariable UUID uuid,
                                                  @RequestParam boolean active) throws HopeException {
        organizationService.updateStatus(uuid, active);
        return success(active ? ResponseCode.ORGANIZATION_ACTIVATED : ResponseCode.ORGANIZATION_DEACTIVATED);
    }

    @PatchMapping("/{uuid}/archive")
    public ResponseEntity<Response> updateArchiveStatus(@PathVariable UUID uuid,
                                                        @RequestParam boolean archive) throws HopeException {
        organizationService.updateArchiveStatus(uuid, archive);
        return success(archive ? ResponseCode.ORGANIZATION_ARCHIVED : ResponseCode.ORGANIZATION_UNARCHIVED);
    }
}
