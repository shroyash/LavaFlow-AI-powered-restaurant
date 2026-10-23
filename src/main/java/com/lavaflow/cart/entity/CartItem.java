package com.lavaflow.cart.entity;

import com.lavaflow.common.entity.BaseEntity;
import com.lavaflow.menu.entity.MenuItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cart_item_menu_item",
                        columnNames = {"cart_id", "menu_item_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_cart_items_cart",
                        columnList = "cart_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}