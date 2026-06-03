package rm.controller;

import rm.model.MenuProduct;
import java.util.List;

public interface MenuUseCase {
    List<MenuProduct> loadAllProducts();
    List<String> loadCategories();
    boolean addProduct(MenuProduct product);
    boolean updateProduct(MenuProduct product);
    MenuProduct getProductById(int id);  // per edit
}
