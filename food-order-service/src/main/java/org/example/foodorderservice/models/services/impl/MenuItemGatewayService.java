package org.example.foodorderservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.foodorderservice.clients.MenuItemClient;
import org.example.foodorderservice.exceptions.MenuItemNotFoundException;
import org.example.foodorderservice.exceptions.MenuItemServiceException;
import org.example.foodorderservice.models.dto.responses.MenuItemResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuItemGatewayService {

    private final MenuItemClient menuItemClient;

    @CircuitBreaker(name = "menuService", fallbackMethod = "fallbackGetMenuItemById")
    public MenuItemResponse getMenuItemById(Long menuItemId) {
        try {
            return menuItemClient.getMenuItemById(menuItemId);
        } catch (FeignException.NotFound e) {
            throw new MenuItemNotFoundException(menuItemId);
        } catch (FeignException e) {
            throw new MenuItemServiceException("Menu service unavailable", e);
        }
    }

    public MenuItemResponse fallbackGetMenuItemById(Long menuItemId, Throwable throwable) {
        throw new MenuItemServiceException("Menu service is unavailable for menu item " + menuItemId, throwable);
    }
}
