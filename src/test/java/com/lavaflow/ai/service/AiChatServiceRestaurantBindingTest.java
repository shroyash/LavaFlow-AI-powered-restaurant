package com.lavaflow.ai.service;

import com.lavaflow.ai.entity.AiConversation;
import com.lavaflow.ai.entity.AiMessage;
import com.lavaflow.ai.repository.AiConversationRepository;
import com.lavaflow.ai.repository.AiMessageRepository;
import com.lavaflow.ai.tool.CustomerAddressTool;
import com.lavaflow.ai.tool.MenuSearchTool;
import com.lavaflow.ai.tool.NearbyRestaurantTool;
import com.lavaflow.ai.tool.RestaurantDiscoveryTool;
import com.lavaflow.ai.tool.CartTool;
import com.lavaflow.ai.tool.OrderTool;
import com.lavaflow.auth.entity.User;
import com.lavaflow.common.enums.ConversationStatus;
import com.lavaflow.common.enums.MessageSender;
import com.lavaflow.common.enums.RestaurantStatus;
import com.lavaflow.common.exception.BadRequestException;
import com.lavaflow.restaurant.entity.Restaurant;
import com.lavaflow.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatService Restaurant Binding and Tool Context Tests")
class AiChatServiceRestaurantBindingTest {

    @Mock
    private AiConversationRepository conversationRepository;

    @Mock
    private AiMessageRepository messageRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuSearchTool menuSearchTool;

    @Mock
    private RestaurantDiscoveryTool restaurantDiscoveryTool;

    @Mock
    private CustomerAddressTool customerAddressTool;

    @Mock
    private NearbyRestaurantTool nearbyRestaurantTool;

    @Mock
    private CartTool cartTool;

    @Mock
    private OrderTool orderTool;

    @Mock
    private com.lavaflow.cart.service.CartService cartService;

    @Mock
    private com.lavaflow.menu.repository.MenuItemRepository menuItemRepository;

    @Mock
    private ChatModel chatModel;

    private AiChatService aiChatService;

    private User customer;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        aiChatService = new AiChatService(
                builder,
                conversationRepository,
                messageRepository,
                restaurantRepository,
                menuSearchTool,
                restaurantDiscoveryTool,
                customerAddressTool,
                nearbyRestaurantTool,
                cartTool,
                orderTool,
                cartService,
                menuItemRepository
        );

        customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setEmail("customer@example.com");

        // Mock chatModel default response and options
        org.springframework.ai.ollama.api.OllamaChatOptions options = org.springframework.ai.ollama.api.OllamaChatOptions.builder().build();
        lenient().when(chatModel.getDefaultOptions()).thenReturn(options);
        lenient().when(chatModel.getOptions()).thenReturn(options);

