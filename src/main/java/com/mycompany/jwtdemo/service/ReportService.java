package com.mycompany.jwtdemo.service;

import com.mycompany.jwtdemo.entity.GstAccountEntity;
import com.mycompany.jwtdemo.entity.GstFiledEntity;
import com.mycompany.jwtdemo.entity.NotFiledOverviewEntity;
import com.mycompany.jwtdemo.model.NotFiledDTO;
import com.mycompany.jwtdemo.repository.GstAccountRepository;
import com.mycompany.jwtdemo.repository.GstFiledRepository;
import com.mycompany.jwtdemo.repository.GstNotFiledRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ReportService {

    @Autowired
    private GstNotFiledRepository notFiledRepository;

    @Autowired
    private GstFiledRepository filedRepository;

    @Autowired
    private GstAccountRepository accountRepository;

    public List<NotFiledDTO> getReports(String month, Integer year, String retType , Long caId) {
        LocalDate startDate, endDate;
        startDate = LocalDate.of(year -1 , Month.valueOf(month.toUpperCase(Locale.ROOT)),1);
        endDate = LocalDate.of(year -1, Month.valueOf(month.toUpperCase(Locale.ROOT)),1)
                .with(TemporalAdjusters.lastDayOfMonth());
        List<GstAccountEntity> accounts =  getGstAccounts(caId);
        List<NotFiledDTO> notFiledList = new ArrayList<>();
        accounts.forEach(acct -> {
            NotFiledDTO notFiledDTO = getReportDTOByReturnTypeAndGstNo(acct.getGstNo(), startDate, endDate, retType);
            notFiledDTO.setGstNo(acct.getGstNo());
            notFiledDTO.setFirmName(acct.getFirmName());
            notFiledList.add(notFiledDTO);
        });
        return notFiledList;
    }

    private NotFiledDTO getReportDTOByReturnTypeAndGstNo(String gstNo, LocalDate startDate, LocalDate endDate, String retType) {
        List<NotFiledOverviewEntity> entityList;
        List<GstFiledEntity> filedEntityList;
        NotFiledDTO notFiledDTO = new NotFiledDTO();
        if(retType.equalsIgnoreCase("BOTH")) {
            //GSTR1 get reports
            filedEntityList = filedRepository.findByGstNoAndReturnTypeAndReturnPeriodBetween(gstNo,
                    "GSTR1", startDate, endDate);
            if(!filedEntityList.isEmpty() && filedEntityList.get(0).getStatus().equalsIgnoreCase("Filed")){
                notFiledDTO.setGstr1Status("Filed");
            }else{
                entityList = notFiledRepository.findByGstNoAndReturnTypeAndDateOfGstFilingBetween(gstNo,
                        "GSTR1", startDate, endDate);
                if(!entityList.isEmpty() && !entityList.get(0).getIsGstFiled()) {
                    notFiledDTO.setGstr1Status("Not Filed");
                }else{
                    notFiledDTO.setGstr1Status("Data Not Available");
                }
            }
            //GSTR3B get reports
            filedEntityList = filedRepository.findByGstNoAndReturnTypeAndReturnPeriodBetween(gstNo,
                    "GSTR3B", startDate, endDate);
            if(!filedEntityList.isEmpty() && filedEntityList.get(0).getStatus().equalsIgnoreCase("Filed")){
                notFiledDTO.setGstr3bStatus("Filed");
            }else{
                entityList = notFiledRepository.findByGstNoAndReturnTypeAndDateOfGstFilingBetween(gstNo,
                        "GSTR3B", startDate, endDate);
                if(!entityList.isEmpty() && !entityList.get(0).getIsGstFiled()) {
                    notFiledDTO.setGstr3bStatus("Not Filed");
                }else{
                    notFiledDTO.setGstr3bStatus("Data Not Available");
                }
            }
        }else if(retType.equalsIgnoreCase("GSTR1")){
            filedEntityList = filedRepository.findByGstNoAndReturnTypeAndReturnPeriodBetween(gstNo,
                    "GSTR1", startDate, endDate);
            if(!filedEntityList.isEmpty() && filedEntityList.get(0).getStatus().equalsIgnoreCase("Filed")){
                notFiledDTO.setGstr1Status("Filed");
            }else{
                entityList = notFiledRepository.findByGstNoAndReturnTypeAndDateOfGstFilingBetween(gstNo,
                        "GSTR1", startDate, endDate);
                if(!entityList.isEmpty() && !entityList.get(0).getIsGstFiled()) {
                    notFiledDTO.setGstr1Status("Not Filed");
                }else{
                    notFiledDTO.setGstr1Status("Data Not Available");
                }
            }
        }else if(retType.equalsIgnoreCase("GSTR3B")){
            filedEntityList = filedRepository.findByGstNoAndReturnTypeAndReturnPeriodBetween(gstNo,
                    "GSTR3B", startDate, endDate);
            if(!filedEntityList.isEmpty() && filedEntityList.get(0).getStatus().equalsIgnoreCase("Filed")){
                notFiledDTO.setGstr3bStatus("Filed");
            }else{
                entityList = notFiledRepository.findByGstNoAndReturnTypeAndDateOfGstFilingBetween(gstNo,
                        "GSTR3B", startDate, endDate);
                if(!entityList.isEmpty() && !entityList.get(0).getIsGstFiled()) {
                    notFiledDTO.setGstr3bStatus("Not Filed");
                }else{
                    notFiledDTO.setGstr3bStatus("Data Not Available");
                }
            }

        }
        return notFiledDTO;
    }


    public List<GstAccountEntity> getGstAccounts(Long caId){
        Pageable pageable = PageRequest.of(0, 100000);
        Page<GstAccountEntity> pagedAccounts = accountRepository.findByCaIdAndActiveContainsOrderByFirmName(caId, "Y", pageable);

        return pagedAccounts.getContent();
    }
}
