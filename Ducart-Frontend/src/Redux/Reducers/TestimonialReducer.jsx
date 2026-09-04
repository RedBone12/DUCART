import { CREATE_TESTIMONIAL_RED, DELETE_TESTIMONIAL_RED, GET_TESTIMONIAL_RED, UPDATE_TESTIMONIAL_RED } from "../Constants"
export default function TestimonialReducer(state=[],action){
    switch(action.type){
        case CREATE_TESTIMONIAL_RED:
            return [...state, action.payload]

        case GET_TESTIMONIAL_RED:
            return Array.isArray(action.payload) ? action.payload : []

        case UPDATE_TESTIMONIAL_RED:
            return state.map((x)=>x.id===action.payload.id ? {...x, ...action.payload} : x)

        case DELETE_TESTIMONIAL_RED:
            return state.filter((x)=>x.id!==action.payload.id)

        default:
            return state
    }
}
