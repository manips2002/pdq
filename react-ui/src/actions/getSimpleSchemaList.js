import { store } from '../reducers/store.js';
import { api } from './config.js';

/**
 * Fetches a simple array of schemas (id, name)
 *
 * @author Camilo Ortiz
 */

export const fetching = () => {
    return {
        type: 'FETCHING',
    };
}
export const resolved = (initialInfo) => {
    return {
        type: 'RESOLVED',
        schemaList: initialInfo.schemas
    };
}
export const error = () => {
    return {
        type: 'ERROR',
    };
}

export const getInitialData = () => {

  //fetching
  store.dispatch(fetching());

  return function(dispatch, getState){
    return fetch(api + "/initSchemas")
    .then(res => res.text())
    .then(res => res = JSON.parse(res))
    .then((res) => {
      //if its ok, we keep the data
      dispatch(resolved(res))
    }).catch(err => dispatch(error()));
  }
}
