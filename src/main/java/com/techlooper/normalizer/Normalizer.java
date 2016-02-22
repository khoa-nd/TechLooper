package com.techlooper.normalizer;

/**
 * Created by NguyenDangKhoa on 2/22/16.
 */
public interface Normalizer<IN, OUT> {

    OUT normalize(IN arg);

}
