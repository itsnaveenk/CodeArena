package com.codearena.exception;

public class ContestNotFoundException extends ResourceNotFoundException {

    public ContestNotFoundException(Long id) {
        super("Contest", id);
    }

    public ContestNotFoundException(String slug) {
        super("Contest", slug);
    }
}
