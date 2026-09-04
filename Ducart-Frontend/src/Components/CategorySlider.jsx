import React from "react";
import OwlCarousel from "react-owl-carousel";
import "owl.carousel/dist/assets/owl.carousel.css";
import "owl.carousel/dist/assets/owl.theme.default.css";

import { Link } from "react-router-dom";

export default function Category(props) {
  const serverUrl = process.env.REACT_APP_SERVER || "http://localhost:8080";

  let options = {
    loop: true,
    autoplay: true,
    autoplayTimeout: 3000,
    nav: true,
    navText: [
      "<button class='btn btn-primary slider-btn' id='slider-btn1'><i class='fa fa-arrow-left'></i></button>",
      "<button class='slider-btn btn btn-primary' id='slider-btn2'><i class='fa fa-arrow-right'></i></button>",
    ],
    responsive: {
      0: { items: 1 },
      576: { items: 2 },
      768: { items: 3 },
      1200: { items: 4 },
      4000: { items: 5 },
    },
  };

  function getShopUrl(itemName) {
    const value = encodeURIComponent(itemName);

    if (props.title === "Maincategory") {
      return `/shop?mc=${value}&sc=All&br=All`;
    }

    if (props.title === "Subcategory") {
      return `/shop?mc=All&sc=${value}&br=All`;
    }

    return `/shop?mc=All&sc=All&br=${value}`;
  }

  return (
    <>
      <div className="container-xxl py-2">
        <div className="container">
          <h1
            className="display-5 text-center mb-5 wow fadeInUp"
            data-wow-delay="0.1s"
          >
            Our {props.title}
          </h1>

          <div className="wow fadeInUp" data-wow-delay="0.1s">
            {props.data && props.data.length > 0 && (
              <OwlCarousel {...options}>
                {props.data.map((item) => {
                  const imagePath = `${serverUrl}${item.pic}`;

                  return (
                    <div
                      key={item.id}
                      className={props.title === "Brand" ? "mx-5" : "mx-2"}
                    >
                      <Link to={getShopUrl(item.name)}>
                        <img
                          src={imagePath}
                          onError={() => {
                            console.log("Image load failed:", imagePath);
                          }}
                          height={props.title === "Brand" ? 80 : 200}
                          width="100%"
                          style={{ objectFit: "cover" }}
                          alt={item.name}
                        />

                        <h2 className="text-center">{item.name}</h2>
                      </Link>
                    </div>
                  );
                })}
              </OwlCarousel>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
