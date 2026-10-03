package com.taskboard.cardtype;

import com.taskboard.cardtype.CardTypeDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 작업 유형 관리 API (관리자 전용) */
@RestController
@RequestMapping("/admin/card-types")
@RequiredArgsConstructor
public class AdminCardTypeController {

    private final CardTypeService cardTypeService;

    @PostMapping
    public ResponseEntity<CardTypeResponse> create(@Valid @RequestBody CardTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardTypeService.create(request));
    }

    @PatchMapping("/{typeId}")
    public ResponseEntity<CardTypeResponse> update(@PathVariable Long typeId,
                                                   @Valid @RequestBody CardTypeRequest request) {
        return ResponseEntity.ok(cardTypeService.update(typeId, request));
    }

    @DeleteMapping("/{typeId}")
    public ResponseEntity<Void> delete(@PathVariable Long typeId) {
        cardTypeService.delete(typeId);
        return ResponseEntity.noContent().build();
    }
}
