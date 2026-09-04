import React from "react";
import { Link } from "react-router-dom";
import { formatCurrency } from "../config/siteConfig";

export default function Products(props) {
  const serverUrl = process.env.REACT_APP_SERVER || "http://localhost:8080";

  function getImageUrl(pic) {
    if (!pic) return "";
    if (pic.startsWith("http")) return pic;
    return `${serverUrl}${pic}`;
  }

  return (
    <>
      <div className="my-3">
        {props.title !== "Shop" ? (
          <div
            className="row g-5 mb-5 align-items-end wow fadeInUp"
            data-wow-delay="0.1s"
          >
            <div className="col-lg-6">
              <p>
                <span className="text-primary me-2">#</span>Our {props.title}{" "}
                Products
              </p>
              <h1 className="display-5 mb-0">
                Checkout Our <span className="text-primary">Ducart</span>{" "}
                {props.title} Products
              </h1>
            </div>
            <div className="col-lg-6 text-lg-end">
              <Link
                className="btn btn-primary py-3 px-5"
                to={`/shop?mc=${encodeURIComponent(props.title)}&sc=All&br=All`}
              >
                Explore More Products
              </Link>
            </div>
          </div>
        ) : null}

        <div className="row g-4">
          {props.data?.map((item) => {
            const firstPic =
              Array.isArray(item?.pics) && item.pics.length > 0
                ? item.pics[0]
                : null;

            return (
              <div className="col-lg-3 col-md-4 col-sm-6 mb-3" key={item.id}>
                <div className="card h-100">
                  {firstPic ? (
                    <img
                      src={getImageUrl(firstPic)}
                      style={{
                        height: 300,
                        width: "100%",
                        objectFit: "cover",
                        objectPosition: "center",
                      }}
                      className="card-img-top"
                      alt={item.name}
                    />
                  ) : (
                    <div
                      style={{
                        height: 300,
                        width: "100%",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        background: "#f5f5f5",
                      }}
                    >
                      No Image
                    </div>
                  )}

                  <div className="card-body d-flex flex-column">
                    <h6 className="card-title">{item.name}</h6>
                    <p className="card-text">
                      <del className="text-danger">
                        {formatCurrency(item.basePrice)}
                      </del>{" "}
                      {formatCurrency(item.finalPrice)}{" "}
                      <sup className="text-success">{item.discount}% Off</sup>
                    </p>
                    <Link
                      to={`/product/${item.id}`}
                      className="btn btn-primary w-100 mt-auto"
                    >
                      Add To Cart
                    </Link>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </>
  );
}
