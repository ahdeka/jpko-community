package com.jpkocommunity.domain.shop.repository;

import com.jpkocommunity.domain.shop.entity.ShopItem;
import com.jpkocommunity.domain.shop.entity.ShopItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopItemRepository extends JpaRepository<ShopItem, Long> {

    List<ShopItem> findByActiveTrue();

    List<ShopItem> findByTypeAndActiveTrue(ShopItemType type);
}
