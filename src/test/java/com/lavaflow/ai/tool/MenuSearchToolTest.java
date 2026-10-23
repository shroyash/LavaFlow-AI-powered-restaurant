package com.lavaflow.ai.tool;

import com.lavaflow.ai.tool.dto.MenuItemToolResponse;
import com.lavaflow.ai.tool.dto.MenuSearchInput;
import com.lavaflow.ai.tool.dto.MenuSearchResult;
import com.lavaflow.menu.dto.MenuItemResponse;
import com.lavaflow.menu.service.MenuItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuSearchTool Tests")
class MenuSearchToolTest {

    @Mock
    private MenuItemService menuItemService;

    private MenuSearchTool menuSearchTool;

    @BeforeEach
    void setUp() {
        menuSearchTool = new MenuSearchTool(menuItemService);
    }

    @Test
    @DisplayName("searchMenu fails closed when ToolContext has no restaurantId")
    void searchMenu_noRestaurantId_returnsNoRestaurantSelected() {
        ToolContext toolContext = new ToolContext(Map.of("customerId", UUID.randomUUID()));

        MenuSearchResult result = menuSearchTool.searchMenu(new MenuSearchInput("pizza"), toolContext);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).containsIgnoringCase("no restaurant is currently selected");
        assertThat(result.items()).isEmpty();
        verifyNoInteractions(menuItemService);
    }

    @Test
    @DisplayName("searchMenu fails closed when ToolContext is null")
    void searchMenu_nullToolContext_returnsNoRestaurantSelected() {
        MenuSearchResult result = menuSearchTool.searchMenu(new MenuSearchInput("pizza"), null);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).containsIgnoringCase("no restaurant is currently selected");
        assertThat(result.items()).isEmpty();
        verifyNoInteractions(menuItemService);
    }

    @Test
    @DisplayName("searchMenu rejects null or blank search queries")
    void searchMenu_blankQuery_returnsValidationError() {
        UUID restaurantId = UUID.randomUUID();
        ToolContext toolContext = new ToolContext(Map.of("restaurantId", restaurantId));

        MenuSearchResult nullResult = menuSearchTool.searchMenu(null, toolContext);
        assertThat(nullResult.success()).isFalse();
        assertThat(nullResult.message()).containsIgnoringCase("must not be blank");

        MenuSearchResult blankResult = menuSearchTool.searchMenu(new MenuSearchInput("   "), toolContext);
        assertThat(blankResult.success()).isFalse();
        assertThat(blankResult.message()).containsIgnoringCase("must not be blank");

        verifyNoInteractions(menuItemService);
    }

    @Test
    @DisplayName("searchMenu rejects oversized queries without silently truncating")
    void searchMenu_oversizedQuery_returnsValidationError() {
        UUID restaurantId = UUID.randomUUID();
        ToolContext toolContext = new ToolContext(Map.of("restaurantId", restaurantId));
        String oversizedQuery = "a".repeat(101);

        MenuSearchResult result = menuSearchTool.searchMenu(new MenuSearchInput(oversizedQuery), toolContext);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).containsIgnoringCase("exceeds the maximum allowed length of 100");
        assertThat(result.items()).isEmpty();
        verifyNoInteractions(menuItemService);
    }

    @Test
    @DisplayName("searchMenu delegates with exact restaurantId, available=true, page limit 10, and allowlisted fields")
    void searchMenu_validContextAndQuery_delegatesToMenuItemService() {
        UUID restaurantId = UUID.randomUUID();
        ToolContext toolContext = new ToolContext(Map.of("restaurantId", restaurantId));

        MenuItemResponse itemResponse = MenuItemResponse.builder()
                .id(UUID.randomUUID().toString())
                .restaurantId(restaurantId.toString())
                .categoryId(UUID.randomUUID().toString())
                .categoryName("Pizzas")
                .name("Margherita")
                .description("Fresh tomato sauce and mozzarella")
                .price(BigDecimal.valueOf(12.99))
                .imageUrl("https://example.com/margherita.jpg")
                .available(true)
                .build();

        when(menuItemService.getItems(eq(restaurantId), isNull(), eq(true), eq("margherita"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(itemResponse)));

        MenuSearchResult result = menuSearchTool.searchMenu(new MenuSearchInput("  margherita  "), toolContext);

        assertThat(result.success()).isTrue();
        assertThat(result.items()).hasSize(1);

        MenuItemToolResponse toolResponse = result.items().get(0);
        assertThat(toolResponse.name()).isEqualTo("Margherita");
        assertThat(toolResponse.categoryName()).isEqualTo("Pizzas");
        assertThat(toolResponse.description()).isEqualTo("Fresh tomato sauce and mozzarella");
        assertThat(toolResponse.price()).isEqualByComparingTo("12.99");
        assertThat(toolResponse.available()).isTrue();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(menuItemService).getItems(eq(restaurantId), isNull(), eq(true), eq("margherita"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("searchMenu accepts restaurantId formatted as String in ToolContext")
    void searchMenu_stringFormattedRestaurantId_parsedCorrectly() {
        UUID restaurantId = UUID.randomUUID();
        ToolContext toolContext = new ToolContext(Map.of("restaurantId", restaurantId.toString()));

        when(menuItemService.getItems(eq(restaurantId), isNull(), eq(true), eq("burger"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        MenuSearchResult result = menuSearchTool.searchMenu(new MenuSearchInput("burger"), toolContext);

        assertThat(result.success()).isTrue();
        verify(menuItemService).getItems(eq(restaurantId), isNull(), eq(true), eq("burger"), any(Pageable.class));
    }
}

