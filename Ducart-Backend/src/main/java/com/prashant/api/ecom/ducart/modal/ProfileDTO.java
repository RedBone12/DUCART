package com.prashant.api.ecom.ducart.modal;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {
    private String name;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String pin;
    private String pic;
}
