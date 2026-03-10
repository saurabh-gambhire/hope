package com.hope.master_service.modules.organization;

import com.hope.master_service.controller.AppController;
import com.hope.master_service.dto.enums.SubOrganizationType;
import com.hope.master_service.dto.organization.SubOrganization;
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
@RequestMapping("/api/master/orgs/{orgUuid}/sub-orgs")
@RequiredArgsConstructor
public class SubOrganizationController extends AppController {

    private final SubOrganizationService subOrganizationService;

    @GetMapping
    public ResponseEntity<Response> getAll(
            @PathVariable UUID orgUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) List<SubOrganizationType> types,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo) throws HopeException {
        Page<SubOrganization> subOrgs = subOrganizationService.search(orgUuid, search, active, types,
                createdBy, createdFrom, createdTo,
                PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy));
        return data(subOrgs);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Response> getByUuid(@PathVariable UUID orgUuid,
                                               @PathVariable UUID uuid) throws HopeException {
        return data(subOrganizationService.getByUuid(uuid));
    }

    @PostMapping
    public ResponseEntity<Response> create(@PathVariable UUID orgUuid,
                                           @Valid @RequestBody SubOrganization subOrganization) throws HopeException {
        SubOrganization created = subOrganizationService.create(orgUuid, subOrganization);
        return data(ResponseCode.SUB_ORGANIZATION_CREATED,
                messageService.getMessage(ResponseCode.SUB_ORGANIZATION_CREATED), created);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<Response> update(@PathVariable UUID orgUuid,
                                           @PathVariable UUID uuid,
                                           @Valid @RequestBody SubOrganization subOrganization) throws HopeException {
        SubOrganization updated = subOrganizationService.update(uuid, subOrganization);
        return data(ResponseCode.SUB_ORGANIZATION_UPDATED,
                messageService.getMessage(ResponseCode.SUB_ORGANIZATION_UPDATED), updated);
    }

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<Response> updateStatus(@PathVariable UUID orgUuid,
                                                  @PathVariable UUID uuid,
                                                  @RequestParam boolean active) throws HopeException {
        subOrganizationService.updateStatus(uuid, active);
        return success(active ? ResponseCode.SUB_ORGANIZATION_ACTIVATED : ResponseCode.SUB_ORGANIZATION_DEACTIVATED);
    }

    @PatchMapping("/{uuid}/archive")
    public ResponseEntity<Response> updateArchiveStatus(@PathVariable UUID orgUuid,
                                                        @PathVariable UUID uuid,
                                                        @RequestParam boolean archive) throws HopeException {
        subOrganizationService.updateArchiveStatus(uuid, archive);
        return success(archive ? ResponseCode.SUB_ORGANIZATION_ARCHIVED : ResponseCode.SUB_ORGANIZATION_UNARCHIVED);
    }
}
