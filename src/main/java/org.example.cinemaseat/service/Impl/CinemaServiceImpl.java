package org.example.cinemaseat.service.Impl;

import org.example.cinemaseat.pojo.entity.Cinema;
import org.example.cinemaseat.service.CinemaService;
import org.example.cinemaseat.mapper.CinemaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CinemaServiceImpl implements CinemaService {
    @Autowired
    private CinemaMapper cinemaMapper;
    @Override
    public List<Cinema> listAll() {
        return cinemaMapper.selectAll();
    }
}
