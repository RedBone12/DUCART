import React from "react";
import { Navigate, Outlet, useLocation } from "react-router-dom";

import { getRole, isLoggedIn } from "../config/auth";

function roleHome(role) {
  if (role === "Admin") return "/admin";
  if (role === "Buyer") return "/profile";
  return "/";
}

export default function ProtectedRoute({ allowedRoles }) {
  const location = useLocation();

  if (!isLoggedIn()) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  const role = getRole();
  if (allowedRoles?.length && !allowedRoles.includes(role)) {
    return <Navigate to={roleHome(role)} replace />;
  }

  return <Outlet />;
}
