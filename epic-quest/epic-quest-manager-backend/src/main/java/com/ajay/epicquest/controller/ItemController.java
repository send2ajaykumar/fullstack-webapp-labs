package com.ajay.epicquest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ajay.epicquest.dto.ItemDto;
import com.ajay.epicquest.model.Item;
import com.ajay.epicquest.model.enums.Rarity;
import com.ajay.epicquest.service.interfaces.ItemService;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Tag(name = "Items", description = "Item management endpoints")
/**
 * Serves the item catalog API, separating public read operations from admin-only
 * mutation endpoints required by the assignment.
 */
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @Operation(summary = "Create a new item", description = "Creates a new item with the provided details. Admin role required.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Item created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or item already exists in category"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Item> createItem(@Valid @RequestBody ItemDto dto) {
        return ResponseEntity.status(201).body(itemService.createItem(dto));
    }

    @GetMapping
    @Operation(summary = "Get all items", description = "Retrieves all items with optional filtering, sorting, and pagination. Public access.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Items retrieved successfully")
    })
    public ResponseEntity<List<Item>> getAllItems(
            @Parameter(description = "Filter by item category (e.g., weapon, potion)") @RequestParam(required = false) String category,
            @Parameter(description = "Filter by item rarity (COMMON, RARE, EPIC, LEGENDARY)") @RequestParam(required = false) Rarity rarity,
            @Parameter(description = "Page number for pagination (0-based)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Number of items per page") @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(itemService.getItems(category, rarity, page, limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID", description = "Retrieves a specific item by its ID. Public access.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public ResponseEntity<Item> getItem(@Parameter(description = "Item ID") @PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItem(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an item", description = "Updates an existing item by ID. Admin role required.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or name conflict in category"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Item> updateItem(@Parameter(description = "Item ID") @PathVariable Long id, @Valid @RequestBody ItemDto dto) {
        return ResponseEntity.ok(itemService.updateItem(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an item", description = "Deletes an item by ID. Admin role required.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Item deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT token required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteItem(@Parameter(description = "Item ID") @PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.status(204).build();
    }
}
