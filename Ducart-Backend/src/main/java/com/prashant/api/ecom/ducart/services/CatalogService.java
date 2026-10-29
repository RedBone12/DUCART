package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Product;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.modal.CatalogPage;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CatalogService {
    private final ProductRepo products;
    private final ProductService mapper;

    public CatalogService(ProductRepo products, ProductService mapper) {
        this.products = products;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CatalogPage search(Map<String, String> query) {
        int page = integer(query, "page", 0, 0, 100000);
        int size = integer(query, "size", 12, 1, 100);
        String keyword = text(query, "q");
        String mc = text(query, "mc"), sc = text(query, "sc"), brand = text(query, "br");
        BigDecimal min = price(query, "min"), max = price(query, "max");
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BadRequestException("Minimum price must not exceed maximum price");
        }
        Sort sort = switch (query.getOrDefault("sort", "latest")) {
            case "latest" -> Sort.by(Sort.Direction.DESC, "id");
            case "priceAsc" -> Sort.by("finalPrice").ascending().and(Sort.by("id"));
            case "priceDesc" -> Sort.by("finalPrice").descending().and(Sort.by("id"));
            default -> throw new BadRequestException("Unsupported sort order");
        };
        Specification<Product> spec = (product, criteria, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.isTrue(product.get("active")));
            for (String[] filter : new String[][] {{"maincategory", mc}, {"subcategory", sc}, {"brand", brand}}) {
                if (!filter[1].isEmpty() && !filter[1].equals("All")) {
                    filters.add(cb.equal(product.get(filter[0]), filter[1]));
                }
            }
            if (!keyword.isEmpty()) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT).replace("!", "!!")
                        .replace("%", "!%").replace("_", "!_") + "%";
                List<Predicate> words = new ArrayList<>();
                for (String field : List.of("name", "maincategory", "subcategory", "brand", "color", "description")) {
                    words.add(cb.like(cb.lower(product.get(field)), pattern, '!'));
                }
                filters.add(cb.or(words.toArray(Predicate[]::new)));
            }
            if (min != null) filters.add(cb.ge(product.get("finalPrice"), min));
            if (max != null) filters.add(cb.le(product.get("finalPrice"), max));
            return cb.and(filters.toArray(Predicate[]::new));
        };
        var result = products.findAll(spec, PageRequest.of(page, size, sort));
        return new CatalogPage(result.getContent().stream().map(mapper::toResponseDTO).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    private String text(Map<String, String> query, String key) {
        String value = query.getOrDefault(key, "").trim();
        if (value.length() > 200) throw new BadRequestException(key + " must be at most 200 characters");
        return value;
    }

    private int integer(Map<String, String> query, String key, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(query.getOrDefault(key, String.valueOf(fallback)));
            if (value >= min && value <= max) return value;
        } catch (NumberFormatException ignored) { }
        throw new BadRequestException("Invalid " + key);
    }

    private BigDecimal price(Map<String, String> query, String key) {
        String value = text(query, key);
        if (value.isEmpty()) return null;
        if (!value.matches("[0-9]{1,12}(\\.[0-9]{1,2})?")) {
            throw new BadRequestException("Invalid " + key + " price");
        }
        return new BigDecimal(value);
    }
}
