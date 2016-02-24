package com.techlooper.util;

import java.util.function.ToDoubleBiFunction;

/**
 * Created by NguyenDangKhoa on 2/23/16.
 */
public class SalaryAverageFunction implements ToDoubleBiFunction<Long, Long> {

    private static final long VIETNAM_CURRENCY_SALARY_DETECTOR = 1000000L;

    private static final long VND_USD_RATE = 21000L;

    @Override
    public double applyAsDouble(Long salaryMin, Long salaryMax) {
        Long salaryMinUSD = convertVNDToUSD(salaryMin);
        Long salaryMaxUSD = convertVNDToUSD(salaryMax);

        if (salaryMinUSD == 0L) {
            return salaryMaxUSD * 0.75D;
        } else if (salaryMaxUSD == 0L) {
            return salaryMinUSD * 1.25D;
        }
        return (salaryMinUSD + salaryMaxUSD) / 2;
    }

    private Long convertVNDToUSD(Long salary) {
        if (salary == null) {
            return 0L;
        }
        return salary > VIETNAM_CURRENCY_SALARY_DETECTOR ? (salary / VND_USD_RATE) : salary;
    }

}
