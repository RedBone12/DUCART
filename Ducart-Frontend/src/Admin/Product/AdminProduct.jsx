import React, { useEffect, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";

import $ from "jquery"; // Import jQuery
import "datatables.net-dt/css/dataTables.dataTables.min.css"; // Import DataTables styles
import "datatables.net";

import { formatCurrency } from "../../config/siteConfig";
import Sidebar from "../../Components/Sidebar";
import HeroSection from "../../Components/HeroSection";
import { Link } from "react-router-dom";

import {
  deleteProduct,
  getProduct,
} from "../../Redux/ActionCreators/ProductActionCreators";
export default function AdminProduct() {
  let dispatch = useDispatch();
  let ProductStateData = useSelector((state) => state.ProductStateData);
  const data = useMemo(
    () => (Array.isArray(ProductStateData) ? ProductStateData : []),
    [ProductStateData],
  );

  function deleteRecord(id) {
    if (window.confirm("Are You Sure to Delete that Item : ")) {
      dispatch(deleteProduct({ id: id }));
    }
  }

  useEffect(() => {
    dispatch(getProduct());
  }, [dispatch]);

  useEffect(() => {
    const time = setTimeout(() => {
      $("#DataTable").DataTable();
    }, 500);
    return () => clearTimeout(time);
  }, [data]);

  return (
    <>
      <HeroSection title="Admin" />
      <div className="container-fluid">
        <div className="row">
          <div className="col-md-3">
            <Sidebar />
          </div>
          <div className="col-md-9">
            <h5 className="bg-primary text-center text-light p-2">
              Product{" "}
              <Link to="/admin/product/create" aria-label="Create product">
                {" "}
                <i className="fa fa-plus text-light float-end"></i>
              </Link>
            </h5>
            <div className="table-responsive">
              <table
                className="table table-bordered table-hover table-striped"
                id="DataTable"
              >
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Maincategory</th>
                    <th>Subcategory</th>
                    <th>Brand</th>
                    <th>Color</th>
                    <th>Size</th>
                    <th>Base Price</th>
                    <th>Discount</th>
                    <th>Final Price</th>
                    <th>Stock</th>
                    <th>StockQuantity</th>
                    <th>Pic</th>
                    <th>Active</th>
                    <th></th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((item, index) => {
                    return (
                      <tr key={index}>
                        <td>{item.id}</td>
                        <td>{item.name}</td>
                        <td>{item.maincategory}</td>
                        <td>{item.subcategory}</td>
                        <td>{item.brand}</td>
                        <td>{item.color}</td>
                        <td>{item.size}</td>
                        <td>{formatCurrency(item.basePrice)}</td>
                        <td>{item.discount}%</td>
                        <td>{formatCurrency(item.finalPrice)}</td>
                        <td
                          className={`${item.stock ? "text-success" : "text-danger"}`}
                        >
                          {item.stock ? "Yes" : "No"}
                        </td>
                        <td>{item.stockQuantity}</td>
                        <td>
                          <div style={{ width: 400 }}>
                            {item.pic?.map((pic, index) => {
                              return (
                                <Link
                                  key={index}
                                  to={`${process.env.REACT_APP_SERVER}${pic}`}
                                  target="_blank"
                                  rel="noreferrer"
                                >
                                  <img
                                    src={`${process.env.REACT_APP_SERVER}${pic}`}
                                    height={50}
                                    width={50}
                                    className="rounded me-2"
                                    alt={`${item.name} ${index + 1}`}
                                  />
                                </Link>
                              );
                            })}
                          </div>
                        </td>
                        <td>{item.active ? "Yes" : "No"}</td>
                        <td>
                          <Link
                            to={`/admin/product/update/${item.id}`}
                            className="btn btn-primary"
                            aria-label={`Edit ${item.name}`}
                          >
                            <i className="fa fa-edit"></i>
                          </Link>
                        </td>
                        <td>
                          <button
                            className="btn btn-danger"
                            onClick={() => deleteRecord(item.id)}
                            aria-label={`Delete ${item.name}`}
                          >
                            <i className="fa fa-trash"></i>
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}

// import React, { useEffect, useState } from 'react'

// import $ from 'jquery';  // Import jQuery
// import 'datatables.net-dt/css/dataTables.dataTables.min.css'; // Import DataTables styles
// import 'datatables.net';

// import Sidebar from '../../Components/Sidebar'
// import HeroSection from '../../Components/HeroSection'
// import { Link } from 'react-router-dom'

// export default function AdminProduct() {
//   let [data, setData] = useState([])

//   async function deleteRecord(id) {
//     if (window.confirm("Are You Sure to Delete that Item : ")) {
//       let response = await fetch(`${process.env.REACT_APP_SERVER}/product/${id}`, {
//         method: "DELETE",
//         headers: {
//           "content-type": "application/json"
//         }
//       })
//       response = await response.json()
//       getAPIData()
//     }
//   }

//   async function getAPIData() {
//     let response = await fetch(`${process.env.REACT_APP_SERVER}/product`, {
//       method: "GET",
//       headers: {
//         "content-type": "application/json"
//       }
//     })
//     response = await response.json()
//     if (response)
//       setData(response)
//     else
//       alert("Something Went Wrong")

//     let time = setTimeout(() => {
//       $('#DataTable').DataTable()
//     }, 500);
//     return time
//   }
//   useEffect(() => {
//     let time = getAPIData()
//     return () => clearTimeout(time)
//   }, [])

//   return (
//     <>
//       <HeroSection title="Admin" />
//       <div className="container-fluid">
//         <div className='row'>
//           <div className="col-md-3">
//             <Sidebar />
//           </div>
//           <div className="col-md-9">
//             <h5 className='bg-primary text-center text-light p-2'>Product <Link to="/admin/product/create"> <i className='fa fa-plus text-light float-end'></i></Link></h5>
//             <table className='table table-bordered table-hover table-striped' id='DataTable'>
//               <thead>
//                 <tr>
//                   <th>ID</th>
//                   <th>Name</th>
//                   <th>Pic</th>
//                   <th>Active</th>
//                   <th></th>
//                   <th></th>
//                 </tr>
//               </thead>
//               <tbody>
//                 {
//                   data.map((item, index) => {
//                     return <tr key={index}>
//                       <td>{item.id}</td>
//                       <td>{item.name}</td>
//                       <td>
//                         <Link to={`${process.env.REACT_APP_SERVER}${item.pic}`} target='_blank' rel='noreferrer'>
//                           <img src={`${process.env.REACT_APP_SERVER}${item.pic}`} height={50} width={80} className='rounded' alt="Product Image" />
//                         </Link>
//                       </td>
//                       <td>{item.active ? "Yes" : "No"}</td>
//                       <td><Link to={`/admin/product/update/${item.id}`} className='btn btn-primary'><i className='fa fa-edit'></i></Link></td>
//                       <td><button className='btn btn-danger' onClick={() => deleteRecord(item.id)}><i className='fa fa-trash'></i></button></td>
//                     </tr>
//                   })
//                 }
//               </tbody>
//             </table>
//           </div>
//         </div>
//       </div>
//     </>
//   )
// }
