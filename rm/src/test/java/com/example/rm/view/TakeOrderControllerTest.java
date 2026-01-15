package com.example.rm.view;

import com.example.rm.app.UserSession;
import com.example.rm.model.MenuProduct;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TakeOrderControllerCartTest {

    @BeforeAll
    static void initSessionOnce() {
        UserSession.cleanUserSession();
        UserSession.getInstance(mock(User.class)); // basta che NON sia null
    }

    @Test
    void incrementTwice_thenTotalItemsIs2_andCartHasQuantity2() throws Exception {
        TakeOrderController controller = new TakeOrderController();

        MenuProduct prodotto = mock(MenuProduct.class);
        when(prodotto.getId()).thenReturn(10);
        when(prodotto.getNome()).thenReturn("Pizza");
        when(prodotto.getPrezzoVendita()).thenReturn(12.50);
        when(prodotto.getCostoRealizzazione()).thenReturn(5.00);

        invoke(controller, "incrementProductQuantity", MenuProduct.class, prodotto);
        invoke(controller, "incrementProductQuantity", MenuProduct.class, prodotto);

        Map<Integer, OrderItem> cart = getCart(controller);
        assertEquals(1, cart.size());
        assertEquals(2, cart.get(10).getQuantita());

        int total = (int) invoke(controller, "calculateTotalItems");
        assertEquals(2, total);
    }

    @Test
    void decrementFrom1_thenProductRemoved() throws Exception {
        TakeOrderController controller = new TakeOrderController();

        MenuProduct prodotto = mock(MenuProduct.class);
        when(prodotto.getId()).thenReturn(10);
        when(prodotto.getNome()).thenReturn("Pizza");
        when(prodotto.getPrezzoVendita()).thenReturn(12.50);
        when(prodotto.getCostoRealizzazione()).thenReturn(5.00);

        invoke(controller, "incrementProductQuantity", MenuProduct.class, prodotto);
        invoke(controller, "decrementProductQuantity", MenuProduct.class, prodotto);

        Map<Integer, OrderItem> cart = getCart(controller);
        assertTrue(cart.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, OrderItem> getCart(TakeOrderController controller) throws Exception {
        Field f = TakeOrderController.class.getDeclaredField("carrello");
        f.setAccessible(true);
        return (Map<Integer, OrderItem>) f.get(controller);
    }

    private Object invoke(Object target, String methodName, Class<?> paramType, Object arg) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName, paramType);
        m.setAccessible(true);
        return m.invoke(target, arg);
    }

    private Object invoke(Object target, String methodName) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        return m.invoke(target);
    }
}
