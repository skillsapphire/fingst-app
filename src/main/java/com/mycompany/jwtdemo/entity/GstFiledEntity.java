package com.mycompany.jwtdemo.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

@Getter
@Setter
@Entity(name = "gst_filed_tracker")
@Table(indexes = {
        @Index(name = "index_return_date", columnList = "returnPeriod", unique = true),
        @Index(name = "index_gstno_return_date", columnList = "gstNo, returnPeriod", unique = true),
})
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
