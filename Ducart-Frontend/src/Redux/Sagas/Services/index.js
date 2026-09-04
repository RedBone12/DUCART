import { getToken } from "../../../config/auth";

function buildJsonHeaders() {
  const token = getToken();
  const headers = {
    "content-type": "application/json",
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  return headers;
}

function buildAuthHeadersOnly() {
  const token = getToken();
  const headers = {};

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  return headers;
}

async function parseResponse(response) {
  const text = await response.text();
  if (!text) return {};
  try {
    return JSON.parse(text);
  } catch (error) {
    return { message: text };
  }
}

export async function createRecord(collection, payload) {
  const response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}`, {
    method: "POST",
    headers: buildJsonHeaders(),
    body: JSON.stringify(payload),
  });
  return await parseResponse(response);
}

export async function createMultipartRecord(collection, payload) {
  const response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}`, {
    method: "POST",
    headers: buildAuthHeadersOnly(),
    body: payload,
  });
  return await parseResponse(response);
}

export async function getRecord(collection) {
  const response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}`, {
    method: "GET",
    headers: buildJsonHeaders(),
  });
  return await parseResponse(response);
}

export async function updateRecord(collection, payload) {
  const response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}/${payload.id}`, {
    method: "PUT",
    headers: buildJsonHeaders(),
    body: JSON.stringify(payload),
  });
  return await parseResponse(response);
}

export async function updateMultipartRecord(collection, payload) {
  const response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}/${payload.get("id")}`, {
    method: "PUT",
    headers: buildAuthHeadersOnly(),
    body: payload,
  });
  return await parseResponse(response);
}

export async function deleteRecord(collection, payload) {
  const response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}/${payload.id}`, {
    method: "DELETE",
    headers: buildJsonHeaders(),
  });
  return await parseResponse(response);
}


// async function parseResponse(response) {
//     const text = await response.text();
//     if (!text) return {};
//     try {
//         return JSON.parse(text);
//     } catch (error) {
//         return { message: text };
//     }
// }

// export async function createRecord(collection, payload) {
//     let response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}`, {
//         method: "POST",
//         headers: {
//             "content-type": "application/json"
//         },
//         body: JSON.stringify(payload)
//     })
//     return await parseResponse(response)
// }

// export async function createMultipartRecord(collection, payload) {
//     let response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}`, {
//         method: "POST",
//         body: payload
//     })
//     return await parseResponse(response)
// }

// export async function getRecord(collection) {
//     let response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}`, {
//         method: "GET",
//         headers: {
//             "content-type": "application/json"
//         }
//     })
//     return await parseResponse(response)
// }

// export async function updateRecord(collection, payload) {
//     let response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}/${payload.id}`, {
//         method: "PUT",
//         headers: {
//             "content-type": "application/json"
//         },
//         body: JSON.stringify(payload)
//     })
//     return await parseResponse(response)
// }

// export async function updateMultipartRecord(collection, payload) {
//     let response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}/${payload.get("id")}`, {
//         method: "PUT",
//         body: payload
//     })
//     return await parseResponse(response)
// }

// export async function deleteRecord(collection, payload) {
//     let response = await fetch(`${process.env.REACT_APP_SERVER}/${collection}/${payload.id}`, {
//         method: "DELETE",
//         headers: {
//             "content-type": "application/json"
//         }
//     })
//     return await parseResponse(response)
// }
