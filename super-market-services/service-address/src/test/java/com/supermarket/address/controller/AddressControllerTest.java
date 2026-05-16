package com.supermarket.address.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.address.entity.Address;
import com.supermarket.address.service.AddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AddressController.class)
@ActiveProfiles("test")
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddressService addressService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateWhenValidBody() throws Exception {
        // Arrange
        Address address = new Address();
        address.setUserId(1L);
        address.setReceiverName("张三");
        address.setReceiverPhone("13800138000");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDetail("科技园南区");

        Address savedAddress = new Address();
        savedAddress.setId(100L);
        savedAddress.setUserId(1L);
        savedAddress.setReceiverName("张三");
        savedAddress.setReceiverPhone("13800138000");
        savedAddress.setProvince("广东省");
        savedAddress.setCity("深圳市");
        savedAddress.setDetail("科技园南区");

        when(addressService.create(any(Address.class))).thenReturn(savedAddress);

        // Act & Assert
        mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.receiverName").value("张三"));
        verify(addressService).create(any(Address.class));
    }

    @Test
    void shouldUpdateWhenValidBody() throws Exception {
        // Arrange
        Address address = new Address();
        address.setId(100L);
        address.setReceiverName("李四");
        address.setReceiverPhone("13900139000");

        Address updatedAddress = new Address();
        updatedAddress.setId(100L);
        updatedAddress.setReceiverName("李四");
        updatedAddress.setReceiverPhone("13900139000");

        when(addressService.update(any(Address.class))).thenReturn(updatedAddress);

        // Act & Assert
        mockMvc.perform(put("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(address)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.receiverName").value("李四"));
        verify(addressService).update(any(Address.class));
    }

    @Test
    void shouldDeleteWhenValidIdAndUserId() throws Exception {
        // Arrange
        doNothing().when(addressService).delete(100L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/address/100")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
        verify(addressService).delete(100L, 1L);
    }

    @Test
    void shouldListWhenValidUserId() throws Exception {
        // Arrange
        Address address1 = new Address();
        address1.setId(1L);
        address1.setReceiverName("张三");

        Address address2 = new Address();
        address2.setId(2L);
        address2.setReceiverName("李四");

        when(addressService.listByUser(1L)).thenReturn(List.of(address1, address2));

        // Act & Assert
        mockMvc.perform(get("/api/address/list")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].receiverName").value("张三"))
                .andExpect(jsonPath("$.data[1].receiverName").value("李四"));
        verify(addressService).listByUser(1L);
    }

    @Test
    void shouldSetDefaultWhenValidIdAndUserId() throws Exception {
        // Arrange
        doNothing().when(addressService).setDefault(100L, 1L);

        // Act & Assert
        mockMvc.perform(put("/api/address/100/default")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
        verify(addressService).setDefault(100L, 1L);
    }
}
