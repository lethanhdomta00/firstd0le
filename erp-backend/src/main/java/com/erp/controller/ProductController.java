package com.erp.controller;

import com.erp.dto.ProductBatchDeleteRequest;
import com.erp.model.Product;
import com.erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    // GET /api/products -> Danh sách sản phẩm có Phân trang, Tìm kiếm & Lọc nâng cao
    @GetMapping
    public Page<Product> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);

        return productRepository.searchProducts(
                keyword != null ? keyword.trim() : null,
                minPrice,
                maxPrice,
                stockStatus != null ? stockStatus.trim() : null,
                pageable
        );
    }

    // GET /api/products/{id} -> Chi tiết sản phẩm
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/products -> Tạo sản phẩm mới (Chỉ ADMIN)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên sản phẩm không được để trống!"));
        }
        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã SKU không được để trống!"));
        }
        return ResponseEntity.ok(productRepository.save(product));
    }

    // PUT /api/products/{id} -> Cập nhật sản phẩm (Chỉ ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(productDetails.getName());
                    product.setSku(productDetails.getSku());
                    product.setPrice(productDetails.getPrice());
                    product.setStock(productDetails.getStock());
                    product.setImageUrl(productDetails.getImageUrl());
                    Product updatedProduct = productRepository.save(product);
                    return ResponseEntity.ok(updatedProduct);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/products/{id} -> Xóa 1 sản phẩm (Chỉ ADMIN)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    productRepository.delete(product);
                    return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm thành công!"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/products/batch-delete -> Xóa hàng loạt nhiều sản phẩm (Chỉ ADMIN)
    @PostMapping("/batch-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> batchDeleteProducts(@RequestBody ProductBatchDeleteRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Danh sách ID sản phẩm cần xóa không hợp lệ!"));
        }
        productRepository.deleteAllById(request.getIds());
        return ResponseEntity.ok(Map.of(
                "message", "Đã xóa thành công " + request.getIds().size() + " sản phẩm!",
                "deletedCount", request.getIds().size()
        ));
    }
}
