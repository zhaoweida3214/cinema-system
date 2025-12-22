package org.example.cinemaseat.service;

import org.example.cinemaseat.pojo.DTO.OrderCreateDTO;
import org.example.cinemaseat.pojo.VO.OrderCreateVO;

public interface OrderService {
    OrderCreateVO createOrder(OrderCreateDTO dto);
}
