import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authJsonHeaders } from "../config/auth";

function getProfileImage(pic) {
  if (!pic) return "/img/noimage.png";

  if (pic.startsWith("http")) {
    return pic;
  }

  if (pic.startsWith("/uploads/")) {
    return `${process.env.REACT_APP_SERVER}${pic}`;
  }

  return `${process.env.REACT_APP_SERVER}/uploads/users/${pic}`;
}
export default function Profile(props) {
  const [user, setUser] = useState({});
  const navigate = useNavigate();

  useEffect(() => {
    async function fetchUser() {
      try {
        const response = await fetch(
          `${process.env.REACT_APP_SERVER}/user/${localStorage.getItem("userid")}`,
          {
            method: "GET",
            headers: authJsonHeaders(),
          },
        );

        const result = await response.json();

        if (!response.ok || !result) {
          navigate("/login");
          return;
        }

        setUser(result);
      } catch (error) {
        console.error("Failed to fetch profile:", error);
      }
    }

    fetchUser();
  }, [navigate]);

  return (
    <div className="row">
      <div
        className={`col-md-6 ${
          props.title === "Checkout" ? "d-none" : "d-block"
        }`}
      >
        {user.pic ? (
          <img
            src={getProfileImage(user.pic)}
            height={props.title === "Admin Profile" ? 265 : 430}
            width="100%"
            alt="Profile"
            onError={(e) => {
              e.currentTarget.src = "/img/noimage.png";
            }}
          />
        ) : (
          <img
            src="/img/noimage.png"
            height={props.title === "Admin Profile" ? 265 : 430}
            width="100%"
            alt="No Profile"
          />
        )}
      </div>

      <div className={`${props.title === "Checkout" ? "col-12" : "col-md-6"}`}>
        <h5 className="bg-primary text-center p-2 text-light">{props.title}</h5>
        <table className="table table-bordered table-striped table-hover">
          <tbody>
            <tr>
              <th>Name</th>
              <td>{user.name}</td>
            </tr>
            <tr>
              <th>User Name</th>
              <td>{user.username}</td>
            </tr>
            <tr>
              <th>Phone</th>
              <td>{user.phone}</td>
            </tr>
            <tr>
              <th>Email</th>
              <td>{user.email}</td>
            </tr>

            {props.title !== "Admin Profile" ? (
              <>
                <tr>
                  <th>Address</th>
                  <td>{user.address}</td>
                </tr>
                <tr>
                  <th>Pin</th>
                  <td>{user.pin}</td>
                </tr>
                <tr>
                  <th>City</th>
                  <td>{user.city}</td>
                </tr>
                <tr>
                  <th>State</th>
                  <td>{user.state}</td>
                </tr>
              </>
            ) : null}

            <tr>
              <td colSpan={2}>
                <Link to="/update-profile" className="btn btn-primary w-100">
                  Update Profile
                </Link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
