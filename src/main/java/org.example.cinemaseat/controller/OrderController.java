package org.example.cinemaseat.controller;

import org.example.cinemaseat.common.Result;
import org.example.cinemaseat.pojo.DTO.OrderCreateDTO;
import org.example.cinemaseat.pojo.VO.OrderCreateVO;
import org.example.cinemaseat.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @PostMapping
    public Result<OrderCreateVO> createOrder(@RequestBody OrderCreateDTO dto) {
        OrderCreateVO vo = orderService.createOrder(dto);
        return Result.success(vo);
    }
}
