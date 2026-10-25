package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.*;
import com.prashant.api.ecom.ducart.exception.*;
import com.prashant.api.ecom.ducart.modal.*;
import com.prashant.api.ecom.ducart.repositories.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
class OrderPlacementIntegrationTest {
    @Autowired CheckoutService service;
    @Autowired ProductRepo products;
    @Autowired UserRepo users;
    @MockitoSpyBean CheckoutRepo orders;
    @Autowired MockMvc mvc;
    Product product;

    @BeforeEach
    void setup() {
        orders.deleteAll();
        products.deleteAll();
        users.deleteAll();
        for (String username : List.of("buyer", "other")) {
            User user = new User();
            user.setName(username);
            user.setUsername(username);
            user.setEmail(username + "@example.com");
            user.setPhone(username);
            user.setPassword("encoded");
            users.saveAndFlush(user);
        }
        product = new Product();
        product.setName("Server priced item");
        product.setBasePrice(24.99);
        product.setDiscount(0.0);
        product.setFinalPrice(0.01); // Checkout must not trust a stale derived catalogue price.
        product.setActive(true);
        product.setStock(true);
        product.setStockQuantity(3);
        product = products.saveAndFlush(product);
    }

    PlaceOrderRequest request(String key, int qty) {
        return new PlaceOrderRequest(key, "COD", List.of(new PlaceOrderRequest.Item(product.getId(), qty)));
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void forgedAmountsOwnerStatusAndStockAreIgnored() throws Exception {
        mvc.perform(post("/checkout").contentType(MediaType.APPLICATION_JSON).content("""
                {"idempotencyKey":"forged-request","paymentMode":"Card Demo",
                 "user":"other","subtotal":0.01,"shipping":0,"total":0.01,
                 "paymentStatus":"Paid","orderStatus":"Delivered","date":"yesterday",
                 "products":[{"productId":%d,"qty":2,"price":0.01,"stockQuantity":999999}]}
                """.formatted(product.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user").value("buyer"))
                .andExpect(jsonPath("$.subtotal").value(49.98))
                .andExpect(jsonPath("$.shipping").value(4.99))
                .andExpect(jsonPath("$.total").value(54.97))
                .andExpect(jsonPath("$.orderStatus").value("Order is Placed"))
                .andExpect(jsonPath("$.paymentStatus").value("Pending"))
                .andExpect(jsonPath("$.products[0].price").value(24.99));
        assertEquals(1, products.findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(new BigDecimal("54.97"), orders.findAll().get(0).getTotal());
    }

    @Test
    void exactDecimalsDiscountAndShippingThreshold() throws Exception {
        product.setBasePrice(0.10);
        products.saveAndFlush(product);
        assertEquals(new BigDecimal("0.30"), service.placeOrder("buyer", request("decimal-request", 3)).getSubtotal());
        product.setBasePrice(125.0);
        product.setDiscount(20.0);
        products.saveAndFlush(product);
        CheckoutDTO order = service.placeOrder("buyer", request("threshold-request", 1));
        assertEquals(new BigDecimal("100.00"), order.getTotal());
        assertEquals(new BigDecimal("0.00"), order.getShipping());
    }

    @Test
    void unavailableMissingAndInsufficientProductsRejectWithoutOrders() {
        assertThrows(ConflictException.class, () -> service.placeOrder("buyer", request("too-many-items", 4)));
        product.setActive(false);
        products.saveAndFlush(product);
        assertThrows(ConflictException.class, () -> service.placeOrder("buyer", request("inactive-item", 1)));
        assertThrows(ResourceNotFoundException.class, () -> service.placeOrder("buyer",
                new PlaceOrderRequest("missing-item", "COD", List.of(new PlaceOrderRequest.Item(Long.MAX_VALUE, 1)))));
        assertEquals(0, orders.count());
        assertEquals(3, products.findById(product.getId()).orElseThrow().getStockQuantity());
    }

    @Test
    void duplicateLinesAreAggregatedBeforeCheckingInventory() throws Exception {
        PlaceOrderRequest request = new PlaceOrderRequest("duplicate-lines", "COD", List.of(
                new PlaceOrderRequest.Item(product.getId(), 1), new PlaceOrderRequest.Item(product.getId(), 2)));
        CheckoutDTO order = service.placeOrder("buyer", request);
        assertEquals(1, order.getProducts().size());
        assertEquals(3, order.getProducts().get(0).getQty());
        assertFalse(products.findById(product.getId()).orElseThrow().isStock());
        assertEquals(order.getId(), service.placeOrder("buyer", request("duplicate-lines", 3)).getId());
    }

    @Test
    void repeatedKeyReturnsOriginalEvenAfterStockAndPriceChange() throws Exception {
        CheckoutDTO first = service.placeOrder("buyer", request("retry-request", 3));
        product = products.findById(product.getId()).orElseThrow();
        product.setBasePrice(100.0);
        products.saveAndFlush(product);
        CheckoutDTO retry = service.placeOrder("buyer", request("retry-request", 3));
        assertEquals(first.getId(), retry.getId());
        assertEquals(first.getTotal(), retry.getTotal());
        assertEquals(1, orders.count());
        assertThrows(ConflictException.class, () -> service.placeOrder("buyer", request("retry-request", 1)));
    }

    @Test
    void failureOnLaterProductRollsBackEarlierDeduction() {
        assertThrows(ResourceNotFoundException.class, () -> service.placeOrder("buyer",
                new PlaceOrderRequest("rollback-later", "COD", List.of(
                        new PlaceOrderRequest.Item(product.getId(), 1),
                        new PlaceOrderRequest.Item(Long.MAX_VALUE, 1)))));
        assertEquals(3, products.findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, orders.count());
    }

    @Test
    void orderSaveFailureRollsBackStockAndAllowsRetry() throws Exception {
        doThrow(new IllegalStateException("Simulated order persistence failure")).when(orders).saveAndFlush(any());
        assertThrows(IllegalStateException.class, () -> service.placeOrder("buyer", request("save-failure", 1)));
        assertEquals(3, products.findById(product.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, orders.count());
        reset(orders);
        service.placeOrder("buyer", request("save-failure", 1));
        assertEquals(2, products.findById(product.getId()).orElseThrow().getStockQuantity());
    }

    @Test
    void concurrentBuyersCannotBuyTheSameLastItem() throws Exception {
        product.setStockQuantity(1);
        products.saveAndFlush(product);
        List<Object> results = concurrently(
                () -> service.placeOrder("buyer", request("buyer-last-item", 1)),
                () -> service.placeOrder("other", request("other-last-item", 1)));
        assertEquals(1, results.stream().filter(CheckoutDTO.class::isInstance).count());
        assertEquals(1, results.stream().filter(ConflictException.class::isInstance).count());
        assertEquals(1, orders.count());
        assertEquals(0, products.findById(product.getId()).orElseThrow().getStockQuantity());
    }

    @Test
    void concurrentRetriesCreateOneOrderAndDeductOnce() throws Exception {
        List<Object> results = concurrently(
                () -> service.placeOrder("buyer", request("concurrent-retry", 1)),
                () -> service.placeOrder("buyer", request("concurrent-retry", 1)));
        assertTrue(results.stream().allMatch(CheckoutDTO.class::isInstance), results.toString());
        assertEquals(((CheckoutDTO) results.get(0)).getId(), ((CheckoutDTO) results.get(1)).getId());
        assertEquals(1, orders.count());
        assertEquals(2, products.findById(product.getId()).orElseThrow().getStockQuantity());
    }

    @Test
    void idempotencyKeysAreScopedToBuyerAndIncludePaymentMode() throws Exception {
        service.placeOrder("buyer", request("shared-request-key", 1));
        service.placeOrder("other", request("shared-request-key", 1));
        assertEquals(2, orders.count());
        assertThrows(ConflictException.class, () -> service.placeOrder("buyer",
                new PlaceOrderRequest("shared-request-key", "Bank Transfer", request("unused-key", 1).products())));
        assertEquals(1, products.findById(product.getId()).orElseThrow().getStockQuantity());
    }

    @Test
    void decimalOrderSnapshotsSurviveCatalogueChanges() throws Exception {
        product.setBasePrice(19.99);
        product.setDiscount(15.0);
        products.saveAndFlush(product);
        CheckoutDTO order = service.placeOrder("buyer", request("snapshot-request", 2));
        assertEquals(new BigDecimal("16.99"), order.getProducts().get(0).getPrice());
        assertEquals(new BigDecimal("33.98"), order.getProducts().get(0).getTotal());
        product.setName("Renamed item");
        product.setBasePrice(100.0);
        products.saveAndFlush(product);
        CheckoutDTO stored = service.findById(order.getId());
        assertEquals("Server priced item", stored.getProducts().get(0).getName());
        assertEquals(new BigDecimal("16.99"), stored.getProducts().get(0).getPrice());
    }

    @Test
    @WithMockUser(username = "buyer", roles = "BUYER")
    void malformedOrdersReturn400BeforeTouchingInventory() throws Exception {
        for (String body : List.of(
                "{\"paymentMode\":\"COD\",\"products\":[]}",
                "{\"idempotencyKey\":\"valid-request\",\"paymentMode\":\"Fake\",\"products\":[]}",
                "{\"idempotencyKey\":\"valid-request\",\"paymentMode\":\"COD\",\"products\":[null]}",
                "{\"idempotencyKey\":\"valid-request\",\"paymentMode\":\"COD\",\"products\":[{\"productId\":1,\"qty\":0}]}")) {
            mvc.perform(post("/checkout").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, orders.count());
        assertEquals(3, products.findById(product.getId()).orElseThrow().getStockQuantity());
    }

    @Test
    void unauthenticatedOrderReturns401() throws Exception {
        mvc.perform(post("/checkout").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private List<Object> concurrently(Callable<CheckoutDTO> first, Callable<CheckoutDTO> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<CheckoutDTO> task : List.of(first, second)) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                    try { return task.call(); } catch (Exception ex) { return ex; }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }
}
