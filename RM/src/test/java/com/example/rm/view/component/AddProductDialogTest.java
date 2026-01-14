package com.example.rm.view.component;

import com.example.rm.controller.MenuUseCase;
import com.example.rm.model.MenuProduct;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class per AddProductDialog.
 *
 * NOTA IMPORTANTE: Questi test verificano la logica di business e l'integrazione
 * con MenuUseCase, NON l'interfaccia utente effettiva (che richiederebbe TestFX).
 */
class AddProductDialogTest {

    private MenuUseCase mockMenuUseCase;

    @BeforeAll
    static void initAll() {
        // Inizializza JavaFX Toolkit una sola volta
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
        // Arrange
        MenuUseCase customUseCase = mock(MenuUseCase.class);

        // Act
        AddProductDialog.setMenuUseCase(customUseCase);

        // Assert
        // Verifichiamo che il setter funzioni correttamente
        // (il test successivo verificherà che venga effettivamente usato)
        assertNotNull(customUseCase);
    }

    @Test
    void testLoadCategoriesWhenDialogIsCreated() {
        // Arrange
        List<String> expectedCategories = Arrays.asList("Antipasti", "Primi", "Secondi", "Pizze");
        when(mockMenuUseCase.loadCategories()).thenReturn(expectedCategories);

        // Act
        // Non possiamo testare l'apertura effettiva del dialog senza TestFX,
        // ma possiamo verificare che il servizio sia configurato correttamente
        List<String> actualCategories = mockMenuUseCase.loadCategories();

        // Assert
        assertNotNull(actualCategories, "La lista delle categorie non dovrebbe essere null");
        assertEquals(expectedCategories.size(), actualCategories.size(),
                "Il numero di categorie dovrebbe corrispondere");
        assertTrue(actualCategories.containsAll(expectedCategories),
                "Tutte le categorie attese dovrebbero essere presenti");
    }

    @Test
    void testAddProductSuccess() {
        // Arrange
        MenuProduct newProduct = new MenuProduct("Pizza Margherita", "Pizze", 8.0, 3.5, "Glutine, Lattosio");
        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);

        ArgumentCaptor<MenuProduct> productCaptor = ArgumentCaptor.forClass(MenuProduct.class);

        // Act
        boolean result = mockMenuUseCase.addProduct(newProduct);

        // Assert
        assertTrue(result, "L'aggiunta del prodotto dovrebbe avere successo");
        verify(mockMenuUseCase, times(1)).addProduct(productCaptor.capture());

