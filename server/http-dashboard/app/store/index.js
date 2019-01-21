import { createStore, compose, applyMiddleware } from 'redux';
import rootReducer from './reducers';
import axios from 'axios';
import axiosMiddleware from 'redux-axios-middleware';
import { responseInterceptor as axiosResponseInterceptor } from './axios';
import { persistStore, autoRehydrate } from 'redux-persist';
import createSagaMiddleware from 'redux-saga';

// import { createWsMiddleware } from "store/redux-websocket-middleware";
import { createBlynkWsMiddleware } from "store/blynk-websocket-middleware";
import {
  blynkSaga,
  INIT_ACTION_TYPE
} from "./blynk-saga";

/* instance for basic API */
axios.defaults.headers['Content-Type'] = 'application/json';
const axiosAPI = axios.create({
  baseURL: '/api',
  options: {
    responseType: 'json',
  }
});
/* axios middleware options*/
const axiosMiddlewareOptions = {
  returnRejectedPromiseOnError: true,
  errorSuffix: '_FAILURE',
  interceptors: {
    response: [
      axiosResponseInterceptor
    ]
  }
};

/* Persist Store Config for PROD & DEV */
const persisStoreConfig = {
  whitelist: [
    /*
     there is white list of stores we should store on storage like LocalStorage.
     Description for each of whitelists below:
     - Login - stores user token
     - Account - ????
     - Storage - Created specially to store data on localStorage
     - Name - description
     */
    // 'Login',
    'Account',
    'Storage'
  ]
};
/* Persist Store Config for DEV */
const persisStoreConfigDev = {};
/* Persist Store Config for PROD */
const persisStoreConfigProd = {};

function configureStoreProd(initialState) {

  const sagaMiddleware = createSagaMiddleware();

  // const wsMiddleware = createWsMiddleware({
  //   defaultEndpoint: `wss://${window.location.hostname}:${window.location.port}/dashws`,
  //   isDebugMode: true,
  // });

  const blynkWsMiddleware = createBlynkWsMiddleware({
    isDebugMode: true,
  });

  const middlewares = [
    // wsMiddleware,
    blynkWsMiddleware,
    axiosMiddleware(axiosAPI, axiosMiddlewareOptions),
    sagaMiddleware
  ];

  const store = createStore(rootReducer, initialState, compose(
    applyMiddleware(...middlewares),
    autoRehydrate()
    )
  );

  return new Promise((resolve) => {
    persistStore(store, Object.assign({}, persisStoreConfig, persisStoreConfigProd), () => {
      sagaMiddleware.run(blynkSaga);
      store.dispatch({
        type: INIT_ACTION_TYPE,
        endpoint: `wss://${window.location.hostname}:${window.location.port}/dashws`,
        isDebugMode: true,
      });
      resolve(store);
    });
  });
}

function configureStoreDev() {

  const sagaMiddleware = createSagaMiddleware();

  // const wsMiddleware = createWsMiddleware({
  //   defaultEndpoint: 'wss://localhost:9443/dashws',
  //   isDebugMode: true,
  // });

  const blynkWsMiddleware = createBlynkWsMiddleware({
    isDebugMode: true,
  });

  const middlewares = [
    // wsMiddleware,
    blynkWsMiddleware,
    axiosMiddleware(axiosAPI, axiosMiddlewareOptions),
    sagaMiddleware
  ];

  const composeEnhancers = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose; // add support for Redux dev tools
  const store = createStore(rootReducer, composeEnhancers(
    applyMiddleware(...middlewares),
    autoRehydrate()
    )
  );

  if (module.hot) {
    module.hot.accept('./reducers', () => {
      const nextReducer = require('./reducers').default; // eslint-disable-line global-require
      store.replaceReducer(nextReducer);
    });
  }

  return new Promise((resolve) => {
    persistStore(store, Object.assign({}, persisStoreConfig, persisStoreConfigDev), () => {
      sagaMiddleware.run(blynkSaga);
      store.dispatch({
        type: INIT_ACTION_TYPE,
        endpoint: 'wss://localhost:9443/dashws',
        isDebugMode: true,
      });
      resolve(store);
    });
  });

}

const configureStore = process.env.NODE_ENV === 'production' ? configureStoreProd : configureStoreDev;

export default configureStore;
