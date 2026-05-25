package com.tradev.domain.item.repository;

import com.tradev.domain.item.dto.ItemSearchCondition;
import com.tradev.domain.item.dto.ItemSummary;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ItemMapper {

    List<ItemSummary> searchItems(ItemSearchCondition condition);
}
