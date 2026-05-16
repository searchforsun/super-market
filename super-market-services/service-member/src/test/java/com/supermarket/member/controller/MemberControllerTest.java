package com.supermarket.member.controller;

import com.supermarket.member.entity.Member;
import com.supermarket.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@ActiveProfiles("test")
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @Test
    void shouldGetMemberWhenValidUserId() throws Exception {
        // Arrange
        Member mockMember = new Member();
        mockMember.setUserId(1L);
        mockMember.setLevel(1);
        mockMember.setPoints(100);
        when(memberService.getOrCreate(1L)).thenReturn(mockMember);

        // Act & Assert
        mockMvc.perform(get("/api/member/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.level").value(1))
                .andExpect(jsonPath("$.data.points").value(100));
        verify(memberService).getOrCreate(1L);
    }

    @Test
    void shouldAddPointsWhenValidInput() throws Exception {
        // Arrange
        doNothing().when(memberService).addPoints(1L, 50);

        // Act & Assert
        mockMvc.perform(post("/api/member/points/add")
                        .param("userId", "1")
                        .param("points", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
        verify(memberService).addPoints(1L, 50);
    }

    @Test
    void shouldDeductPointsWhenValidInput() throws Exception {
        // Arrange
        when(memberService.deductPoints(1L, 30)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/member/points/deduct")
                        .param("userId", "1")
                        .param("points", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
        verify(memberService).deductPoints(1L, 30);
    }

    @Test
    void shouldReturnErrorWhenDeductPointsWithInvalidPoints() throws Exception {
        // Arrange: points must be @Min(1), so 0 should fail
        // Act & Assert
        mockMvc.perform(post("/api/member/points/deduct")
                        .param("userId", "1")
                        .param("points", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verify(memberService, never()).deductPoints(anyLong(), anyInt());
    }

    @Test
    void shouldReturnErrorWhenDeductPointsWithNullUserId() throws Exception {
        // Act & Assert
        // Missing required @RequestParam userId triggers MissingServletRequestParameterException
        // which is not a ConstraintViolationException, falls through to generic handler -> 500
        mockMvc.perform(post("/api/member/points/deduct")
                        .param("points", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500));
        verify(memberService, never()).deductPoints(anyLong(), anyInt());
    }
}
