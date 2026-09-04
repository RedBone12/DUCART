import { CREATE_CHECKOUT_RED, DELETE_CHECKOUT_RED, GET_CHECKOUT_RED, UPDATE_CHECKOUT_RED } from "../Constants"
export default function CheckoutReducer(state=[],action){
    switch(action.type){
        case CREATE_CHECKOUT_RED:
            return [...state, action.payload]

        case GET_CHECKOUT_RED:
            return Array.isArray(action.payload) ? action.payload : []

        case UPDATE_CHECKOUT_RED:
            return state.map((x)=>x.id===action.payload.id ? {...x, ...action.payload} : x)

        case DELETE_CHECKOUT_RED:
            return state.filter((x)=>x.id!==action.payload.id)

        default:
            return state
    }
}
