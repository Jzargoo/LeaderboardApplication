**User Endpoints:**

1. **Product Service**
    - `GET /products/*filters` Header: X-PARTIALLY-FILTERED List<Long>
    - `GET /products/{id}`
    - `POST /products`
    - `PUT /products/{id}`
    - `GET /categories`
    - `DELETE /products/{id}`
2. **Catalog Service** (service with 2 reading models: Elasticsearch (Id + name) for string full-search + MongoDB (id + category + rate))
    - `GET /catalog/daily?count=*` – Endpoint for recommended products, e.g., select a random category every day like “The Day of Electronics” and return products from this category.
    - `GET /catalog?q={query}&filters={filterDto}`
3. **Cart Service**
    - `POST /cart`
    - `PUT /add/product/{cartId}`
    - `GET /cart/{cartId}`
    - `DELETE /cart/{cartId}`
    - `DELETE /cart/{productId}/{cartId}`
4. **Pricing Service**
    - `GET /pricing/{productId}`
    - `POST /pricing/apply-promo`
    - `POST /pricing/update` (internal)
5. **Inventory Service**
    - `GET /inventory/{productId}`
6. **Order Service**
    - `GET /orders/{id}`
    - `GET /orders/user/{userId}`
    - `PUT /orders/{id}/status`
7. **User Service**
    - `GET /users/{id}` – get user information
    - `PUT /users` – update information not related to Keycloak, e.g., icon
8. **Notification Service**
    - `GET /notifications/user/{userId}` – get user notifications
9. **Delivery Service**
    - `GET /delivery/{orderId}` – get delivery status
    - `POST /delivery/update` – update delivery status
10. **Reaction Service**
    - `POST /reactions` – add review and rating
    - `GET /reactions/product/{productId}` – get product reviews
    - `GET /reactions/user/{userId}` – get user reviews
    - `PUT /reactions/{id}` – update review/rating
11. **Statistical / Analytics Service**
    - `GET /analytics/product/{productId}` – product statistics (views, purchases, add-to-cart) only for products by shops
    - `GET /analytics/user/{userId}` – user behavior
12. **Shop Service**
- `GET /shops/{shopId}` – get shop information
- `PUT /shops/{shopId}` – update shop information
- `DELETE /shops/{shopId}` – delete shop and its products

Asynch paths (connections between kafka handlers):

Place order (orchestration):

Cart service → product service (check existing, in-variants) → inventory service (check availability) → payment service → order service → {notification service; statistical analysis}  → delivery service → notification service

Create a product (orchestration):

product service → inventory service → shop service → notification service

Update a product (choreography):

product service → {catalog service; pricing service; statistical analysis}

availability of a product (event):

inventory service →  catalog service

Add to catalog(event):

reaction service → catalog service