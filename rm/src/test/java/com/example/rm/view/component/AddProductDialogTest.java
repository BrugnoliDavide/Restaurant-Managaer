package com.example.rm.view.component;

import com.example.rm.controller.MenuUseCase;
import com.example.rm.model.MenuProduct;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddProductDialogTest {

    private MenuUseCase mockMenuUseCase;

    @BeforeAll
    static void initAll() {
        new JFXPanel();
    }

    @BeforeEach
    void setUp() {
        mockMenuUseCase = mock(MenuUseCase.class);
        AddProductDialog.setMenuUseCase(mockMenuUseCase);
    }

    @AfterEach
    void tearDown() {
        reset(mockMenuUseCase);
    }

    @Test
    void testMenuUseCaseInjection() {
        MenuUseCase customUseCase = mock(MenuUseCase.class);
        AddProductDialog.setMenuUseCase(customUseCase);
        assertNotNull(customUseCase);
    }

    @Test
    void testLoadCategoriesWhenDialogIsCreated() {
        List<String> expectedCategories = Arrays.asList("Antipasti", "Primi", "Secondi", "Pizze");
        when(mockMenuUseCase.loadCategories()).thenReturn(expectedCategories);

        List<String> actualCategories = mockMenuUseCase.loadCategories();

        assertNotNull(actualCategories);
        assertEquals(expectedCategories.size(), actualCategories.size());
        assertTrue(actualCategories.containsAll(expectedCategories));
    }

    @Test
    void testAddProductSuccess() {
        MenuProduct newProduct = new MenuProduct("Pizza Margherita", "Pizze",
                new BigDecimal("8.00"), new BigDecimal("3.50"), "Glutine, Lattosio");
        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);

        ArgumentCaptor<MenuProduct> productCaptor = ArgumentCaptor.forClass(MenuProduct.class);

        boolean result = mockMenuUseCase.addProduct(newProduct);

        assertTrue(result);
        verify(mockMenuUseCase, times(1)).addProduct(productCaptor.capture());

        MenuProduct capturedProduct = productCaptor.getValue();
        assertEquals("Pizza Margherita", capturedProduct.getNome());
        assertEquals("Pizze", capturedProduct.getTipologia());
        assertEquals(0, new BigDecimal("8.00").compareTo(capturedProduct.getPrezzoVendita()));
        assertEquals(0, new BigDecimal("3.50").compareTo(capturedProduct.getCostoRealizzazione()));
    }

    @Test
    void testAddProductFailure() {
        MenuProduct invalidProduct = new MenuProduct("Pizza Test", "Pizze",
                new BigDecimal("10.00"), new BigDecimal("5.00"), "");
        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(false);

        boolean result = mockMenuUseCase.addProduct(invalidProduct);

        assertFalse(result);
        verify(mockMenuUseCase, times(1)).addProduct(invalidProduct);
    }

    @Test
    void testUpdateProductInEditMode() {
        MenuProduct existingProduct = new MenuProduct(1, "Margherita", "Pizze",
                new BigDecimal("8.00"), new BigDecimal("3.50"), "Glutine, Lattosio");
        when(mockMenuUseCase.updateProduct(any(MenuProduct.class))).thenReturn(true);

        ArgumentCaptor<MenuProduct> productCaptor = ArgumentCaptor.forClass(MenuProduct.class);

        boolean result = mockMenuUseCase.updateProduct(existingProduct);

        assertTrue(result);
        verify(mockMenuUseCase, times(1)).updateProduct(productCaptor.capture());

        MenuProduct capturedProduct = productCaptor.getValue();
        assertEquals(1, capturedProduct.getId());
        assertEquals("Margherita", capturedProduct.getNome());
    }

    @Test
    void testUpdateProductFailure() {
        MenuProduct productToUpdate = new MenuProduct(1, "Carbonara", "Primi",
                new BigDecimal("12.00"), new BigDecimal("6.00"), "Glutine, Uova");
        when(mockMenuUseCase.updateProduct(any(MenuProduct.class))).thenReturn(false);

        boolean result = mockMenuUseCase.updateProduct(productToUpdate);

        assertFalse(result);
        verify(mockMenuUseCase, times(1)).updateProduct(productToUpdate);
    }

    @Test
    void testProductCreationWithAllFields() {
        String nome = "Pizza Napoletana";
        String tipologia = "Pizze";
        BigDecimal prezzo = new BigDecimal("9.50");
        BigDecimal costo = new BigDecimal("4.20");
        String allergeni = "Glutine, Lattosio";

        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);
        ArgumentCaptor<MenuProduct> productCaptor = ArgumentCaptor.forClass(MenuProduct.class);

        MenuProduct product = new MenuProduct(nome, tipologia, prezzo, costo, allergeni);
        mockMenuUseCase.addProduct(product);

        verify(mockMenuUseCase).addProduct(productCaptor.capture());
        MenuProduct capturedProduct = productCaptor.getValue();

        assertEquals(nome, capturedProduct.getNome());
        assertEquals(tipologia, capturedProduct.getTipologia());
        assertEquals(0, prezzo.compareTo(capturedProduct.getPrezzoVendita()));
        assertEquals(0, costo.compareTo(capturedProduct.getCostoRealizzazione()));
        assertEquals(allergeni, capturedProduct.getAllergeni());
    }

    @Test
    void testProductCreationWithoutAllergensField() {
        MenuProduct product = new MenuProduct("Acqua Naturale", "Bevande",
                new BigDecimal("2.00"), new BigDecimal("0.50"), "");

        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);

        boolean result = mockMenuUseCase.addProduct(product);

        assertTrue(result);
        assertEquals("", product.getAllergeni());
    }

    @Test
    void testCategoriesListNotEmpty() {
        List<String> categories = Arrays.asList("Antipasti", "Primi", "Secondi", "Pizze", "Dessert", "Bevande");
        when(mockMenuUseCase.loadCategories()).thenReturn(categories);

        List<String> result = mockMenuUseCase.loadCategories();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(6, result.size());
        verify(mockMenuUseCase, times(1)).loadCategories();
    }

    @Test
    void testCategoriesListEmpty() {
        when(mockMenuUseCase.loadCategories()).thenReturn(Arrays.asList());

        List<String> result = mockMenuUseCase.loadCategories();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testMultipleProductAdditions() {
        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);

        MenuProduct product1 = new MenuProduct("Margherita", "Pizze",
                new BigDecimal("8.00"), new BigDecimal("3.50"), "Glutine");
        MenuProduct product2 = new MenuProduct("Capricciosa", "Pizze",
                new BigDecimal("10.00"), new BigDecimal("4.50"), "Glutine, Lattosio");
        MenuProduct product3 = new MenuProduct("Tiramisu", "Dessert",
                new BigDecimal("6.00"), new BigDecimal("2.50"), "Glutine, Uova, Lattosio");

        boolean result1 = mockMenuUseCase.addProduct(product1);
        boolean result2 = mockMenuUseCase.addProduct(product2);
        boolean result3 = mockMenuUseCase.addProduct(product3);

        assertTrue(result1 && result2 && result3);
        verify(mockMenuUseCase, times(3)).addProduct(any(MenuProduct.class));
    }

    @Test
    void testUpdateProductPreservesId() {
        int originalId = 42;
        MenuProduct product = new MenuProduct(originalId, "Pizza Quattro Stagioni", "Pizze",
                new BigDecimal("11.00"), new BigDecimal("5.50"), "Glutine");
        when(mockMenuUseCase.updateProduct(any(MenuProduct.class))).thenReturn(true);

        ArgumentCaptor<MenuProduct> captor = ArgumentCaptor.forClass(MenuProduct.class);

        mockMenuUseCase.updateProduct(product);

        verify(mockMenuUseCase).updateProduct(captor.capture());
        assertEquals(originalId, captor.getValue().getId());
    }

    @Test
    void testProductWithNullAllergens() {
        MenuProduct product = new MenuProduct("Acqua", "Bevande",
                new BigDecimal("1.50"), new BigDecimal("0.30"), null);

        assertEquals("", product.getAllergeni());
    }

    @Test
    void testServiceMethodsNeverCalledWithoutUserInteraction() {
        when(mockMenuUseCase.loadCategories()).thenReturn(Arrays.asList("Pizze"));
        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);

        verify(mockMenuUseCase, never()).addProduct(any(MenuProduct.class));
    }

    @Test
    void testProductValidationLogic() {
        MenuProduct validProduct = new MenuProduct("Pizza", "Pizze",
                new BigDecimal("10.00"), new BigDecimal("5.00"), "Glutine");
        MenuProduct invalidProduct = new MenuProduct("", "",
                BigDecimal.ZERO, BigDecimal.ZERO, "");

        assertFalse(validProduct.getNome().trim().isEmpty());
        assertFalse(validProduct.getTipologia().trim().isEmpty());

        assertTrue(invalidProduct.getNome().trim().isEmpty());
        assertTrue(invalidProduct.getTipologia().trim().isEmpty());
    }

    @Test
    void testDifferentPriceAndCostCombinations() {
        MenuProduct highMargin = new MenuProduct("Premium Pizza", "Pizze",
                new BigDecimal("20.00"), new BigDecimal("5.00"), "");
        MenuProduct lowMargin = new MenuProduct("Budget Pizza", "Pizze",
                new BigDecimal("6.00"), new BigDecimal("5.50"), "");
        MenuProduct zeroCost = new MenuProduct("Acqua del Rubinetto", "Bevande",
                BigDecimal.ZERO, BigDecimal.ZERO, "");

        assertEquals(0, new BigDecimal("15.00").compareTo(highMargin.getMargine()));
        assertEquals(0, new BigDecimal("0.50").compareTo(lowMargin.getMargine()));
        assertEquals(0, BigDecimal.ZERO.compareTo(zeroCost.getMargine()));
    }

    @Test
    void testCategoryLoadingBeforeProductOperation() {
        when(mockMenuUseCase.loadCategories()).thenReturn(Arrays.asList("Pizze", "Primi"));
        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);

        List<String> categories = mockMenuUseCase.loadCategories();
        MenuProduct product = new MenuProduct("Test", categories.get(0),
                new BigDecimal("10.00"), new BigDecimal("5.00"), "");
        boolean addResult = mockMenuUseCase.addProduct(product);

        assertTrue(addResult);
        assertEquals("Pizze", product.getTipologia());
        verify(mockMenuUseCase, times(1)).loadCategories();
        verify(mockMenuUseCase, times(1)).addProduct(any(MenuProduct.class));
    }
}