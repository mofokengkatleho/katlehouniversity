package com.katlehouniversity.ecd.controller;

import com.katlehouniversity.ecd.dto.ChildDto;
import com.katlehouniversity.ecd.service.ChildService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/children")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ChildController {

    private final ChildService childService;

    @GetMapping
    public ResponseEntity<List<ChildDto>> getAllChildren(
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        List<ChildDto> children = activeOnly ?
                childService.getActiveChildren() :
                childService.getAllChildren();
        return ResponseEntity.ok(children);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChildDto> getChildById(@PathVariable Long id) {
        return ResponseEntity.ok(childService.getChildById(id));
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<ChildDto> getChildByReference(@PathVariable String reference) {
        return ResponseEntity.ok(childService.getChildByPaymentReference(reference));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ChildDto>> searchChildren(@RequestParam String name) {
        return ResponseEntity.ok(childService.searchChildren(name));
    }

    @PostMapping
    public ResponseEntity<ChildDto> createChild(@Valid @RequestBody ChildDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(childService.createChild(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChildDto> updateChild(
            @PathVariable Long id,
            @Valid @RequestBody ChildDto dto) {
        return ResponseEntity.ok(childService.updateChild(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChild(@PathVariable Long id) {
        childService.deleteChild(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getActiveChildrenCount() {
        return ResponseEntity.ok(childService.getActiveChildrenCount());
    }
}
