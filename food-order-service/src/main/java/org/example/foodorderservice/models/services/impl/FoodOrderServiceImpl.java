package org.example.foodorderservice.models.services.impl;

import org.example.foodorderservice.models.constants.FoodOrderStatus;
import org.example.foodorderservice.models.dto.requests.CreateFoodOrderDetailRequest;
import org.example.foodorderservice.models.dto.requests.CreateFoodOrderRequest;
import org.example.foodorderservice.models.dto.responses.FoodOrderDetailResponse;
import org.example.foodorderservice.models.dto.responses.FoodOrderResponse;
import org.example.foodorderservice.models.dto.responses.MenuItemResponse;
import org.example.foodorderservice.models.entities.FoodOrder;
import org.example.foodorderservice.models.entities.FoodOrderDetail;
import org.example.foodorderservice.models.repositories.FoodOrderDetailRepository;
import org.example.foodorderservice.models.repositories.FoodOrderRepository;
import org.example.foodorderservice.models.services.FoodOrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FoodOrderServiceImpl implements FoodOrderService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderDetailRepository foodOrderDetailRepository;
    private final MenuItemGatewayService menuItemGatewayService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String foodOrderCreatedTopic;

    public FoodOrderServiceImpl(
            FoodOrderRepository foodOrderRepository,
            FoodOrderDetailRepository foodOrderDetailRepository,
            MenuItemGatewayService menuItemGatewayService,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${notification.kafka.food-order-created-topic:food-order-created}") String foodOrderCreatedTopic) {
        this.foodOrderRepository = foodOrderRepository;
        this.foodOrderDetailRepository = foodOrderDetailRepository;
        this.menuItemGatewayService = menuItemGatewayService;
        this.kafkaTemplate = kafkaTemplate;
        this.foodOrderCreatedTopic = foodOrderCreatedTopic;
    }

    @Override
    @Transactional
    public FoodOrderResponse createFoodOrder(CreateFoodOrderRequest request) {
        FoodOrder foodOrder = FoodOrder.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .status(FoodOrderStatus.PENDING)
                .totalAmount(0d)
                .build();

        FoodOrder savedOrder = foodOrderRepository.save(foodOrder);
        List<FoodOrderDetailResponse> detailResponses = new ArrayList<>();
        double totalAmount = 0d;

        for (CreateFoodOrderDetailRequest detailRequest : request.items()) {
            MenuItemResponse menuItem = menuItemGatewayService.getMenuItemById(detailRequest.menuItemId());
            double lineTotal = menuItem.currentPrice() * detailRequest.quantity();
            totalAmount += lineTotal;

            FoodOrderDetail detail = FoodOrderDetail.builder()
                    .foodOrder(savedOrder)
                    .menuItemId(menuItem.menuItemId())
                    .quantity(detailRequest.quantity())
                    .currentPrice(menuItem.currentPrice())
                    .build();
            FoodOrderDetail savedDetail = foodOrderDetailRepository.save(detail);

            detailResponses.add(new FoodOrderDetailResponse(
                    savedDetail.getId(),
                    menuItem.menuItemId(),
                    menuItem.name(),
                    detailRequest.quantity(),
                    menuItem.currentPrice(),
                    lineTotal
            ));
        }

        savedOrder.setTotalAmount(totalAmount);
        FoodOrder persistedOrder = foodOrderRepository.save(savedOrder);
        kafkaTemplate.send(foodOrderCreatedTopic, persistedOrder.getCustomerEmail());

        return new FoodOrderResponse(
                persistedOrder.getId(),
                persistedOrder.getCustomerName(),
                persistedOrder.getCustomerEmail(),
                persistedOrder.getTotalAmount(),
                persistedOrder.getStatus(),
                detailResponses
        );
    }
}
