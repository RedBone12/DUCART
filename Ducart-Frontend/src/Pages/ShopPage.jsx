import React, { useEffect, useState } from "react";

import HeroSection from "../Components/HeroSection";
import Products from "../Components/Products";

import { getProduct } from "../Redux/ActionCreators/ProductActionCreators";
import { getMaincategory } from "../Redux/ActionCreators/MaincategoryActionCreators";
import { getSubcategory } from "../Redux/ActionCreators/SubcategoryActionCreators";
import { getBrand } from "../Redux/ActionCreators/BrandActionCreators";

import { useDispatch, useSelector } from "react-redux";
import { Link, useLocation } from "react-router-dom";

export default function ShopPage() {
  const [products, setProducts] = useState([]);
  const [maincategory, setMaincategory] = useState([]);
  const [subcategory, setSubcategory] = useState([]);
  const [brand, setBrand] = useState([]);

  const [mc, setMc] = useState("All");
  const [sc, setSc] = useState("All");
  const [br, setBr] = useState("All");

  const [search, setSearch] = useState("");
  const [min, setMin] = useState(0);
  const [max, setMax] = useState(1200);

  const dispatch = useDispatch();
  const searchQuery = useLocation().search;

  const ProductStateData = useSelector((state) => state.ProductStateData);
  const MaincategoryStateData = useSelector(
    (state) => state.MaincategoryStateData,
  );
  const SubcategoryStateData = useSelector(
    (state) => state.SubcategoryStateData,
  );
  const BrandStateData = useSelector((state) => state.BrandStateData);

  const safeValue = (value) => (value && value.trim() ? value : "All");
  const shopUrl = (nextMc = "All", nextSc = "All", nextBr = "All") =>
    `/shop?mc=${encodeURIComponent(nextMc)}&sc=${encodeURIComponent(nextSc)}&br=${encodeURIComponent(nextBr)}`;

  function matchesSearch(item, keyword) {
    const q = keyword.toLocaleLowerCase();
    return (
      item.name?.toLocaleLowerCase().includes(q) ||
      item.maincategory?.toLocaleLowerCase().includes(q) ||
      item.subcategory?.toLocaleLowerCase().includes(q) ||
      item.brand?.toLocaleLowerCase().includes(q) ||
      item.color?.toLocaleLowerCase().includes(q) ||
      item.description?.toLocaleLowerCase().includes(q)
    );
  }

  function applyFilter(nextMc, nextSc, nextBr, minAmount = -1, maxAmount = -1) {
    let data = ProductStateData.filter((item) => item.active);

    if (nextMc !== "All") {
      data = data.filter((item) => item.maincategory === nextMc);
    }

    if (nextSc !== "All") {
      data = data.filter((item) => item.subcategory === nextSc);
    }

    if (nextBr !== "All") {
      data = data.filter((item) => item.brand === nextBr);
    }

    if (minAmount !== -1 && maxAmount !== -1) {
      const minValue = Number(minAmount);
      const maxValue = Number(maxAmount);
      data = data.filter(
        (item) => item.finalPrice >= minValue && item.finalPrice <= maxValue,
      );
    }

    setProducts(data);
  }

  function postSearch(e) {
    e.preventDefault();

    if (!search.trim()) {
      applyFilter(mc, sc, br);
      return;
    }

    setProducts(
      ProductStateData.filter(
        (item) => item.active && matchesSearch(item, search),
      ),
    );
  }

  function postPriceFilter(e) {
    e.preventDefault();

    if (search.trim()) {
      setProducts(
        ProductStateData.filter(
          (item) => item.active && matchesSearch(item, search),
        ).filter(
          (item) =>
            item.finalPrice >= Number(min) && item.finalPrice <= Number(max),
        ),
      );
    } else {
      applyFilter(mc, sc, br, min, max);
    }
  }

  function applySortFilter(option) {
    let sortedProducts = [...products];

    if (option === "1") {
      sortedProducts.sort((x, y) => Number(y.id) - Number(x.id));
    } else if (option === "2") {
      sortedProducts.sort(
        (x, y) => Number(y.finalPrice) - Number(x.finalPrice),
      );
    } else {
      sortedProducts.sort(
        (x, y) => Number(x.finalPrice) - Number(y.finalPrice),
      );
    }

    setProducts(sortedProducts);
  }

  useEffect(() => {
    dispatch(getProduct());
    dispatch(getMaincategory());
    dispatch(getSubcategory());
    dispatch(getBrand());
  }, [dispatch]);

  useEffect(() => {
    const query = new URLSearchParams(searchQuery);
    const nextMc = safeValue(query.get("mc"));
    const nextSc = safeValue(query.get("sc"));
    const nextBr = safeValue(query.get("br"));

    setMc(nextMc);
    setSc(nextSc);
    setBr(nextBr);
    setSearch("");
    applyFilter(nextMc, nextSc, nextBr);
  }, [ProductStateData, searchQuery]);

  useEffect(() => {
    setMaincategory(MaincategoryStateData.filter((item) => item.active));
  }, [MaincategoryStateData]);

  useEffect(() => {
    setSubcategory(SubcategoryStateData.filter((item) => item.active));
  }, [SubcategoryStateData]);

  useEffect(() => {
    setBrand(BrandStateData.filter((item) => item.active));
  }, [BrandStateData]);

  return (
    <>
      <HeroSection title="Shop" />

      <div className="container-fluid my-3">
        <div className="row">
          <div className="col-md-2">
            <div className="list-group mb-3">
              <Link
                to={shopUrl("All", "All", "All")}
                className="list-group-item list-group-item-action active"
                aria-current="true"
              >
                Maincategory
              </Link>
              <Link
                to={shopUrl("All", "All", "All")}
                className="list-group-item list-group-item-action"
              >
                All
              </Link>
              {maincategory?.map((item) => (
                <Link
                  key={item.id}
                  to={shopUrl(item.name, "All", "All")}
                  className="list-group-item list-group-item-action"
                >
                  {item.name}
                </Link>
              ))}
            </div>

            <div className="list-group mb-3">
              <Link
                to={shopUrl("All", "All", "All")}
                className="list-group-item list-group-item-action active"
                aria-current="true"
              >
                Subcategory
              </Link>
              <Link
                to={shopUrl("All", "All", "All")}
                className="list-group-item list-group-item-action"
              >
                All
              </Link>
              {subcategory?.map((item) => (
                <Link
                  key={item.id}
                  to={shopUrl("All", item.name, "All")}
                  className="list-group-item list-group-item-action"
                >
                  {item.name}
                </Link>
              ))}
            </div>

            <div className="list-group mb-3">
              <Link
                to={shopUrl("All", "All", "All")}
                className="list-group-item list-group-item-action active"
                aria-current="true"
              >
                Brand
              </Link>
              <Link
                to={shopUrl("All", "All", "All")}
                className="list-group-item list-group-item-action"
              >
                All
              </Link>
              {brand?.map((item) => (
                <Link
                  key={item.id}
                  to={shopUrl("All", "All", item.name)}
                  className="list-group-item list-group-item-action"
                >
                  {item.name}
                </Link>
              ))}
            </div>

            <div className="mb-3">
              <h5 className="bg-primary text-center p-2 text-light">
                Price Range
              </h5>
              <form onSubmit={postPriceFilter}>
                <div className="row">
                  <div className="col-md-6 mb-3">
                    <label>Min Amount</label>
                    <input
                      type="number"
                      name="min"
                      onChange={(e) => setMin(e.target.value)}
                      className="form-control border-3 border-primary"
                      value={min}
                      placeholder="Min Amount"
                    />
                  </div>
                  <div className="col-md-6 mb-3">
                    <label>Max Amount</label>
                    <input
                      type="number"
                      name="max"
                      onChange={(e) => setMax(e.target.value)}
                      className="form-control border-3 border-primary"
                      value={max}
                      placeholder="Max Amount"
                    />
                  </div>
                </div>
                <div className="mb-3">
                  <button type="submit" className="btn btn-primary w-100">
                    Apply Filter
                  </button>
                </div>
              </form>
            </div>
          </div>

          <div className="col-md-10">
            <div className="row">
              <div className="col-md-9">
                <form onSubmit={postSearch}>
                  <div className="btn-group w-100">
                    <input
                      type="search"
                      name="search"
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                      placeholder="Search Products by Name, Maincategory, Subcategory, Brand, Color, Description..."
                      className="form-control border-3 border-primary"
                    />
                    <button
                      type="submit"
                      className="btn btn-primary px-4"
                      aria-label="Search products"
                    >
                      <i className="fa fa-search"></i>
                    </button>
                  </div>
                </form>
              </div>

              <div className="col-md-3">
                <select
                  name="sort"
                  onChange={(e) => applySortFilter(e.target.value)}
                  className="form-select border-3 border-primary"
                >
                  <option value="1">Latest</option>
                  <option value="2">Price: High to Low</option>
                  <option value="3">Price: Low to High</option>
                </select>
              </div>
            </div>

            {products.length > 0 ? (
              <Products data={products} title="Shop" />
            ) : (
              <div className="text-center py-5">
                <h4>No products found</h4>
                <p className="text-muted">
                  Try another category, brand, search term, or price range.
                </p>
                <Link
                  to={shopUrl("All", "All", "All")}
                  className="btn btn-primary"
                >
                  Show All Products
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
