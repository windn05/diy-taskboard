package com.taskboard.controller;

import com.taskboard.dto.CardTypeDtos.*;
import com.taskboard.service.CardTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 작업 유형 조회. 변경은 AdminCardTypeController */
@RestController
@RequiredArgsConstructor
public class CardTypeController {

    private final CardTypeService cardTypeService;

    @GetMapping("/card-types")
    public List<CardTypeResponse> list() {
        return cardTypeService.list();
    }
}
