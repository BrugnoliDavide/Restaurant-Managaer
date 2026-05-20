package com.example.rm.util;

import com.example.rm.app.UsersFactory;
import com.example.rm.bean.OrderBean;
import com.example.rm.bean.OrderItemBean;
import com.example.rm.bean.ProductBean;
import com.example.rm.bean.UserBean;
import com.example.rm.model.MenuProduct;
import com.example.rm.model.Order;
import com.example.rm.model.OrderItem;
import com.example.rm.model.User;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converte tra Bean (layer DAO, puro Java) e Model (layer UI, JavaFX Properties).
 *
 * <p>Regola architetturale:</p>
 * <ul>
 *   <li>I DAO restituiscono Bean (nessuna dipendenza JavaFX).</li>
 *   <li>I Service mappano a Model prima di restituirli al layer superiore.</li>
 *   <li>Controller e View ricevono sempre Model, come in precedenza.</li>
 * </ul>
 */
public final class BeanMapper {

    private BeanMapper() {
        throw new IllegalStateException("Utility class");
    }

    // =========================================================================
    //  ProductBean  ↔  MenuProduct
    // =========================================================================

    public static MenuProduct toModel(ProductBean bean) {
        if (bean == null) return null;
        return new MenuProduct(
                bean.getId(),
                bean.getNome(),
                bean.getTipologia(),
                bean.getPrezzoVendita(),
                bean.getCostoRealizzazione(),
                bean.getAllergeni()
        );
    }

    public static ProductBean toBean(MenuProduct model) {
        if (model == null) return null;
        return new ProductBean(
                model.getId(),
                model.getNome(),
                model.getTipologia(),
                model.getPrezzoVendita(),
                model.getCostoRealizzazione(),
                model.getAllergeni()
        );
    }

    // =========================================================================
    //  OrderBean  ↔  Order
    // =========================================================================

    public static Order toModel(OrderBean bean) {
        if (bean == null) return null;
        return new Order(
                bean.getId(),
                bean.getDataOra(),
                bean.getTavolo(),
                bean.getUsername(),
                bean.getNote(),
                bean.getStatus(),
                bean.getTotale()
        );
    }

    public static OrderBean toBean(Order model) {
        if (model == null) return null;
        return new OrderBean(
                model.getId(),
                model.getDataOra(),
                model.getTavolo(),
                model.getUsername(),
                model.getNote(),
                model.getStatus(),
                model.getTotale()
        );
    }

    // =========================================================================
    //  OrderItemBean  ↔  OrderItem
    // =========================================================================

    /**
     * Converte un OrderItemBean in OrderItem.
     * La tipologia del prodotto non è presente nello snapshot e viene lasciata vuota;
     * se necessaria (es. decomposizione ordini) usare il metodo DAO interno apposito.
     */
    public static OrderItem toModel(OrderItemBean bean) {
        if (bean == null) return null;

        MenuProduct product = new MenuProduct();
        product.setId(bean.getMenuItemId());
        product.setNome(bean.getNomeProdottoSnapshot() != null
                ? bean.getNomeProdottoSnapshot() : "");
        product.setTipologia(""); // non disponibile nello snapshot

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantita(bean.getQuantita());
        item.setPrezzoSnapshot(bean.getPrezzoVenditaSnapshot());
        item.setCostoSnapshot(bean.getCostoRealizzazioneSnapshot());
        item.setNomeSnapshot(bean.getNomeProdottoSnapshot());
        return item;
    }

    /**
     * Converte un OrderItem in OrderItemBean.
     *
     * @param model   articolo da convertire
     * @param orderId ID dell'ordine padre (assente nell'OrderItem)
     */
    public static OrderItemBean toBean(OrderItem model, int orderId) {
        if (model == null) return null;
        return new OrderItemBean(
                orderId,
                model.getProduct() != null ? model.getProduct().getId() : 0,
                model.getQuantita(),
                model.getPrezzoSnapshot(),
                model.getCostoSnapshot(),
                model.getNomeSnapshot()
        );
    }

    // =========================================================================
    //  UserBean  ↔  User
    // =========================================================================

    /**
     * Ricostruisce il Model polimorfico corretto tramite UsersFactory.
     * Il passwordHash non viene mai trasportato nel Model.
     */
    public static User toModel(UserBean bean) {
        if (bean == null) return null;
        return UsersFactory.createUser(bean.getUsername(), bean.getRole());
    }

    public static UserBean toBean(User model) {
        if (model == null) return null;
        // passwordHash NON viene esposto fuori dal layer di sicurezza
        return new UserBean(model.getUsername(), null, model.getRole());
    }

    // =========================================================================
    //  Conversioni su Liste
    // =========================================================================

    public static List<MenuProduct> toProductModels(List<ProductBean> beans) {
        if (beans == null) return Collections.emptyList();
        return beans.stream().map(BeanMapper::toModel).collect(Collectors.toList());
    }

    public static List<ProductBean> toProductBeans(List<MenuProduct> models) {
        if (models == null) return Collections.emptyList();
        return models.stream().map(BeanMapper::toBean).collect(Collectors.toList());
    }

    public static List<Order> toOrderModels(List<OrderBean> beans) {
        if (beans == null) return Collections.emptyList();
        return beans.stream().map(BeanMapper::toModel).collect(Collectors.toList());
    }

    public static List<OrderBean> toOrderBeans(List<Order> models) {
        if (models == null) return Collections.emptyList();
        return models.stream().map(BeanMapper::toBean).collect(Collectors.toList());
    }

    public static List<OrderItem> toOrderItemModels(List<OrderItemBean> beans) {
        if (beans == null) return Collections.emptyList();
        return beans.stream().map(BeanMapper::toModel).collect(Collectors.toList());
    }

    public static List<User> toUserModels(List<UserBean> beans) {
        if (beans == null) return Collections.emptyList();
        return beans.stream().map(BeanMapper::toModel).collect(Collectors.toList());
    }
}