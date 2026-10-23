package com.sem4.openapispringboot.exception;

public class ProductNotFoundException extends RuntimeException {
    private final Long id;

    public ProductNotFoundException(Long id) {
        super("Không tìm thấy sản phẩm id=" + id);
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
