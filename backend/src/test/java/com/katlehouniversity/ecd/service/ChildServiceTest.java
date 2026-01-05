package com.katlehouniversity.ecd.service;

import com.katlehouniversity.ecd.dto.ChildDto;
import com.katlehouniversity.ecd.entity.Child;
import com.katlehouniversity.ecd.exception.ResourceNotFoundException;
import com.katlehouniversity.ecd.repository.ChildRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Child Service Tests")
class ChildServiceTest {

    @Mock
    private ChildRepository childRepository;

    @InjectMocks
    private ChildService childService;

    private Child testChild;
    private ChildDto testChildDto;

    @BeforeEach
    void setUp() {
        testChild = Child.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .paymentReference("JOHNDOE")
                .monthlyFee(BigDecimal.valueOf(1500))
                .status(Child.StudentStatus.ACTIVE)
                .build();

        testChildDto = ChildDto.builder()
                .firstName("John")
                .lastName("Doe")
                .monthlyFee(BigDecimal.valueOf(1500))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should get all children successfully")
    void testGetAllChildren() {
        // Arrange
        List<Child> children = Arrays.asList(testChild);
        when(childRepository.findAll()).thenReturn(children);

        // Act
        List<ChildDto> result = childService.getAllChildren();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getFirstName());
        verify(childRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should get child by ID successfully")
    void testGetChildById() {
        // Arrange
        when(childRepository.findById(1L)).thenReturn(Optional.of(testChild));

        // Act
        ChildDto result = childService.getChildById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        verify(childRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when child not found")
    void testGetChildByIdNotFound() {
        // Arrange
        when(childRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            childService.getChildById(999L);
        });
        verify(childRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should create child successfully")
    void testCreateChild() {
        // Arrange
        when(childRepository.save(any(Child.class))).thenReturn(testChild);

        // Act
        ChildDto result = childService.createChild(testChildDto);

        // Assert
        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(childRepository, times(1)).save(any(Child.class));
    }

    @Test
    @DisplayName("Should get active children count")
    void testGetActiveChildrenCount() {
        // Arrange
        when(childRepository.countActiveChildren()).thenReturn(5L);

        // Act
        long count = childService.getActiveChildrenCount();

        // Assert
        assertEquals(5L, count);
        verify(childRepository, times(1)).countActiveChildren();
    }
}
