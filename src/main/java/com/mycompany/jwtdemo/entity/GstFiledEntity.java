package com.mycompany.jwtdemo.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDate;

@Getter
@Setter
@Entity(name = "gst_filed_tracker")
public class GstFiledEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String gstNo;
    private String returnType;
    private LocalDate returnPeriod;
    private String status;
    private String mode;
    private LocalDate dateOfFiling;
}
