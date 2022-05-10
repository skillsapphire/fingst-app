package com.mycompany.jwtdemo.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class GstCommonUtil {

    //2021-03-01
    public LocalDate convertReturnPeriodDate(String returnPeriod){//032021
        return LocalDate
                .of(Integer.parseInt(returnPeriod.substring(2)),
                        Integer.parseInt(returnPeriod.substring(0,2)),
                        1 );
    }

    //2020-21
    public String getFinancialYearValue(String fy){//2021
        Integer currYr = Integer.parseInt(fy);
        Integer prevYr = currYr - 1;
        String fyFinal = prevYr+"-"+currYr.toString().substring(2);
        return fyFinal;
    }

    //[042021, 052021, 062021, 072021, 082021, 092021, 102021, 112021, 122021, 012022, 022022, 032022]
    public List<String> listOfStaticFy(String fy){
        List<String> returnPeriodList = new ArrayList<>();
        //String fy = "2022";
        Integer fyInt = Integer.parseInt(fy);
        StringBuilder sb = null;
        //if fy==2022 then returnPeriodList - [042021,052021,062021....012022,022022,032022]
        for(Integer i=3; i<=9; i++){
            sb = new StringBuilder();
            sb.append("0");
            sb.append(i);
            sb.append(fyInt-1);
            returnPeriodList.add(sb.toString());
        }
        for(Integer i=10; i<=12; i++){
            sb = new StringBuilder();
            sb.append(i);
            sb.append(fyInt-1);
            returnPeriodList.add(sb.toString());
        }
        for(Integer i=1; i<=3; i++){
            sb = new StringBuilder();
            sb.append("0");
            sb.append(i);
            sb.append(fyInt);
            returnPeriodList.add(sb.toString());
        }
        //System.out.println(returnPeriodList);
        return returnPeriodList;
    }
}
