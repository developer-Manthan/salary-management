package com.manthan.salary_management.controller;

import com.manthan.salary_management.seed.DataSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seed")
@RequiredArgsConstructor
public class SeedController {

    private final DataSeeder dataSeeder;

    @PostMapping
    public ResponseEntity<DataSeeder.SeedResult> seedData() {
        DataSeeder.SeedResult result = dataSeeder.seed();
        return ResponseEntity.ok(result);
    }
}
