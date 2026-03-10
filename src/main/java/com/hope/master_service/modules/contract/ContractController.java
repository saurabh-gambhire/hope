package com.hope.master_service.modules.contract;

import com.hope.master_service.controller.AppController;
import com.hope.master_service.dto.contract.Contract;
import com.hope.master_service.dto.enums.ContractStatus;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/master/contracts")
@RequiredArgsConstructor
public class ContractController extends AppController {

    private final ContractService contractService;

    // ======================== LIST & SEARCH ========================

    @GetMapping
    public ResponseEntity<Response> search(
            @RequestParam UUID organizationUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<ContractStatus> statuses,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean isTemplate,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo) throws HopeException {
        Page<Contract> contracts = contractService.search(organizationUuid, search, statuses, active,
                isTemplate, createdBy, createdFrom, createdTo,
                PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy));
        return data(contracts);
    }

    // ======================== GET BY UUID ========================

    @GetMapping("/{uuid}")
    public ResponseEntity<Response> getByUuid(@PathVariable UUID uuid) throws HopeException {
        return data(contractService.getByUuid(uuid));
    }

    // ======================== CREATE ========================

    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody Contract contract) throws HopeException {
        Contract created = contractService.create(contract);
        return data(ResponseCode.CONTRACT_CREATED,
                messageService.getMessage(ResponseCode.CONTRACT_CREATED), created);
    }

    // ======================== UPDATE ========================

    @PutMapping("/{uuid}")
    public ResponseEntity<Response> update(@PathVariable UUID uuid,
                                           @RequestBody Contract contract) throws HopeException {
        Contract updated = contractService.update(uuid, contract);
        return data(ResponseCode.CONTRACT_UPDATED,
                messageService.getMessage(ResponseCode.CONTRACT_UPDATED), updated);
    }

    // ======================== STATUS ========================

    @PatchMapping("/{uuid}/status")
    public ResponseEntity<Response> updateStatus(@PathVariable UUID uuid,
                                                  @RequestParam ContractStatus status) throws HopeException {
        contractService.updateStatus(uuid, status);
        return switch (status) {
            case ACTIVE -> success(ResponseCode.CONTRACT_ACTIVATED);
            case SUSPENDED -> success(ResponseCode.CONTRACT_SUSPENDED);
            case EXPIRED -> success(ResponseCode.CONTRACT_DEACTIVATED);
            default -> success(ResponseCode.CONTRACT_UPDATED);
        };
    }

    // ======================== ARCHIVE ========================

    @PatchMapping("/{uuid}/archive")
    public ResponseEntity<Response> updateArchiveStatus(@PathVariable UUID uuid,
                                                         @RequestParam boolean archive) throws HopeException {
        contractService.updateArchiveStatus(uuid, archive);
        return success(archive ? ResponseCode.CONTRACT_ARCHIVED : ResponseCode.CONTRACT_UNARCHIVED);
    }

    // ======================== CLONE ========================

    @PostMapping("/{uuid}/clone")
    public ResponseEntity<Response> cloneContract(@PathVariable UUID uuid,
                                                   @RequestBody(required = false) Map<String, String> payload) throws HopeException {
        String newName = payload != null ? payload.get("name") : null;
        Contract cloned = contractService.cloneContract(uuid, newName);
        return data(ResponseCode.CONTRACT_CLONED,
                messageService.getMessage(ResponseCode.CONTRACT_CLONED), cloned);
    }

    // ======================== SUB-ORG ASSIGNMENT ========================

    @PostMapping("/{uuid}/sub-organizations/{subOrgUuid}")
    public ResponseEntity<Response> assignSubOrganization(@PathVariable UUID uuid,
                                                           @PathVariable UUID subOrgUuid) throws HopeException {
        Contract updated = contractService.assignSubOrganization(uuid, subOrgUuid);
        return data(ResponseCode.CONTRACT_UPDATED,
                messageService.getMessage(ResponseCode.CONTRACT_UPDATED), updated);
    }

    @DeleteMapping("/{uuid}/sub-organizations/{subOrgUuid}")
    public ResponseEntity<Response> unassignSubOrganization(@PathVariable UUID uuid,
                                                             @PathVariable UUID subOrgUuid) throws HopeException {
        contractService.unassignSubOrganization(uuid, subOrgUuid);
        return success(ResponseCode.CONTRACT_UPDATED);
    }

    // ======================== CONTRACTS BY SUB-ORG ========================

    @GetMapping("/by-sub-organization/{subOrgUuid}")
    public ResponseEntity<Response> getBySubOrganization(@PathVariable UUID subOrgUuid) throws HopeException {
        return data(contractService.getBySubOrganization(subOrgUuid));
    }
}
