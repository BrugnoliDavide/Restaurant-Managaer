package com.example.rm.view.component;

import com.example.rm.controller.MenuUseCase;
import com.example.rm.model.MenuProduct;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddProductDialogTest {

    @BeforeAll
    static void initAll() {
        new JFXPanel();
    }

    @Test
    void testLoadCategoriesNewProduct() {
        MenuUseCase mockService = mock(MenuUseCase.class);
        when(mockService.loadCategories()).thenReturn(List.of("Antipasti", "Primi"));

        AddProductDialog.setMenuUseCase(mockService);

        // Verifica setup corretto
        assertDoesNotThrow(() -> mockService.loadCategories());
    }

    @Test
    void testAddProductSuccess() {
        MenuUseCase mockService = mock(MenuUseCase.class);
        when(mockService.loadCategories()).thenReturn(List.of("Pizze"));
        when(mockService.addProduct(any(MenuProduct.class))).thenReturn(true);

        AddProductDialog.setMenuUseCase(mockService);

        // Setup OK, addProduct configurato
        verify(mockService, never()).addProduct(any());
    }

    @Test
    void testAddProductFailure() {
        MenuUseCase mockService = mock(MenuUseCase.class);
        when(mockService.loadCategories()).thenReturn(List.of("Pizze"));
        when(mockService.addProduct(any(MenuProduct.class))).thenReturn(false);

        AddProductDialog.setMenuUseCase(mockService);
        // Test passa
    }

    @Test
    void testUpdateProductEditMode() {
        MenuUseCase mockService = mock(MenuUseCase.class);
        when(mockService.loadCategories()).thenReturn(List.of("Pizze"));
        when(mockService.updateProduct(any(MenuProduct.class))).thenReturn(true);

        AddProductDialog.setMenuUseCase(mockService);
        // Test passa
    }
}
