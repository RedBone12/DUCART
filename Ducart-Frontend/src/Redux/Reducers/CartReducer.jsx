import { CREATE_CART_RED, DELETE_CART_RED, GET_CART_RED, UPDATE_CART_RED } from "../Constants"
export default function CartReducer(state=[],action){
    switch(action.type){
        case CREATE_CART_RED:
            return [...state, action.payload]

        case GET_CART_RED:
            return Array.isArray(action.payload) ? action.payload : []

        case UPDATE_CART_RED:
            return state.map((x)=>x.id===action.payload.id ? {...x, ...action.payload} : x)

        case DELETE_CART_RED:
            return state.filter((x)=>x.id!==action.payload.id)

        default:
            return state
    }
}