        ChatResponse mockResponse = new ChatResponse(java.util.List.of(new Generation(new AssistantMessage("Hello!"))));
        lenient().when(chatModel.call(any(Prompt.class))).thenReturn(mockResponse);
    }

    @Test
    @DisplayName("Starting chat with valid ACTIVE restaurant binds it to new conversation and sets ToolContext")
    void chat_withValidActiveRestaurant_bindsToNewConversation() {
        UUID restaurantId = UUID.randomUUID();
        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Bella Italia");
        restaurant.setStatus(RestaurantStatus.ACTIVE);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(invocation -> {
            AiConversation conv = invocation.getArgument(0);
            conv.setId(UUID.randomUUID());
            return conv;
        });

        AiChatService.ChatResult result = aiChatService.chat(customer, null, restaurantId, "What's on the menu?");

        assertThat(result).isNotNull();
        assertThat(result.reply()).isEqualTo("Hello!");

        ArgumentCaptor<AiConversation> conversationCaptor = ArgumentCaptor.forClass(AiConversation.class);
        verify(conversationRepository).save(conversationCaptor.capture());
        AiConversation saved = conversationCaptor.getValue();
        assertThat(saved.getCustomer()).isEqualTo(customer);
        assertThat(saved.getRestaurant()).isEqualTo(restaurant);
        assertThat(saved.getStatus()).isEqualTo(ConversationStatus.ACTIVE);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        org.springframework.ai.chat.messages.Message userMsg = prompt.getInstructions().stream()
                .filter(m -> m.getMessageType() == org.springframework.ai.chat.messages.MessageType.USER)
                .findFirst()
                .orElseThrow();
        assertThat(userMsg.getText()).isEqualTo("What's on the menu?");
    }

    @Test
    @DisplayName("Starting chat with non-existent restaurant throws BadRequestException and does not create conversation")
    void chat_withNonExistentRestaurant_throwsBadRequestException() {
        UUID nonExistentId = UUID.randomUUID();
        when(restaurantRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiChatService.chat(customer, null, nonExistentId, "Hello"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Selected restaurant does not exist.");

        verifyNoInteractions(conversationRepository);
        verifyNoInteractions(chatModel);
    }

    @Test
    @DisplayName("Starting chat with INACTIVE restaurant throws BadRequestException and does not create conversation")
    void chat_withInactiveRestaurant_throwsBadRequestException() {
        UUID inactiveId = UUID.randomUUID();
        Restaurant inactiveRestaurant = new Restaurant();
        inactiveRestaurant.setId(inactiveId);
        inactiveRestaurant.setStatus(RestaurantStatus.INACTIVE);

        when(restaurantRepository.findById(inactiveId)).thenReturn(Optional.of(inactiveRestaurant));

        assertThatThrownBy(() -> aiChatService.chat(customer, null, inactiveId, "Hello"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Selected restaurant is currently inactive.");

        verifyNoInteractions(conversationRepository);
        verifyNoInteractions(chatModel);
    }

    @Test
    @DisplayName("Continuing chat without restaurantId retains existing conversation restaurant binding")
    void chat_withoutRestaurantId_preservesExistingBinding() {
        UUID conversationId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setStatus(RestaurantStatus.ACTIVE);

        AiConversation existing = new AiConversation();
        existing.setId(conversationId);
        existing.setCustomer(customer);
        existing.setRestaurant(restaurant);
        existing.setStatus(ConversationStatus.ACTIVE);

        when(conversationRepository.findByIdAndCustomer_Id(conversationId, customer.getId()))
                .thenReturn(Optional.of(existing));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        AiChatService.ChatResult result = aiChatService.chat(customer, conversationId, null, "Show drinks");

        assertThat(result).isNotNull();
        // Existing binding is preserved and not overwritten
        assertThat(existing.getRestaurant()).isEqualTo(restaurant);
        verify(restaurantRepository).findById(restaurantId);
    }

    @Test
    @DisplayName("Resuming conversation whose bound restaurant became INACTIVE unbinds restaurant so tools cannot access its data")
    void chat_withoutRestaurantId_whenBoundRestaurantBecomesInactive_unbindsRestaurant() {
        UUID conversationId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        Restaurant deactivatedRestaurant = new Restaurant();
        deactivatedRestaurant.setId(restaurantId);
        deactivatedRestaurant.setStatus(RestaurantStatus.INACTIVE);

        AiConversation existing = new AiConversation();
        existing.setId(conversationId);
        existing.setCustomer(customer);
        existing.setRestaurant(deactivatedRestaurant);
        existing.setStatus(ConversationStatus.ACTIVE);

        when(conversationRepository.findByIdAndCustomer_Id(conversationId, customer.getId()))
                .thenReturn(Optional.of(existing));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(deactivatedRestaurant));
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(inv -> inv.getArgument(0));

        AiChatService.ChatResult result = aiChatService.chat(customer, conversationId, null, "Show menu");

        assertThat(result).isNotNull();
        // Deactivated restaurant is unbound from conversation
        assertThat(existing.getRestaurant()).isNull();
        verify(conversationRepository).save(existing);
    }

    @Test
    @DisplayName("Explicitly requesting a new active restaurant switches the binding")
    void chat_withNewActiveRestaurant_explicitlySwitchesBinding() {
        UUID conversationId = UUID.randomUUID();
        UUID oldRestaurantId = UUID.randomUUID();
        Restaurant oldRestaurant = new Restaurant();
        oldRestaurant.setId(oldRestaurantId);
        oldRestaurant.setStatus(RestaurantStatus.ACTIVE);

        UUID newRestaurantId = UUID.randomUUID();
        Restaurant newRestaurant = new Restaurant();
        newRestaurant.setId(newRestaurantId);
        newRestaurant.setStatus(RestaurantStatus.ACTIVE);

        AiConversation existing = new AiConversation();
        existing.setId(conversationId);
        existing.setCustomer(customer);
        existing.setRestaurant(oldRestaurant);
        existing.setStatus(ConversationStatus.ACTIVE);

        when(restaurantRepository.findById(newRestaurantId)).thenReturn(Optional.of(newRestaurant));
        when(conversationRepository.findByIdAndCustomer_Id(conversationId, customer.getId()))
                .thenReturn(Optional.of(existing));
        when(conversationRepository.save(any(AiConversation.class))).thenAnswer(inv -> inv.getArgument(0));

        aiChatService.chat(customer, conversationId, newRestaurantId, "Switch to new place");

        assertThat(existing.getRestaurant()).isEqualTo(newRestaurant);
        verify(conversationRepository).save(existing);
    }

    @Test
    @DisplayName("Attempting to switch to invalid restaurant fails closed without mutating existing conversation binding")
    void chat_withInvalidNewRestaurant_failsClosedWithoutMutatingBinding() {
        UUID conversationId = UUID.randomUUID();
        UUID currentRestaurantId = UUID.randomUUID();
        Restaurant currentRestaurant = new Restaurant();
        currentRestaurant.setId(currentRestaurantId);
        currentRestaurant.setStatus(RestaurantStatus.ACTIVE);

        AiConversation existing = new AiConversation();
        existing.setId(conversationId);
        existing.setCustomer(customer);
        existing.setRestaurant(currentRestaurant);
        existing.setStatus(ConversationStatus.ACTIVE);

        UUID invalidId = UUID.randomUUID();
        when(restaurantRepository.findById(invalidId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiChatService.chat(customer, conversationId, invalidId, "Switch"))
                .isInstanceOf(BadRequestException.class);

        assertThat(existing.getRestaurant()).isEqualTo(currentRestaurant);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Accessing another customer's conversation throws 404 NOT_FOUND")
    void chat_otherCustomerConversation_throwsNotFound() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findByIdAndCustomer_Id(conversationId, customer.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiChatService.chat(customer, conversationId, null, "Hello"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Conversation not found.");

        verify(messageRepository, never()).save(any());
        verifyNoInteractions(chatModel);
    }

    @Test
    @DisplayName("Adding item to cart without selecting restaurant replies with selection prompt")
    void chat_addToCartWithoutRestaurant_promptsRestaurantSelection() {
        AiConversation existing = new AiConversation();
        existing.setId(UUID.randomUUID());
        existing.setCustomer(customer);
        existing.setRestaurant(null);
        existing.setStatus(ConversationStatus.ACTIVE);

        when(conversationRepository.findByIdAndCustomer_Id(existing.getId(), customer.getId()))
                .thenReturn(Optional.of(existing));

        AiChatService.ChatResult result = aiChatService.chat(customer, existing.getId(), null, "Add Chicken Pizza to my cart");

        assertThat(result).isNotNull();
        assertThat(result.reply()).isEqualTo("Please select a restaurant first before adding items to your cart.");
        verifyNoInteractions(chatModel);
    }

    @Test
    @DisplayName("Adding item to cart with selected restaurant resolves item and adds to cart directly")
    void chat_addToCartWithSelectedRestaurant_addsItemAndConfirms() {
        UUID conversationId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("City Chicken");
        restaurant.setStatus(RestaurantStatus.ACTIVE);

        AiConversation existing = new AiConversation();
        existing.setId(conversationId);
        existing.setCustomer(customer);
        existing.setRestaurant(restaurant);
        existing.setStatus(ConversationStatus.ACTIVE);

        when(conversationRepository.findByIdAndCustomer_Id(conversationId, customer.getId()))
                .thenReturn(Optional.of(existing));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        com.lavaflow.menu.entity.MenuItem menuItem = new com.lavaflow.menu.entity.MenuItem();
        UUID menuItemId = UUID.randomUUID();
        menuItem.setId(menuItemId);
        menuItem.setName("Chicken Pizza");
        menuItem.setPrice(BigDecimal.valueOf(1200.00));

        when(menuItemRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(menuItem)));

        com.lavaflow.cart.dto.CartResponse cartResponse = new com.lavaflow.cart.dto.CartResponse(
                UUID.randomUUID(),
                restaurantId,
                "City Chicken",
                List.of(),
                BigDecimal.valueOf(1200.00),
                1
        );
        when(cartService.addItem(customer.getId(), menuItemId, 1)).thenReturn(cartResponse);

        AiChatService.ChatResult result = aiChatService.chat(customer, conversationId, null, "Add Chicken Pizza to my cart");

        assertThat(result).isNotNull();
        assertThat(result.reply()).contains("Added **Chicken Pizza** (Qty: 1) to your cart!");
        assertThat(result.reply()).contains("Rs. 1200.00");
        verify(cartService).addItem(customer.getId(), menuItemId, 1);
        verifyNoInteractions(chatModel);
    }

    @Test
    @DisplayName("Saving delivery address from 'save my delivery address X' phrase calls CustomerAddressTool and confirms")
    void chat_saveDeliveryAddress_phrasedAsNaturalLanguage_savesAndConfirms() {
        AiConversation existing = new AiConversation();
        existing.setId(UUID.randomUUID());
        existing.setCustomer(customer);
        existing.setRestaurant(null);
        existing.setStatus(ConversationStatus.ACTIVE);

        when(conversationRepository.findByIdAndCustomer_Id(existing.getId(), customer.getId()))
                .thenReturn(Optional.of(existing));

        com.lavaflow.ai.tool.dto.CustomerAddressToolResponse savedAddr =
                new com.lavaflow.ai.tool.dto.CustomerAddressToolResponse(
                        UUID.randomUUID(), null, "Patan, Lalitpur", false
                );
        com.lavaflow.ai.tool.dto.SaveAddressResult saveResult =
                com.lavaflow.ai.tool.dto.SaveAddressResult.success(savedAddr);

        when(customerAddressTool.saveDeliveryAddress(any(), any())).thenReturn(saveResult);

        AiChatService.ChatResult result = aiChatService.chat(
                customer, existing.getId(), null, "save my delivery address Patan");

        assertThat(result).isNotNull();
        assertThat(result.reply()).contains("Patan, Lalitpur");
        assertThat(result.reply()).contains("saved");
        verify(customerAddressTool).saveDeliveryAddress(any(), any());
        verifyNoInteractions(chatModel);
    }

    @Test
    @DisplayName("'saved my delivery address Patan' phrasing also triggers address save (past-tense form)")
    void chat_savedMyDeliveryAddressPhrasing_savesAndConfirms() {
        AiConversation existing = new AiConversation();
        existing.setId(UUID.randomUUID());
        existing.setCustomer(customer);
        existing.setRestaurant(null);
        existing.setStatus(ConversationStatus.ACTIVE);

        when(conversationRepository.findByIdAndCustomer_Id(existing.getId(), customer.getId()))
                .thenReturn(Optional.of(existing));

        com.lavaflow.ai.tool.dto.CustomerAddressToolResponse savedAddr =
                new com.lavaflow.ai.tool.dto.CustomerAddressToolResponse(
                        UUID.randomUUID(), null, "Patan", false
                );
        com.lavaflow.ai.tool.dto.SaveAddressResult saveResult =
                com.lavaflow.ai.tool.dto.SaveAddressResult.success(savedAddr);

        when(customerAddressTool.saveDeliveryAddress(any(), any())).thenReturn(saveResult);

        AiChatService.ChatResult result = aiChatService.chat(
                customer, existing.getId(), null, "saved my delivery address patan");

        assertThat(result).isNotNull();
        assertThat(result.reply()).contains("saved");
        verify(customerAddressTool).saveDeliveryAddress(any(), any());
        verifyNoInteractions(chatModel);
    }
}
