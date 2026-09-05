START TRANSACTION;

INSERT INTO product (
    name,
    maincategory,
    subcategory,
    brand,
    color,
    size,
    base_price,
    discount,
    final_price,
    stock,
    description,
    stock_quantity,
    active
)
SELECT
    'Nike Air Force 1 Sneakers',
    'Shoes',
    'Sneakers',
    'Nike',
    'White',
    '42',
    99.99,
    10.00,
    89.99,
    1,
    'Classic white Nike sneakers for everyday wear.',
    20,
    1
WHERE NOT EXISTS (
    SELECT 1
    FROM product
    WHERE name = 'Nike Air Force 1 Sneakers'
);

INSERT INTO product_pics (product_id, pics)
SELECT
    id,
    '/uploads/products/nike_air_force_1_sneakers.jpg'
FROM product
WHERE name = 'Nike Air Force 1 Sneakers'
  AND NOT EXISTS (
      SELECT 1
      FROM product_pics
      WHERE product_id = product.id
        AND pics = '/uploads/products/nike_air_force_1_sneakers.jpg'
  );

COMMIT;