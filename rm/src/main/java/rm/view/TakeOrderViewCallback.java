package rm.view;

/**
 * Interfaccia BCE: contratto tra il controller applicativo
 * (TakeOrderController) e la boundary (TakeOrderView / TakeOrderEinkView).
 * Il controller notifica la view tramite questi metodi.
 */
public interface TakeOrderViewCallback {

    void onProductsLoaded(int count);

    void onCartUpdated(int totalItems);

    void onOrderSuccess();

    void onOrderFailure(String message);
}