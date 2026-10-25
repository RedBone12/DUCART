import React, { useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import HeroSection from "../Components/HeroSection";

export default function ForgotPassword() {
  const token = new URLSearchParams(useLocation().search).get("token");
  const [identity, setIdentity] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const lock = useRef(false);

  async function submit(event) {
    event.preventDefault();
    if (lock.current) return;
    setError("");
    setMessage("");
    if (!token && !identity.trim()) { setError("Username or email is required"); return; }
    if (token && (password.length < 12 || password.length > 72)) {
      setError("Password must be between 12 and 72 characters"); return;
    }
    if (token && password !== confirmation) { setError("Passwords do not match"); return; }
    lock.current = true;
    setSubmitting(true);
    try {
      const response = await fetch(
        process.env.REACT_APP_SERVER + (token ? "/user/reset-password" : "/user/forgot-password"), {
          method: "POST", headers: { "content-type": "application/json" },
          body: JSON.stringify(token ? { token, newPassword: password } : { usernameOrEmail: identity.trim() }),
        },
      );
      const result = await response.json();
      if (!response.ok) throw new Error(result.message || "Unable to process request. Please try again.");
      setMessage(result.message);
      if (token) { setCompleted(true); setPassword(""); setConfirmation(""); }
    } catch (failure) {
      setError(failure.message || "Unable to process request. Please try again.");
    } finally {
      lock.current = false;
      setSubmitting(false);
    }
  }

  return <>
    <HeroSection title="Reset Your Password" />
    <div className="container my-3"><div className="row"><div className="col-md-6 col-sm-8 m-auto">
      <h5 className="bg-primary text-light text-center p-2">{token ? "Choose a new password" : "Request a password reset link"}</h5>
      {!completed && <form onSubmit={submit}>
        {!token ? <div className="mb-3">
          <label htmlFor="reset-identity">Username or Email</label>
          <input id="reset-identity" className="form-control" autoComplete="username" maxLength={254}
            value={identity} onChange={(event) => setIdentity(event.target.value)} />
          <p>We will email a link to your registered address. The link expires in 15 minutes.</p>
        </div> : <>
          <div className="mb-3">
            <label htmlFor="reset-password">New Password</label>
            <input id="reset-password" className="form-control" type={showPassword ? "text" : "password"}
              autoComplete="new-password" maxLength={72} value={password} onChange={(event) => setPassword(event.target.value)} />
            <button type="button" className="btn btn-secondary" aria-label="Toggle password visibility"
              onClick={() => setShowPassword(!showPassword)}>{showPassword ? "Hide" : "Show"}</button>
            <p>Use at least 12 characters. Maximum 72 UTF-8 bytes.</p>
          </div>
          <div className="mb-3">
            <label htmlFor="reset-confirmation">Confirm Password</label>
            <input id="reset-confirmation" className="form-control" type={showPassword ? "text" : "password"}
              autoComplete="new-password" maxLength={72} value={confirmation} onChange={(event) => setConfirmation(event.target.value)} />
          </div>
        </>}
        <button type="submit" className="btn btn-primary w-100" disabled={submitting}>
          {submitting ? "Please wait..." : token ? "Reset Password" : "Send Reset Link"}
        </button>
      </form>}
      {error && <p role="alert" className="text-danger">{error}</p>}
      {message && <p role="status" className="text-success">{message}</p>}
      <div className="d-flex justify-content-between mt-3">
        <Link to="/login">Back to Login</Link>
        {token ? <Link to="/forgot-password">Request a new link</Link> : <Link to="/signup">Create an Account</Link>}
      </div>
    </div></div></div>
  </>;
}
