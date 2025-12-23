package org.example.cinemaseat.controller;

import org.example.cinemaseat.common.Result;
import org.example.cinemaseat.pojo.entity.Cinema;
import org.example.cinemaseat.service.CinemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/cinemas")
    public class CinemaController {
        @Autowired
        private CinemaService cinemaService;
        @GetMapping
        public Result<List<Cinema>> listCinemas() {
            List<Cinema> cinemas = cinemaService.listAll();
            return Result.success(cinemas);
        }
    }

