package com.mycompany.jwtdemo.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;

@Getter
@Setter
@Entity(name = "gst_not_filed_tracker")
@Table(indexes = {
        @Index(name = "index_date", columnList = "dateOfGstFiling", unique = true),
        @Index(name = "index_gstno_date", columnList = "gstNo, dateOfGstFiling", unique = true),
})
public class NotFiledOverviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String gstNo;
    private String returnType;
    private Boolean isGstFiled;
    private LocalDate dateOfGstFiling;
}
