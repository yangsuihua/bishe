package com.videoplatform.admin.controller;

import com.videoplatform.admin.result.Result;
import com.videoplatform.admin.service.AdminService;
import com.videoplatform.admin.vo.AdminStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminService adminService;
    
    @GetMapping("/stats")
    public Result<AdminStatsVO> getStats() {
        AdminStatsVO stats = adminService.getSystemStats();
        return Result.success(stats);
    }
}
