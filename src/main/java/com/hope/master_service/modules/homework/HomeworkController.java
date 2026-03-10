package com.hope.master_service.modules.homework;

import com.hope.master_service.controller.AppController;
import com.hope.master_service.dto.homework.Homework;
import com.hope.master_service.dto.homework.HomeworkCurriculum;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/master/homework")
@RequiredArgsConstructor
public class HomeworkController extends AppController {

    private final HomeworkService homeworkService;

    // ======================== HOMEWORK LIBRARY CRUD ========================

    @GetMapping
    public ResponseEntity<Response> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID subOrganizationUuid,
            @RequestParam(required = false) UUID contractUuid,
            @RequestParam(required = false) Boolean active) throws HopeException {
        Page<Homework> homeworkPage = homeworkService.search(search, subOrganizationUuid, contractUuid, active,
                PageRequest.of(page, size, Sort.Direction.fromString(sortDirection), sortBy));
        return data(homeworkPage);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Response> getByUuid(@PathVariable UUID uuid) throws HopeException {
        return data(homeworkService.getByUuid(uuid));
    }

    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody Homework homework) throws HopeException {
        Homework created = homeworkService.create(homework);
        return data(ResponseCode.HOMEWORK_CREATED,
                messageService.getMessage(ResponseCode.HOMEWORK_CREATED), created);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<Response> update(@PathVariable UUID uuid,
                                           @Valid @RequestBody Homework homework) throws HopeException {
        Homework updated = homeworkService.update(uuid, homework);
        return data(ResponseCode.HOMEWORK_UPDATED,
                messageService.getMessage(ResponseCode.HOMEWORK_UPDATED), updated);
    }

    @PatchMapping("/{uuid}/archive")
    public ResponseEntity<Response> updateArchiveStatus(@PathVariable UUID uuid,
                                                         @RequestParam boolean archive) throws HopeException {
        homeworkService.updateArchiveStatus(uuid, archive);
        return success(archive ? ResponseCode.HOMEWORK_ARCHIVED : ResponseCode.HOMEWORK_UNARCHIVED);
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Response> delete(@PathVariable UUID uuid) throws HopeException {
        homeworkService.delete(uuid);
        return success(ResponseCode.HOMEWORK_DELETED);
    }

    // ======================== DOCUMENT MANAGEMENT ========================

    @PostMapping("/{uuid}/document/replace")
    public ResponseEntity<Response> replaceDocument(@PathVariable UUID uuid,
                                                     @RequestBody Map<String, Object> payload) throws HopeException {
        String document = (String) payload.get("document");
        String fileName = (String) payload.get("fileName");
        Long fileSize = payload.get("fileSize") != null ? ((Number) payload.get("fileSize")).longValue() : null;

        Homework updated = homeworkService.replaceDocument(uuid, document, fileName, fileSize);
        return data(ResponseCode.HOMEWORK_DOCUMENT_REPLACED,
                messageService.getMessage(ResponseCode.HOMEWORK_DOCUMENT_REPLACED), updated);
    }

    @DeleteMapping("/{uuid}/document")
    public ResponseEntity<Response> removeDocument(@PathVariable UUID uuid) throws HopeException {
        Homework updated = homeworkService.removeDocument(uuid);
        return data(ResponseCode.HOMEWORK_DOCUMENT_REMOVED,
                messageService.getMessage(ResponseCode.HOMEWORK_DOCUMENT_REMOVED), updated);
    }

    @GetMapping("/{uuid}/document/url")
    public ResponseEntity<Response> getDocumentUrl(@PathVariable UUID uuid) throws HopeException {
        String url = homeworkService.getDocumentUrl(uuid);
        return data(Map.of("url", url));
    }

    @GetMapping("/{uuid}/document/versions")
    public ResponseEntity<Response> getDocumentVersions(@PathVariable UUID uuid) throws HopeException {
        return data(homeworkService.getDocumentVersions(uuid));
    }

    // ======================== CURRICULUM MANAGEMENT ========================

    @GetMapping("/curriculum")
    public ResponseEntity<Response> getCurriculum(@RequestParam UUID subOrganizationUuid,
                                                   @RequestParam UUID contractUuid) throws HopeException {
        return data(homeworkService.getCurriculum(subOrganizationUuid, contractUuid));
    }

    @PostMapping("/curriculum")
    public ResponseEntity<Response> addToCurriculum(@Valid @RequestBody HomeworkCurriculum curriculum) throws HopeException {
        HomeworkCurriculum created = homeworkService.addToCurriculum(curriculum);
        return data(ResponseCode.HOMEWORK_CURRICULUM_UPDATED,
                messageService.getMessage(ResponseCode.HOMEWORK_CURRICULUM_UPDATED), created);
    }

    @DeleteMapping("/curriculum/{uuid}")
    public ResponseEntity<Response> removeFromCurriculum(@PathVariable UUID uuid) throws HopeException {
        homeworkService.removeFromCurriculum(uuid);
        return success(ResponseCode.HOMEWORK_CURRICULUM_UPDATED);
    }

    @PutMapping("/curriculum/order")
    public ResponseEntity<Response> updateCurriculumOrder(
            @RequestParam UUID subOrganizationUuid,
            @RequestParam UUID contractUuid,
            @RequestBody List<HomeworkCurriculum> orderedItems) throws HopeException {
        homeworkService.updateCurriculumOrder(subOrganizationUuid, contractUuid, orderedItems);
        return success(ResponseCode.HOMEWORK_CURRICULUM_UPDATED);
    }

    @GetMapping("/client-curriculum")
    public ResponseEntity<Response> getClientCurriculum(@RequestParam UUID subOrganizationUuid,
                                                         @RequestParam UUID contractUuid) throws HopeException {
        return data(homeworkService.getClientCurriculum(subOrganizationUuid, contractUuid));
    }
}
