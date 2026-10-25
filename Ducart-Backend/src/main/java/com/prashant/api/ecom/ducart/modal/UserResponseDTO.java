package com.prashant.api.ecom.ducart.modal;

import com.prashant.api.ecom.ducart.entities.User;

public record UserResponseDTO(Long userid, String name, String username, String email,
        String phone, String role, String address, String city, String state, String pin, String pic) {
    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(user.getUserid(), user.getName(), user.getUsername(), user.getEmail(),
                user.getPhone(), user.getRole(), user.getAddress(), user.getCity(), user.getState(),
                user.getPin(), user.getPic());
    }
}
