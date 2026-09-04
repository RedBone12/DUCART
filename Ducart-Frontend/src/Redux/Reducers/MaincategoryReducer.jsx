import { CREATE_MAINCATEGORY_RED, DELETE_MAINCATEGORY_RED, GET_MAINCATEGORY_RED, UPDATE_MAINCATEGORY_RED } from "../Constants"
export default function MaincategoryReducer(state=[],action){
    switch(action.type){
        case CREATE_MAINCATEGORY_RED:
            return [...state, action.payload]

        case GET_MAINCATEGORY_RED:
            return Array.isArray(action.payload) ? action.payload : []

        case UPDATE_MAINCATEGORY_RED:
            return state.map((x)=>x.id===action.payload.id ? {...x, ...action.payload} : x)

        case DELETE_MAINCATEGORY_RED:
            return state.filter((x)=>x.id!==action.payload.id)

        default:
            return state
    }
}
