package com.stationery.request.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class CreateRequestDto {

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<RequestItemDto> items;

    public CreateRequestDto() {}

    public CreateRequestDto(List<RequestItemDto> items) {
        this.items = items;
    }

    public List<RequestItemDto> getItems() { return items; }
    public void setItems(List<RequestItemDto> items) { this.items = items; }

    public static CreateRequestDtoBuilder builder() { return new CreateRequestDtoBuilder(); }

    public static class CreateRequestDtoBuilder {
        private List<RequestItemDto> items;

        public CreateRequestDtoBuilder items(List<RequestItemDto> items) { this.items = items; return this; }

        public CreateRequestDto build() {
            return new CreateRequestDto(items);
        }
    }
}
