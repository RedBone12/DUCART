import React from 'react'

import HeroSection from "../Components/HeroSection"
import { Link, useLocation } from 'react-router-dom'
import { formatCurrency } from '../config/siteConfig'
export default function Confirmation() {
  const { state } = useLocation();
  const order = state?.order;
  return (
    <>
        <HeroSection title="Order Confirmation"/>

        <div className="container-fluid my-3 text-center">
            <h2>Thank You</h2>
            <h3>Your Order has Been Placed</h3>
            {order && <p>Order #{order.id} · Total {formatCurrency(order.total)} · Payment {order.paymentStatus}</p>}
            <h4>Now You Can Track You  Order in Profile Page</h4>
            <Link to="/shop" className='btn btn-primary'>Shop More</Link>
        </div>
    </>
  )
}
