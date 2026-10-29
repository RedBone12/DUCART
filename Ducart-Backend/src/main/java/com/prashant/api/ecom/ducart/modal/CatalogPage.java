package com.prashant.api.ecom.ducart.modal;

import java.util.List;

public record CatalogPage(List<ProductResponseDTO> content, int page, int size,
        long totalElements, int totalPages) { }
