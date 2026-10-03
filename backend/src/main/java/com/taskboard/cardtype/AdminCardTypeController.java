package com.taskboard.cardtype;

import com.taskboard.cardtype.CardTypeDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 작업 유형(Task/Bug 등) 관리. 전역 설정이라 관리자 전용. 조회는 CardTypeController */
@RestController
@RequestMapping("/admin/card-types")
@RequiredArgsConstructor
public class AdminCardTypeController {

    private final CardTypeService cardTypeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardTypeResponse create(@Valid @RequestBody CardTypeRequest request) {
        return cardTypeService.create(request);
    }

    @PatchMapping("/{typeId}")
    public CardTypeResponse update(@PathVariable Long typeId, @Valid @RequestBody CardTypeRequest request) {
        return cardTypeService.update(typeId, request);
    }

    @DeleteMapping("/{typeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long typeId) {
        cardTypeService.delete(typeId);
    }
}
