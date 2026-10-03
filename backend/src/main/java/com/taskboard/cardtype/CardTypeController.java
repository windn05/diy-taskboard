package com.taskboard.cardtype;

import com.taskboard.cardtype.CardTypeDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 작업 유형 조회 API */
@RestController
@RequiredArgsConstructor
public class CardTypeController {

    private final CardTypeService cardTypeService;

    @GetMapping("/card-types")
    public ResponseEntity<List<CardTypeResponse>> list() {
        return ResponseEntity.ok(cardTypeService.list());
    }
}
