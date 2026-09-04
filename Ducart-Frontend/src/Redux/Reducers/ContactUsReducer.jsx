import { CREATE_CONTACT_US_RED, DELETE_CONTACT_US_RED, GET_CONTACT_US_RED, UPDATE_CONTACT_US_RED } from "../Constants"
export default function ContactUsReducer(state=[],action){
    switch(action.type){
        case CREATE_CONTACT_US_RED:
            return [...state, action.payload]

        case GET_CONTACT_US_RED:
            return Array.isArray(action.payload) ? action.payload : []

        case UPDATE_CONTACT_US_RED:
            return state.map((x)=>x.id===action.payload.id ? {...x, ...action.payload} : x)

        case DELETE_CONTACT_US_RED:
            return state.filter((x)=>x.id!==action.payload.id)

        default:
            return state
    }
}
