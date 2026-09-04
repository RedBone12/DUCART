import { put, takeEvery } from "redux-saga/effects"
import { CREATE_TESTIMONIAL, CREATE_TESTIMONIAL_RED, DELETE_TESTIMONIAL, DELETE_TESTIMONIAL_RED, GET_TESTIMONIAL, GET_TESTIMONIAL_RED, UPDATE_TESTIMONIAL, UPDATE_TESTIMONIAL_RED } from "../Constants"

import { createRecord, createMultipartRecord, deleteRecord, getRecord, updateRecord, updateMultipartRecord } from "./Services"

export function* createSaga(action) {
    let response = action.payload instanceof FormData
        ? yield createMultipartRecord("testimonial", action.payload)
        : yield createRecord("testimonial", action.payload)
    yield put({ type: CREATE_TESTIMONIAL_RED, payload: response })
}
export function* getSaga() {
    let response = yield getRecord("testimonial")
    yield put({ type: GET_TESTIMONIAL_RED, payload: response })
}
export function* updateSaga(action) {
    let response = action.payload instanceof FormData
        ? yield updateMultipartRecord("testimonial", action.payload)
        : yield updateRecord("testimonial", action.payload)
    yield put({ type: UPDATE_TESTIMONIAL_RED, payload: response })
}

export function* deleteSaga(action) {
    yield deleteRecord("testimonial", action.payload)
    yield put({ type: DELETE_TESTIMONIAL_RED, payload: action.payload })
}

export default function* testimonialSaga() {
    yield takeEvery(CREATE_TESTIMONIAL, createSaga)
    yield takeEvery(GET_TESTIMONIAL, getSaga)
    yield takeEvery(UPDATE_TESTIMONIAL, updateSaga)
    yield takeEvery(DELETE_TESTIMONIAL, deleteSaga)
}
