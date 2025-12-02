package com.example.springboot.dto.waitlist;

import com.example.springboot.common.BaseResponse;
import lombok.Data;
import java.util.List;

@Data
public class WaitlistPageResponse extends BaseResponse<List<WaitlistResponse>> {
    private long total;
    private int page;
    private int size;

    public WaitlistPageResponse(int code, String message, List<WaitlistResponse> data,
                                long total, int page, int size) {
        super(code, message, data);
        this.total = total;
        this.page = page;
        this.size = size;
    }
}