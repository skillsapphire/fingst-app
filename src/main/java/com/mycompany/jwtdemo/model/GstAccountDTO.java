package com.mycompany.jwtdemo.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Getter
@Setter
public class GstAccountDTO {

    private Long id;
    private String gstNo;
    private String contactPerson;
    private String proprietorName;
    private String email;
    private String phone;
    private String gstPortalUsername;
    private String gstPortalPassword;
    private String firmName;
    private Long caId;
    private LocalDate creationDate;
    private ZonedDateTime lastRefreshed;
    private String lastRefreshedFormatted;

    private ZonedDateTime lastRefreshedCurrFy;
    private String lastRefreshedCurrFyFormatted;
    private ZonedDateTime lastRefreshedPrevFy;
    private String lastRefreshedPrevFyFormatted;
}
