package com.lavaflow.inventory.entity;

import com.lavaflow.common.entity.BaseEntity;
import com.lavaflow.menu.entity.MenuItem;
import com.lavaflow.outlet.entity.Outlet;
import com.lavaflow.restaurant.entity.Restaurant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "inventory_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_inventory_outlet_menu_item", columnNames = {"outlet_id", "menu_item_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_id", nullable = false)
    private Outlet outlet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;
}
