package com.prashant.api.ecom.ducart.modal;

public class AuthResponseDTO {
    private Long userid;
    private String name;
    private String username;
    private String email;
    private String role;
    private String token;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(Long userid, String name, String username, String email, String role, String token) {
        this.userid = userid;
        this.name = name;
        this.username = username;
        this.email = email;
        this.role = role;
        this.token = token;
    }

    public Long getUserid() {
        return userid;
    }

    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}