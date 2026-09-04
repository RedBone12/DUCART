import React, { useState } from "react";
import HeroSection from "../Components/HeroSection";
import { Link, useNavigate } from "react-router-dom";

export default function ForgotPassword() {
  const [data, setData] = useState({
    usernameOrEmail: "",
    phone: "",
    newPassword: "",
    confirmPassword: "",
  });

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const navigate = useNavigate();

  function getInputData(e) {
    const { name, value } = e.target;

    setData((old) => {
      return {
        ...old,
        [name]: value,
      };
    });

    setError("");
    setMessage("");
  }

  async function resetPassword(e) {
    e.preventDefault();

    if (
      !data.usernameOrEmail.trim() ||
      !data.phone.trim() ||
      !data.newPassword ||
      !data.confirmPassword
    ) {
      setError("All fields are required");
      return;
    }

    if (data.newPassword.length < 6) {
      setError("Password must be at least 6 characters");
      return;
    }

    if (data.newPassword !== data.confirmPassword) {
      setError("New password and confirm password do not match");
      return;
    }

    try {
      let response = await fetch(
        `${process.env.REACT_APP_SERVER}/user/forgot-password`,
        {
          method: "PUT",
          headers: {
            "content-type": "application/json",
          },
          body: JSON.stringify({
            usernameOrEmail: data.usernameOrEmail,
            phone: data.phone,
            newPassword: data.newPassword,
          }),
        }
      );

      if (response.ok) {
        setMessage("Password reset successfully. Redirecting to login...");
        setTimeout(() => {
          navigate("/login");
        }, 1500);
      } else {
        setError("Account details do not match. Please check and try again.");
      }
    } catch (err) {
      setError("Server error. Please try again later.");
    }
  }

  return (
    <>
      <HeroSection title="Forgot Password - Reset Your Account" />

      <div className="container my-3">
        <div className="row">
          <div className="col-md-6 col-sm-8 m-auto">
            <h5 className="bg-primary text-light text-center p-2">
              Reset Your Password
            </h5>

            <form onSubmit={resetPassword}>
              <div className="mb-3">
                <label>Username or Email*</label>
                <input
                  type="text"
                  name="usernameOrEmail"
                  value={data.usernameOrEmail}
                  onChange={getInputData}
                  className="form-control border-3 border-primary"
                  placeholder="Enter username or email"
                />
              </div>

              <div className="mb-3">
                <label>Phone Number*</label>
                <input
                  type="text"
                  name="phone"
                  value={data.phone}
                  onChange={getInputData}
                  className="form-control border-3 border-primary"
                  placeholder="Enter registered phone number"
                />
              </div>

              <div className="mb-3">
                <label>New Password*</label>
                <div className="btn-group d-flex">
                  <input
                    type={showPassword ? "text" : "password"}
                    name="newPassword"
                    value={data.newPassword}
                    onChange={getInputData}
                    className="form-control border-3 border-primary"
                    placeholder="New password"
                  />
                  <button
                    type="button"
                    className="btn btn-primary"
                    aria-label="Toggle password visibility"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    <i
                      className={`fa ${
                        showPassword ? "fa-eye-slash" : "fa-eye"
                      }`}
                    ></i>
                  </button>
                </div>
              </div>

              <div className="mb-3">
                <label>Confirm Password*</label>
                <input
                  type={showPassword ? "text" : "password"}
                  name="confirmPassword"
                  value={data.confirmPassword}
                  onChange={getInputData}
                  className="form-control border-3 border-primary"
                  placeholder="Confirm password"
                />
              </div>

              {error ? <p className="text-danger">{error}</p> : null}
              {message ? <p className="text-success">{message}</p> : null}

              <div className="mb-3">
                <button type="submit" className="btn btn-primary w-100">
                  Reset Password
                </button>
              </div>
            </form>

            <div className="d-flex justify-content-between">
              <Link to="/login">Back to Login</Link>
              <Link to="/signup">Create an Account</Link>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
