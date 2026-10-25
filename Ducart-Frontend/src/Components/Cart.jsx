import React, { useCallback, useEffect, useRef, useState } from "react";

import { useDispatch, useSelector } from "react-redux";
import { Link, useNavigate } from "react-router-dom";

import {
  getCart,
  updateCart,
  deleteCart,
} from "../Redux/ActionCreators/CartActionCreators";
import { submitOrder, pendingOrder, clearPendingOrder } from "../config/checkout";
import { getProduct } from "../Redux/ActionCreators/ProductActionCreators";
import siteConfig, { formatCurrency } from "../config/siteConfig";

//cart component
export default function Cart(props) {
  let [cart, setCart] = useState([]);
  let [subtotal, setSubtotal] = useState(0);
  let [shipping, setShipping] = useState(0);
  let [total, setTotal] = useState(0);
  let [mode, setMode] = useState(siteConfig.paymentModes[0].value);

  let CartStateData = useSelector((state) => state.CartStateData);
  let ProductStateData = useSelector((state) => state.ProductStateData);
  let dispatch = useDispatch();

  let navigate = useNavigate();

  const [submitting, setSubmitting] = useState(false);
  const [orderError, setOrderError] = useState("");
  const submissionLock = useRef(false);

  async function placeOrder() {
    if (submissionLock.current) return;
    submissionLock.current = true;
    setSubmitting(true);
    setOrderError("");
    try {
      const products = cart.map((item) => {
        const productId = item.productId ?? ProductStateData.find((p) => p.name === item.product)?.id;
        if (!productId) throw new Error(item.product + " was not found in product list");
        return { productId, qty: Number(item.qty) };
      }).sort((a, b) => a.productId - b.productId);
      const order = await submitOrder(pendingOrder(mode, products));
      clearPendingOrder();
      cart.forEach((item) => dispatch(deleteCart({ id: item.id })));
      dispatch(getProduct());
      navigate("/confirmation", { state: { order } });
    } catch (error) {
      setOrderError(error.message || "Unable to place order. Please retry.");
    } finally {
      submissionLock.current = false;
      setSubmitting(false);
    }
  }

  function deleteRecord(id) {
    if (window.confirm("Are You Sure to Delete that Item : ")) {
      dispatch(deleteCart({ id: id }));
    }
  }

  function updateRecord(id, option) {
    const item = cart.find((x) => x.id === id);
    if (!item) return;

    const currentQty = Number(item.qty);
    const stockQuantity = Number(item.stockQuantity);

    if (
      (option === "DEC" && currentQty <= 1) ||
      (option === "INC" && currentQty >= stockQuantity)
    )
      return;

    const nextQty = option === "DEC" ? currentQty - 1 : currentQty + 1;
    const updatedItem = {
      ...item,
      qty: nextQty,
      total: nextQty * Number(item.price),
    };
    const updatedCart = cart.map((cartItem) =>
      cartItem.id === id ? updatedItem : cartItem,
    );

    dispatch(updateCart(updatedItem));
    setCart(updatedCart);
    calculation(updatedCart);
  }
  const calculation = useCallback((cart) => {
    const subtotal = cart.reduce(
      (sum, item) => sum + Number(item.total || 0),
      0,
    );
    const shipping =
      subtotal > 0 && subtotal < siteConfig.freeShippingThreshold
        ? siteConfig.shippingFee
        : 0;

    setSubtotal(subtotal);
    setShipping(shipping);
    setTotal(Number((subtotal + shipping).toFixed(2)));
  }, []);

  useEffect(() => {
    dispatch(getCart());
  }, [dispatch]);

  useEffect(() => {
    const nextCart = Array.isArray(CartStateData) ? CartStateData : [];
    setCart(nextCart);
    calculation(nextCart);
  }, [CartStateData, calculation]);

  useEffect(() => {
    dispatch(getProduct());
  }, [dispatch]);
  return (
    <>
      <h5 className="bg-primary p-2 text-center text-light">Cart Section</h5>
      {cart.length ? (
        <>
          <div className="table-responsive">
            <table className="table table-bordered table-striped table-hover">
              <thead>
                <tr>
                  <th></th>
                  <th>Name</th>
                  <th>Brand</th>
                  <th>Color</th>
                  <th>Size</th>
                  {props.title === "Checkout" ? null : <th>Stock Quantity</th>}
                  <th>Price</th>
                  <th>Quantity</th>
                  <th>Total</th>
                  {props.title === "Checkout" ? null : <th></th>}
                </tr>
              </thead>
              <tbody>
                {cart?.map((item) => {
                  return (
                    <tr key={item.id}>
                      <td>
                        <Link
                          to={`${process.env.REACT_APP_SERVER}${item.pic}`}
                          target="_blank"
                          rel="noreferrer"
                        >
                          <img
                            src={`${process.env.REACT_APP_SERVER}${item.pic}`}
                            height={50}
                            width={50}
                            className="rounded"
                            alt={item.name}
                          />
                        </Link>
                      </td>
                      <td>{item.name}</td>
                      <td>{item.brand}</td>
                      <td>{item.color}</td>
                      <td>{item.size}</td>
                      {props.title === "Checkout" ? null : (
                        <td>{item.stockQuantity} Left in Stock</td>
                      )}
                      <td>{formatCurrency(item.price)}</td>
                      <td>
                        <div className="btn-group w-100">
                          <button
                            className={`btn btn-primary ${
                              props.title === "Checkout" ? "d-none" : ""
                            }`}
                            onClick={() => updateRecord(item.id, "DEC")}
                            aria-label={`Decrease ${item.name} quantity`}
                          >
                            <i className="fa fa-minus"></i>
                          </button>
                          <h5 className="w-50 text-center mt-1">{item.qty}</h5>
                          <button
                            className={`btn btn-primary ${
                              props.title === "Checkout" ? "d-none" : ""
                            }`}
                            onClick={() => updateRecord(item.id, "INC")}
                            aria-label={`Increase ${item.name} quantity`}
                          >
                            <i className="fa fa-plus"></i>
                          </button>
                        </div>
                      </td>

                      <td>{formatCurrency(item.total)}</td>
                      {props.title === "Checkout" ? null : (
                        <td>
                          <button
                            className="btn btn-danger"
                            onClick={() => deleteRecord(item.id)}
                            aria-label={`Remove ${item.name} from cart`}
                          >
                            <i className="fa fa-trash"></i>
                          </button>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <div className="row">
            <div className="col-md-6"></div>
            <div
              className={`${
                props.title === "Checkout" ? "col-12" : "col-md-6"
              }`}
            >
              <table className="table table-striped table-hover table-bordered">
                <tbody>
                  <tr>
                    <th>Subtotal</th>
                    <td>{formatCurrency(subtotal)}</td>
                  </tr>
                  <tr>
                    <th>Shipping</th>
                    <td>{formatCurrency(shipping)}</td>
                  </tr>
                  <tr>
                    <th>Total</th>
                    <td>{formatCurrency(total)}</td>
                  </tr>
                  <tr>
                    <th>Payment Mode</th>
                    <td>
                      <select
                        name="mode"
                        value={mode}
                        onChange={(e) => setMode(e.target.value)}
                        className="form-control border-3 border-primary"
                      >
                        {siteConfig.paymentModes.map((paymentMode) => (
                          <option
                            key={paymentMode.value}
                            value={paymentMode.value}
                          >
                            {paymentMode.label}
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                  <tr>
                    {props.title === "Checkout" ? (
                      <td colSpan={2}>
                        {orderError && <p role="alert" className="text-danger">{orderError}</p>}
                        <p className="text-muted">Final prices and availability are confirmed by the server. Demo payments do not charge you.</p>
                        <button
                          onClick={placeOrder}
                          disabled={submitting}
                          className="btn btn-primary w-100"
                        >
                          {submitting ? "Placing Order..." : "Place Order"}
                        </button>
                      </td>
                    ) : (
                      <td colSpan={2}>
                        <Link to="/checkout" className="btn btn-primary w-100">
                          Checkout
                        </Link>
                      </td>
                    )}
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </>
      ) : (
        <div className="text-center card p-5">
          <h3>No Items in Cart</h3>
          <Link to="/shop" className="btn btn-primary w-25 m-auto">
            Shop Now
          </Link>
        </div>
      )}
    </>
  );
}
