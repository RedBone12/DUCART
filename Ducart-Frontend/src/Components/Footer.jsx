import React, { useState } from "react";
import { Link } from "react-router-dom";
import siteConfig from "../config/siteConfig";

export default function Footer() {
  let [email, setEmail] = useState("");
  let [show, setShow] = useState("");

  async function postData() {
    let response = await fetch(`${process.env.REACT_APP_SERVER}/newsletter`, {
      method: "GET",
      headers: {
        "content-type": "application/json",
      },
    });
    response = await response.json();

    if (response.find((x) => x.email === email))
      setShow("Your Email Address is Already Registered");
    else {
      response = await fetch(`${process.env.REACT_APP_SERVER}/newsletter`, {
        method: "POST",
        headers: {
          "content-type": "application/json",
        },
        body: JSON.stringify({ email: email, active: true }),
      });
      response = await response.json();
      setShow("Thanks To Subscribe Our Newsletter Service");
    }
  }
  return (
    <>
      <div
        className="container-fluid footer bg-dark text-light footer mt-5 pt-5 wow fadeIn"
        data-wow-delay="0.1s"
      >
        <div className="container py-5">
          <div className="row g-5">
            <div className="col-lg-4 col-md-6">
              <h5 className="text-light mb-4">Address</h5>

              <p className="mb-2">
                <i className="fa fa-map-marker me-3"></i>
                {siteConfig.addressShort}
              </p>

              <Link
                className="mb-2 text-light d-block"
                to={`tel:${siteConfig.phoneLink}`}
              >
                <i className="fa fa-phone me-3"></i>
                {siteConfig.phone}
              </Link>

              <Link
                className="mb-2 text-light d-block"
                to={`mailto:${siteConfig.email}`}
              >
                <i className="fa fa-envelope me-3"></i>
                {siteConfig.email}
              </Link>

              <div className="d-flex pt-2">
                <a
                  className="btn btn-outline-light btn-social"
                  href={siteConfig.github}
                  target="_blank"
                  rel="noreferrer"
                >
                  <i className="fab fa-github"></i>
                </a>

                <a
                  className="btn btn-outline-light btn-social"
                  href={siteConfig.linkedin}
                  target="_blank"
                  rel="noreferrer"
                >
                  <i className="fab fa-linkedin-in"></i>
                </a>
              </div>
             
            </div>
            <div className="col-lg-2 col-md-6">
              <h5 className="text-light mb-4">Quick Links</h5>
              <Link className="btn btn-link" to="/">
                Home
              </Link>
              <Link className="btn btn-link" to="/about">
                About Us
              </Link>
              <Link className="btn btn-link" to="/shop">
                Shop
              </Link>
              <Link className="btn btn-link" to="/features">
                Features
              </Link>
            </div>
            <div className="col-lg-2 col-md-6">
              <h5 className="text-light mb-4">Quick Links</h5>
              <Link className="btn btn-link" to="/testimonials">
                Testimonials
              </Link>
              <Link className="btn btn-link" to="/contactus">
                Contact Us
              </Link>
              <Link className="btn btn-link" to="/terms">
                Terms & Condition
              </Link>
              <Link className="btn btn-link" to="/refund-policy">
                Refund Policy
              </Link>
            </div>
            <div className="col-lg-4 col-md-6">
              <h5 className="text-light mb-4">Newsletter</h5>
              <p className="text-justify">
                Subscribe for product updates, seasonal offers, and project
                demo news.
              </p>
              {show ? <p>{show}</p> : null}
              <div
                className="position-relative mx-auto"
                style={{ maxWidth: "400px" }}
              >
                <input
                  name="email"
                  className="form-control border-0 w-100 py-3 ps-4 pe-5"
                  onChange={(e) => setEmail(e.target.value)}
                  type="email"
                  placeholder="Your email"
                />
                <button
                  type="button"
                  onClick={postData}
                  className="btn btn-primary py-2 position-absolute top-0 end-0 mt-2 me-2"
                >
                  Subscribe
                </button>
              </div>
            </div>
          </div>
        </div>
        <div className="container">
          <div className="copyright">
            <div className="row">
              <div className="col-md-6 text-center text-md-start mb-3 mb-md-0">
                &copy;{" "}
                <Link className="border-bottom" to="/">
                  {siteConfig.brandName}
                </Link>
                , All Rights Reserved.
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
