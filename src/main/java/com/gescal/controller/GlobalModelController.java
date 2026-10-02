package com.gescal.controller;

import com.gescal.service.WorkingYearService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelController {
    private final WorkingYearService workingYear;

    public GlobalModelController(WorkingYearService workingYear) {
        this.workingYear = workingYear;
    }

    @ModelAttribute("workingYear")
    public int workingYear() {
        return workingYear.get();
    }
}
