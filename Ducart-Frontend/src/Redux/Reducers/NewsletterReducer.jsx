import { CREATE_NEWSLETTER_RED, DELETE_NEWSLETTER_RED, GET_NEWSLETTER_RED, UPDATE_NEWSLETTER_RED } from "../Constants"
export default function NewsletterReducer(state=[],action){
    switch(action.type){
        case CREATE_NEWSLETTER_RED:
            return [...state, action.payload]

        case GET_NEWSLETTER_RED:
            return Array.isArray(action.payload) ? action.payload : []

        case UPDATE_NEWSLETTER_RED:
            return state.map((x)=>x.id===action.payload.id ? {...x, ...action.payload} : x)

        case DELETE_NEWSLETTER_RED:
            return state.filter((x)=>x.id!==action.payload.id)

        default:
            return state
    }
}
