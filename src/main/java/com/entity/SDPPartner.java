package com.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SDPPartner {
    private Long id;
    private String partnerCode;
    private String partnerName;
    private String token;
    private String ip;
    private int status;
    private String description;
    private int quotaReal;
    private int quotaFake;
    private int quotaPhoto;
    private int quotaJunk;

}
