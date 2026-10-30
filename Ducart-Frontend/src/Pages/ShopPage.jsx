import React, { useEffect, useState } from "react";
import HeroSection from "../Components/HeroSection";
import Products from "../Components/Products";
import { getMaincategory } from "../Redux/ActionCreators/MaincategoryActionCreators";
import { getSubcategory } from "../Redux/ActionCreators/SubcategoryActionCreators";
import { getBrand } from "../Redux/ActionCreators/BrandActionCreators";
import { useDispatch, useSelector } from "react-redux";
import { Link, useSearchParams } from "react-router-dom";

export default function ShopPage() {
  const [query, setQuery] = useSearchParams();
  const queryString = query.toString();
  const [search, setSearch] = useState(query.get("q") || "");
  const [min, setMin] = useState(query.get("min") || "");
  const [max, setMax] = useState(query.get("max") || "");
  const [result, setResult] = useState({ content: [], totalElements: 0, totalPages: 0, page: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const dispatch = useDispatch();
  const maincategory = useSelector(state => state.MaincategoryStateData).filter(item => item.active);
  const subcategory = useSelector(state => state.SubcategoryStateData).filter(item => item.active);
  const brand = useSelector(state => state.BrandStateData).filter(item => item.active);
  const products = result.content;

  function shopUrl(key, value) {
    const next = new URLSearchParams(query);
    next.delete("page");
    if (value === "All") next.delete(key); else next.set(key, value);
    return "/shop?" + next.toString();
  }
  function update(values) {
    setError("");
    const next = new URLSearchParams(query);
    next.delete("page");
    Object.entries(values).forEach(([key, value]) => {
      if (value === "") next.delete(key); else next.set(key, value);
    });
    setQuery(next);
  }
  function postSearch(event) {
    event.preventDefault(); update({ q: search.trim() });
  }
  function postPriceFilter(event) {
    event.preventDefault();
    if (min !== "" && max !== "" && Number(min) > Number(max)) {
      setError("Minimum price must not exceed maximum price"); return;
    }
    update({ min, max, q: search.trim() });
  }
  function goPage(page) {
    const next = new URLSearchParams(query);
    next.set("page", String(page)); setQuery(next);
  }
  useEffect(() => {
    dispatch(getMaincategory()); dispatch(getSubcategory()); dispatch(getBrand());
  }, [dispatch]);
  useEffect(() => {
    const current = new URLSearchParams(queryString);
    setSearch(current.get("q") || ""); setMin(current.get("min") || ""); setMax(current.get("max") || "");
    const controller = new AbortController();
    setLoading(true); setError("");
    fetch(process.env.REACT_APP_SERVER + "/product/search?" + queryString, { signal: controller.signal })
      .then(async response => {
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || "Unable to load products");
        return data;
      })
      .then(data => { if (!controller.signal.aborted) setResult(data); })
      .catch(failure => { if (!controller.signal.aborted) setError(failure.message); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [queryString, retry]);

  return (
    <>
      <HeroSection title="Shop" />

      <div className="container-fluid my-3">
        <div className="row">
          <div className="col-md-2">
            <div className="list-group mb-3">
              <div className="list-group-item bg-primary text-white">Maincategory</div>
              <Link
                to={shopUrl("mc", "All")}
                className="list-group-item list-group-item-action"
              >
                All
              </Link>
              {maincategory?.map((item) => (
                <Link
                  key={item.id}
                  to={shopUrl("mc", item.name)}
                  aria-current={query.get("mc") === item.name ? "true" : undefined}
                  className={"list-group-item list-group-item-action" + (query.get("mc") === item.name ? " active" : "")}
                >
                  {item.name}
                </Link>
              ))}
            </div>

            <div className="list-group mb-3">
              <div className="list-group-item bg-primary text-white">Subcategory</div>
              <Link
                to={shopUrl("sc", "All")}
                className="list-group-item list-group-item-action"
              >
                All
              </Link>
              {subcategory?.map((item) => (
                <Link
                  key={item.id}
                  to={shopUrl("sc", item.name)}
                  aria-current={query.get("sc") === item.name ? "true" : undefined}
                  className={"list-group-item list-group-item-action" + (query.get("sc") === item.name ? " active" : "")}
                >
                  {item.name}
                </Link>
              ))}
            </div>

            <div className="list-group mb-3">
              <div className="list-group-item bg-primary text-white">Brand</div>
              <Link
                to={shopUrl("br", "All")}
                className="list-group-item list-group-item-action"
              >
                All
              </Link>
              {brand?.map((item) => (
                <Link
                  key={item.id}
                  to={shopUrl("br", item.name)}
                  aria-current={query.get("br") === item.name ? "true" : undefined}
                  className={"list-group-item list-group-item-action" + (query.get("br") === item.name ? " active" : "")}
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
                    <label htmlFor="min-price">Min Amount</label>
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      name="min"
                      id="min-price"
                      onChange={(e) => setMin(e.target.value)}
                      className="form-control border-3 border-primary"
                      value={min}
                      placeholder="Min Amount"
                    />
                  </div>
                  <div className="col-md-6 mb-3">
                    <label htmlFor="max-price">Max Amount</label>
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      name="max"
                      id="max-price"
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
                      maxLength={200}
                      aria-label="Product keyword"
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
                  value={query.get("sort") || "latest"}
                  aria-label="Sort products"
                  onChange={(e) => update({ sort: e.target.value })}
                  className="form-select border-3 border-primary"
                >
                  <option value="latest">Latest</option>
                  <option value="priceDesc">Price: High to Low</option>
                  <option value="priceAsc">Price: Low to High</option>
                </select>
              </div>
            </div>

            {loading ? <p role="status">Loading products...</p> : error ? (
              <div role="alert">{error} <button onClick={() => setRetry(value => value + 1)}>Retry</button></div>
            ) : products.length > 0 ? (
              <Products data={products} title="Shop" />
            ) : (
              <div className="text-center py-5">
                <h4>No products found</h4>
                <p className="text-muted">
                  Try another category, brand, search term, or price range.
                </p>
                <Link
                  to={"/shop"}
                  className="btn btn-primary"
                >
                  Show All Products
                </Link>
              </div>
            )}
            {!loading && !error && <nav aria-label="Product pages" className="d-flex gap-3 align-items-center my-3">
              <button className="btn btn-outline-primary" disabled={result.page === 0} onClick={() => goPage(result.page - 1)}>Previous</button>
              <span>{result.totalElements} products · Page {result.page + 1} of {Math.max(1, result.totalPages)}</span>
              <button className="btn btn-outline-primary" disabled={result.page + 1 >= result.totalPages} onClick={() => goPage(result.page + 1)}>Next</button>
            </nav>}
          </div>
        </div>
      </div>
    </>
  );
}
