import React, { useEffect, useState } from "react";
import HeroSection from "../Components/HeroSection";
import Products from "../Components/Products";

import { getProduct } from "../Redux/ActionCreators/ProductActionCreators";
import {
  createCart,
  getCart,
  updateCart,
} from "../Redux/ActionCreators/CartActionCreators";
import {
  createWishlist,
  getWishlist,
} from "../Redux/ActionCreators/WishlistActionCreators";

import { useNavigate, useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { formatCurrency } from "../config/siteConfig";
import { isLoggedIn } from "../config/auth";

export default function SingleProductPage() {
  let [qty, setQty] = useState(1);
  let [data, setData] = useState({ pics: [] });
  let [relatedProducts, setRelatedProducts] = useState([]);

  let { id } = useParams();

  let navigate = useNavigate();

  let dispatch = useDispatch();
  let ProductStateData = useSelector((state) => state.ProductStateData);
  let CartStateData = useSelector((state) => state.CartStateData);
  let WishlistStateData = useSelector((state) => state.WishlistStateData);

  console.log("CartStateData =", CartStateData);
  console.log("WishlistStateData =", WishlistStateData);
  // function addToCart() {
  //   let item = CartStateData.find(
  //     (x) => x.user === localStorage.getItem("userid") && x.product === id,
  //   );
  //   if (!item) {
  //     item = {
  //       user: localStorage.getItem("userid"),
  //       product: id,
  //       name: data.name, //Not Used in Case of Real Backend
  //       brand: data.brand, //Not Used in Case of Real Backend
  //       color: data.color, //Not Used in Case of Real Backend
  //       size: data.size, //Not Used in Case of Real Backend
  //       price: data.finalPrice, //Not Used in Case of Real Backend
  //       stockQuantity: data.stockQuantity, //Not Used in Case of Real Backend,
  //       pic: data.pics[0], //Not Used in Case of Real Backend,
  //       qty: qty,
  //       total: qty * data.finalPrice,
  //     };
  //     dispatch(createCart(item));
  //   }
  //   navigate("/cart");
  // }
  function addToCart() {
    if (!isLoggedIn()) {
      navigate("/login");
      return;
    }

    const cartList = Array.isArray(CartStateData) ? CartStateData : [];

    let item = cartList.find((x) => String(x.product) === String(data.name));

    if (!item) {
      item = {
        product: data.name,
        name: data.name,
        brand: data.brand,
        color: data.color,
        size: data.size,
        price: data.finalPrice,
        stockQuantity: data.stockQuantity,
        pic: data.pics?.[0] || "",
        qty: qty,
        total: Number(qty) * Number(data.finalPrice),
      };

      dispatch(createCart(item));
    } else {
      const newQty = Number(item.qty) + Number(qty);

      if (newQty > Number(data.stockQuantity)) {
        alert(`Only ${data.stockQuantity} items are available in stock`);
        return;
      }

      const updatedItem = {
        ...item,
        qty: newQty,
        total: newQty * Number(item.price || data.finalPrice),
      };

      dispatch(updateCart(updatedItem));
    }

    navigate("/cart");
  }

  function addToWishlist() {
    if (!isLoggedIn()) {
      navigate("/login");
      return;
    }

    const wishlistList = Array.isArray(WishlistStateData)
      ? WishlistStateData
      : [];

    let item = wishlistList.find(
      (x) => String(x.product) === String(data.name),
    );

    if (!item) {
      item = {
        product: data.name,
        name: data.name,
        brand: data.brand,
        color: data.color,
        size: data.size,
        price: data.finalPrice,
        stockQuantity: data.stockQuantity,
        pic: data.pics?.[0] || "",
      };
      dispatch(createWishlist(item));
    } else {
      alert("This product is already in your wishlist");
    }

    navigate("/profile");
  }

  useEffect(() => {
    dispatch(getProduct());
  }, [dispatch]);

  useEffect(() => {
    if (ProductStateData.length) {
      let item = ProductStateData.find((x) => String(x.id) === String(id));

      if (item) {
        let safeItem = {
          ...item,
          pics: Array.isArray(item.pics) ? item.pics : [],
        };

        setData(safeItem);
        setRelatedProducts(
          ProductStateData.filter(
            (x) =>
              x.active &&
              x.maincategory === item.maincategory &&
              String(x.id) !== String(id),
          ).slice(0, 12),
        );
      }
    }
  }, [ProductStateData, id]);

  useEffect(() => {
    (() => {
      dispatch(getCart());
    })();
  }, [dispatch]);

  useEffect(() => {
    (() => {
      dispatch(getWishlist());
    })();
  }, [dispatch]);
  return (
    <>
      <HeroSection title="Product" />

      <div className="container my-3">
        <div className="row">
          <div className="col-md-6">
            <div id="carouselExampleIndicators" className="carousel slide">
              <div className="carousel-indicators">
                <button
                  type="button"
                  data-bs-target="#carouselExampleIndicators"
                  data-bs-slide-to="0"
                  className="active"
                  aria-current="true"
                  aria-label="Slide 1"
                ></button>
                {data.pics?.slice(1).map((item, index) => {
                  return (
                    <button
                      key={index}
                      type="button"
                      data-bs-target="#carouselExampleIndicators"
                      data-bs-slide-to={index + 1}
                      aria-label={`Slide ${index + 2}`}
                    ></button>
                  );
                })}
              </div>
              <div className="carousel-inner">
                <div className="carousel-item active">
                  {data.pics?.[0] ? (
                    <img
                      src={`${process.env.REACT_APP_SERVER}${data.pics[0]}`}
                      height={450}
                      className="d-block w-100"
                      alt={data.name}
                    />
                  ) : (
                    <div
                      style={{
                        height: 450,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        background: "#f5f5f5",
                      }}
                    >
                      No Image
                    </div>
                  )}{" "}
                </div>
                {data.pics?.slice(1).map((item, index) => {
                  return (
                    <div className="carousel-item" key={index}>
                      <img
                        src={`${process.env.REACT_APP_SERVER}${item}`}
                        height={450}
                        className="d-block w-100"
                        alt="..."
                      />
                    </div>
                  );
                })}
              </div>
              <button
                className="carousel-control-prev"
                type="button"
                data-bs-target="#carouselExampleIndicators"
                data-bs-slide="prev"
              >
                <span
                  className="carousel-control-prev-icon"
                  aria-hidden="true"
                ></span>
                <span className="visually-hidden">Previous</span>
              </button>
              <button
                className="carousel-control-next"
                type="button"
                data-bs-target="#carouselExampleIndicators"
                data-bs-slide="next"
              >
                <span
                  className="carousel-control-next-icon"
                  aria-hidden="true"
                ></span>
                <span className="visually-hidden">Next</span>
              </button>
            </div>
            <div
              className={`d-flex my-3 ${(data.pics?.length || 0) < 4 ? "" : "justify-content-between"}`}
            >
              {data.pics?.map((item, index) => {
                return (
                  <img
                    key={index}
                    src={`${process.env.REACT_APP_SERVER}${item}`}
                    height={100}
                    width={100}
                    className="me-1"
                    data-bs-target="#carouselExampleIndicators"
                    data-bs-slide-to={index}
                    aria-label={`Slide ${index + 1}`}
                  />
                );
              })}
            </div>
          </div>
          <div className="col-md-6">
            <h5 className="bg-primary text-center p-2 text-light">
              {data.name}
            </h5>
            <table className="table table-bordered table-hover table-striped">
              <tbody>
                <tr>
                  <th>Maincategory/Subcategory/Brand</th>
                  <td>
                    {data.maincategory}/{data.subcategory}/{data.brand}
                  </td>
                </tr>
                <tr>
                  <th>Color/Size</th>
                  <td>
                    {data.color}/{data.size}
                  </td>
                </tr>
                <tr>
                  <th>Price</th>
                  <td>
                    <del className="text-danger">
                      {formatCurrency(data.basePrice)}
                    </del>{" "}
                    {formatCurrency(data.finalPrice)}
                    <sup>{data.discount}% Off</sup>
                  </td>
                </tr>
                <tr>
                  <th>Stock</th>
                  <td>
                    {data.stock
                      ? `Yes(${data.stockQuantity} Left In Stock) `
                      : "No"}
                  </td>
                </tr>
                <tr>
                  <td colSpan={2}>
                    <div className="row">
                      <div className="col-md-4 col-4 m-auto mb-3">
                        <div className="btn-group w-100">
                          <button
                            className="btn btn-primary"
                            onClick={() => (qty > 1 ? setQty(qty - 1) : null)}
                            aria-label="Decrease quantity"
                          >
                            <i className="fa fa-minus"></i>
                          </button>
                          <h4 className="w-75 text-center">{qty}</h4>
                          <button
                            className="btn btn-primary"
                            onClick={() =>
                              qty < data.stockQuantity ? setQty(qty + 1) : null
                            }
                            aria-label="Increase quantity"
                          >
                            <i className="fa fa-plus"></i>
                          </button>
                        </div>
                      </div>
                      <div className="col-md-8">
                        <div className="btn-group w-100">
                          <button
                            className="btn btn-primary w-100"
                            onClick={addToCart}
                          >
                            <i className="fa fa-shopping-cart"></i> Add to Cart
                          </button>
                          <button
                            className="btn btn-secondary w-100"
                            onClick={addToWishlist}
                          >
                            <i className="fa fa-heart"></i> Add to Wishlist
                          </button>
                        </div>
                      </div>
                    </div>
                  </td>
                </tr>
                <tr>
                  <th>Description</th>
                  <td>
                    <div
                      className="description"
                      dangerouslySetInnerHTML={{ __html: data.description }}
                    />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <Products title={`Other ${data.maincategory}`} data={relatedProducts} />
      </div>
    </>
  );
}