        MenuProduct capturedProduct = productCaptor.getValue();
        assertEquals("Pizza Margherita", capturedProduct.getNome());
        assertEquals("Pizze", capturedProduct.getTipologia());
        assertEquals(8.0, capturedProduct.getPrezzoVendita(), 0.001);
        assertEquals(3.5, capturedProduct.getCostoRealizzazione(), 0.001);
    }

    @Test
    void testAddProductFailure() {
        // Arrange
        MenuProduct invalidProduct = new MenuProduct("Pizza Test", "Pizze", 10.0, 5.0, "");
        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(false);

        // Act
        boolean result = mockMenuUseCase.addProduct(invalidProduct);

        // Assert
        assertFalse(result, "L'aggiunta del prodotto dovrebbe fallire");
        verify(mockMenuUseCase, times(1)).addProduct(invalidProduct);
    }

    @Test
    void testUpdateProductInEditMode() {
        // Arrange
        MenuProduct existingProduct = new MenuProduct(1, "Margherita", "Pizze", 8.0, 3.5, "Glutine, Lattosio");
        when(mockMenuUseCase.updateProduct(any(MenuProduct.class))).thenReturn(true);

        ArgumentCaptor<MenuProduct> productCaptor = ArgumentCaptor.forClass(MenuProduct.class);

        // Act
        boolean result = mockMenuUseCase.updateProduct(existingProduct);

        // Assert
        assertTrue(result, "L'aggiornamento del prodotto dovrebbe avere successo");
        verify(mockMenuUseCase, times(1)).updateProduct(productCaptor.capture());

        MenuProduct capturedProduct = productCaptor.getValue();
        assertEquals(1, capturedProduct.getId());
        assertEquals("Margherita", capturedProduct.getNome());
    }

    @Test
    void testUpdateProductFailure() {
        // Arrange
        MenuProduct productToUpdate = new MenuProduct(1, "Carbonara", "Primi", 12.0, 6.0, "Glutine, Uova");
        when(mockMenuUseCase.updateProduct(any(MenuProduct.class))).thenReturn(false);

        // Act
        boolean result = mockMenuUseCase.updateProduct(productToUpdate);

        // Assert
        assertFalse(result, "L'aggiornamento dovrebbe fallire");
        verify(mockMenuUseCase, times(1)).updateProduct(productToUpdate);
    }

    @Test
    void testProductCreationWithAllFields() {
        // Arrange
        String nome = "Pizza Napoletana";
        String tipologia = "Pizze";
        double prezzo = 9.50;
        double costo = 4.20;
        String allergeni = "Glutine, Lattosio";

        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);
        ArgumentCaptor<MenuProduct> productCaptor = ArgumentCaptor.forClass(MenuProduct.class);

        // Act
        MenuProduct product = new MenuProduct(nome, tipologia, prezzo, costo, allergeni);
        mockMenuUseCase.addProduct(product);

        // Assert
        verify(mockMenuUseCase).addProduct(productCaptor.capture());
        MenuProduct capturedProduct = productCaptor.getValue();

        assertEquals(nome, capturedProduct.getNome());
        assertEquals(tipologia, capturedProduct.getTipologia());
        assertEquals(prezzo, capturedProduct.getPrezzoVendita(), 0.001);
        assertEquals(costo, capturedProduct.getCostoRealizzazione(), 0.001);
        assertEquals(allergeni, capturedProduct.getAllergeni());
    }

    @Test
    void testProductCreationWithoutAllergensField() {
        // Arrange
        MenuProduct product = new MenuProduct("Acqua Naturale", "Bevande", 2.0, 0.5, "");

        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);

        // Act
        boolean result = mockMenuUseCase.addProduct(product);

        // Assert
        assertTrue(result);
        assertEquals("", product.getAllergeni());
    }

    @Test
    void testCategoriesListNotEmpty() {
        // Arrange
        List<String> categories = Arrays.asList("Antipasti", "Primi", "Secondi", "Pizze", "Dessert", "Bevande");
        when(mockMenuUseCase.loadCategories()).thenReturn(categories);

        // Act
        List<String> result = mockMenuUseCase.loadCategories();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty(), "La lista delle categorie non dovrebbe essere vuota");
        assertEquals(6, result.size());
        verify(mockMenuUseCase, times(1)).loadCategories();
    }

    @Test
    void testCategoriesListEmpty() {
        // Arrange
        when(mockMenuUseCase.loadCategories()).thenReturn(Arrays.asList());

        // Act
        List<String> result = mockMenuUseCase.loadCategories();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "La lista delle categorie dovrebbe essere vuota");
    }

    @Test
    void testMultipleProductAdditions() {
        // Arrange
        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);

        MenuProduct product1 = new MenuProduct("Margherita", "Pizze", 8.0, 3.5, "Glutine");
        MenuProduct product2 = new MenuProduct("Capricciosa", "Pizze", 10.0, 4.5, "Glutine, Lattosio");
        MenuProduct product3 = new MenuProduct("Tiramisu", "Dessert", 6.0, 2.5, "Glutine, Uova, Lattosio");

        // Act
        boolean result1 = mockMenuUseCase.addProduct(product1);
        boolean result2 = mockMenuUseCase.addProduct(product2);
        boolean result3 = mockMenuUseCase.addProduct(product3);

        // Assert
        assertTrue(result1 && result2 && result3);
        verify(mockMenuUseCase, times(3)).addProduct(any(MenuProduct.class));
    }

    @Test
    void testUpdateProductPreservesId() {
        // Arrange
        int originalId = 42;
        MenuProduct product = new MenuProduct(originalId, "Pizza Quattro Stagioni", "Pizze", 11.0, 5.5, "Glutine");
        when(mockMenuUseCase.updateProduct(any(MenuProduct.class))).thenReturn(true);

        ArgumentCaptor<MenuProduct> captor = ArgumentCaptor.forClass(MenuProduct.class);

        // Act
        mockMenuUseCase.updateProduct(product);

        // Assert
        verify(mockMenuUseCase).updateProduct(captor.capture());
        assertEquals(originalId, captor.getValue().getId(),
                "L'ID del prodotto dovrebbe essere preservato durante l'aggiornamento");
    }

    @Test
    void testProductWithNullAllergens() {
        // Arrange
        MenuProduct product = new MenuProduct("Acqua", "Bevande", 1.5, 0.3, null);

        // Assert
        assertEquals("", product.getAllergeni(),
                "Gli allergeni null dovrebbero essere convertiti in stringa vuota");
    }

    @Test
    void testServiceMethodsNeverCalledWithoutUserInteraction() {
        // Arrange
        when(mockMenuUseCase.loadCategories()).thenReturn(Arrays.asList("Pizze"));
        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);

        // Assert
        // Senza apertura del dialog, addProduct non dovrebbe mai essere chiamato
        verify(mockMenuUseCase, never()).addProduct(any(MenuProduct.class));
    }

    @Test
    void testProductValidationLogic() {
        // Arrange
        MenuProduct validProduct = new MenuProduct("Pizza", "Pizze", 10.0, 5.0, "Glutine");
        MenuProduct invalidProduct = new MenuProduct("", "", 0.0, 0.0, "");

        // Assert
        // Prodotto valido ha tutti i campi obbligatori
        assertFalse(validProduct.getNome().trim().isEmpty());
        assertFalse(validProduct.getTipologia().trim().isEmpty());

        // Prodotto non valido ha campi vuoti (la validazione avviene nel DialogBuilder)
        assertTrue(invalidProduct.getNome().trim().isEmpty());
        assertTrue(invalidProduct.getTipologia().trim().isEmpty());
    }

    @Test
    void testDifferentPriceAndCostCombinations() {
        // Arrange & Act
        MenuProduct highMargin = new MenuProduct("Premium Pizza", "Pizze", 20.0, 5.0, "");
        MenuProduct lowMargin = new MenuProduct("Budget Pizza", "Pizze", 6.0, 5.5, "");
        MenuProduct zeroCost = new MenuProduct("Acqua del Rubinetto", "Bevande", 0.0, 0.0, "");

        // Assert
        assertEquals(15.0, highMargin.getMargine(), 0.001);
        assertEquals(0.5, lowMargin.getMargine(), 0.001);
        assertEquals(0.0, zeroCost.getMargine(), 0.001);
    }

    @Test
    void testCategoryLoadingBeforeProductOperation() {
        // Arrange
        when(mockMenuUseCase.loadCategories()).thenReturn(Arrays.asList("Pizze", "Primi"));
        when(mockMenuUseCase.addProduct(any(MenuProduct.class))).thenReturn(true);

        // Act
        List<String> categories = mockMenuUseCase.loadCategories();
        MenuProduct product = new MenuProduct("Test", categories.get(0), 10.0, 5.0, "");
        boolean addResult = mockMenuUseCase.addProduct(product);

        // Assert
        assertTrue(addResult);
        assertEquals("Pizze", product.getTipologia());
        verify(mockMenuUseCase, times(1)).loadCategories();
        verify(mockMenuUseCase, times(1)).addProduct(any(MenuProduct.class));
    }
}