package com.mycompany.jwtdemo.service;

import com.mycompany.jwtdemo.entity.GstAccountEntity;
import com.mycompany.jwtdemo.entity.GstFiledEntity;
import com.mycompany.jwtdemo.entity.NotFiledOverviewEntity;
import com.mycompany.jwtdemo.model.FilingOverviewDTO;
import com.mycompany.jwtdemo.model.MonthYearModel;
import com.mycompany.jwtdemo.repository.GstAccountRepository;
import com.mycompany.jwtdemo.repository.GstFiledRepository;
import com.mycompany.jwtdemo.repository.GstNotFiledRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FilingOverviewService {

    @Autowired
    private GstAccountRepository accountRepository;

    @Autowired
    private GstFiledRepository gstFiledRepository;

    @Autowired
    private GstNotFiledRepository notFiledRepository;

    List<String> prevYearmonths = Arrays.asList("APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER");
    List<String> nextYearmonths = Arrays.asList("JANUARY", "FEBRUARY", "MARCH");

    public List<FilingOverviewDTO> getFilingOverview(Long caId, String fy){
        List<FilingOverviewDTO> overviewDTOS = new ArrayList<>();
        Map<String,LocalDate> fiscalYear = calculateFiscalYear(fy);
        // Get the firm name and gst no
        List<GstAccountEntity> gstAccounts = getGstAccounts(caId);
        gstAccounts.stream().forEach(account -> {
            FilingOverviewDTO overviewDTO = new FilingOverviewDTO();
            overviewDTO.setAccountId(account.getId());
            overviewDTO.setFirmName(account.getFirmName());
            overviewDTO.setGstNo(account.getGstNo());
            overviewDTOS.add(overviewDTO);
        });

        overviewDTOS.forEach(firm -> {
            List<GstFiledEntity> filedEntities = gstFiledRepository.
                    findAllByGstNoAndReturnPeriodBetween(firm.getGstNo(), fiscalYear.get("startDate"), fiscalYear.get("endDate"));
            firm.setReturnPeriodGstr3b(getMostRecentReturnDateGstrByReturnType(filedEntities, "GSTR3B"));
            firm.setReturnPeriodGstr1(getMostRecentReturnDateGstrByReturnType(filedEntities, "GSTR1"));
            firm.setGstr1NotFiledPeriod(getNotFiledGstr1Months(filedEntities, fiscalYear));
        });
        return overviewDTOS;
    }
    public List<FilingOverviewDTO> getFilingOverviewWithGST1Filter(Long caId, String fy, String nfGst1){

        List<FilingOverviewDTO> gstr1FiledList = new ArrayList<>();
        List<FilingOverviewDTO> gstr1NotFiledList = new ArrayList<>();

        List<FilingOverviewDTO> overviewDTOList = getFilingOverview(caId, fy);
        for(FilingOverviewDTO fo: overviewDTOList){
            //This account has GSTR1 filed for all months
            if(fo.getGstr1NotFiledPeriod().isEmpty()){
                gstr1FiledList.add(fo);
            }else{
                gstr1NotFiledList.add(fo);
            }
        }
        if(nfGst1.equalsIgnoreCase("NO")){
            return gstr1FiledList;
        }else if(nfGst1.equalsIgnoreCase("YES")){
            return gstr1NotFiledList;
        }
        return  null;
    }

    public List<GstAccountEntity> getGstAccounts(Long caId){
        Pageable pageable = PageRequest.of(0, 100000);
        Page<GstAccountEntity> pagedAccounts = accountRepository.findByCaIdAndActiveContainsOrderByFirmName(caId, "Y", pageable);

        return pagedAccounts.getContent();
    }

    private LocalDate getMostRecentReturnDateGstrByReturnType(List<GstFiledEntity> filedEntities, String returnType){
        List<GstFiledEntity> gstrList = filedEntities.stream().filter(e -> e.getReturnType().equalsIgnoreCase(returnType))
                .collect(Collectors.toList());
        LocalDate maxDate = null;
        List<LocalDate> dates = gstrList.stream().map(GstFiledEntity::getReturnPeriod).collect(Collectors.toList());
        if(!dates.isEmpty())
          maxDate = Collections.max(dates);
        return maxDate;
    }

    private Map<String,LocalDate> calculateFiscalYear(String fy){
        String[] yr = fy.split("-");
        Map<String,LocalDate> fiscalYear = new HashMap<>();
        Integer year = Integer.parseInt(yr[0]);
        fiscalYear.put("startDate", LocalDate.of(year, Month.APRIL,1));
        fiscalYear.put("endDate", LocalDate.of(year + 1, Month.MARCH,31));
        return fiscalYear;
    }

    private List<MonthYearModel> getNotFiledGstr1Months(List<GstFiledEntity> filedEntities , Map<String, LocalDate> fy){
        List<MonthYearModel> notFiledDetails = new ArrayList<>();
        LocalDate start = fy.get("startDate");
        while(start.isBefore(fy.get("endDate"))){
            notFiledDetails.add(new MonthYearModel(start.getMonth().name(), String.valueOf(start.getYear())));
            start = start.plusMonths(1);
        }
        filedEntities.forEach(e -> {
            notFiledDetails.removeIf(nfe -> nfe.getMonth().equalsIgnoreCase(e.getReturnPeriod().getMonth().name()) &&
                    nfe.getYear().equalsIgnoreCase(String.valueOf(e.getReturnPeriod().getYear())));
        });
        return notFiledDetails;
    }
    @Transactional
    public void updateNotFiledOverview(List<GstAccountEntity> accounts, String type) {
        for(GstAccountEntity ge: accounts){
            notFiledRepository.deleteAllByGstNo(ge.getGstNo());
        }
        accounts.forEach(acc -> {
            List<GstFiledEntity> filedEntities = gstFiledRepository.findAllByGstNoOrderByGstNo(acc.getGstNo());
            if(type.equalsIgnoreCase("BOTH")) {
                List<GstFiledEntity> gstr1List = filedEntities.stream()
                        .filter(e -> e.getReturnType().equalsIgnoreCase("GSTR1"))
                        .collect(Collectors.toList());
                updateEntityByDates(gstr1List, acc.getGstNo(), "GSTR1");
                List<GstFiledEntity> gstr3bList = filedEntities.stream()
                        .filter(e -> e.getReturnType().equalsIgnoreCase("GSTR3B"))
                        .collect(Collectors.toList());
                updateEntityByDates(gstr3bList, acc.getGstNo(), "GSTR3B");
            }else {
                List<GstFiledEntity> gstrTypeList = filedEntities.stream()
                        .filter(e -> e.getReturnType().equalsIgnoreCase(type))
                        .collect(Collectors.toList());
                updateEntityByDates(gstrTypeList, acc.getGstNo(), type);
            }
        });
    }

    public void updateEntityByDates(List<GstFiledEntity> gstList, String gstNo, String returnType){
        List<LocalDate> dates = gstList.stream().map(GstFiledEntity::getReturnPeriod).collect(Collectors.toList());
        List<NotFiledOverviewEntity> nfowList = new ArrayList<>();
        LocalDate minDate,maxDate;
        if(!dates.isEmpty()) {
            minDate = Collections.min(dates);
            maxDate = Collections.max(dates);
            if(prevYearmonths.contains(minDate.getMonth().name())){
                minDate = LocalDate.of(minDate.getYear(), Month.APRIL, 1);
            }
            if(nextYearmonths.contains(maxDate.getMonth().name())){
                maxDate = LocalDate.of(maxDate.getYear(), Month.MARCH, 31);
            }

            while(minDate.isBefore(maxDate)){
                NotFiledOverviewEntity nfow = new NotFiledOverviewEntity();
                nfow.setGstNo(gstNo);
                nfow.setReturnType(returnType);
                nfow.setIsGstFiled(Boolean.FALSE);
                nfow.setDateOfGstFiling(LocalDate.of(minDate.getYear(), minDate.getMonth(), 1));
                minDate = minDate.plusMonths(1);
                nfowList.add(nfow);
            }
        }

        if(!nfowList.isEmpty() && !gstList.isEmpty()) {
            gstList.forEach(f -> nfowList.forEach(nfow -> {
                if (f.getReturnPeriod().getMonth().equals(nfow.getDateOfGstFiling().getMonth()) &&
                        f.getReturnPeriod().getYear() == nfow.getDateOfGstFiling().getYear()) {
                    nfow.setDateOfGstFiling(f.getReturnPeriod());
                    nfow.setIsGstFiled(Boolean.TRUE);
                }
            }));
            notFiledRepository.saveAll(nfowList);
        }
    }
}
