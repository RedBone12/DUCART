import { CREATE_BRAND_RED, DELETE_BRAND_RED, GET_BRAND_RED, UPDATE_BRAND_RED } from "../Constants"
export default function BrandReducer(state=[],action){
    switch(action.type){
        case CREATE_BRAND_RED:
            return [...state, action.payload]

        case GET_BRAND_RED:
            return Array.isArray(action.payload) ? action.payload : []

        case UPDATE_BRAND_RED:
            return state.map((x)=>x.id===action.payload.id ? {...x, ...action.payload} : x)

        case DELETE_BRAND_RED:
            return state.filter((x)=>x.id!==action.payload.id)

        default:
            return state
    }
}
