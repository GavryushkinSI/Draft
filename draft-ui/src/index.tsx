import React from 'react';
import ReactDOM from 'react-dom';
import {combineReducers, createStore} from "redux";
import {devToolsEnhancer} from '@redux-devtools/extension';
import {BrowserRouter, Route, Routes} from "react-router-dom";
import {Provider} from "react-redux";
import Admin from "./components/Admin";
import "./styles/common.css";
import "./styles/sideBar.css";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css"
import notificationReducer, {INotificationState} from "./reducers/notificationReducers";
import {IStrategy} from "./models/models";
import "./styles/notification.css";

const initial = {
    data: [],
    lastTimeUpdate: undefined,
    isLoading: false,
};

export enum EActionTypes {
    SET_IS_LOADING = "SET_IS_LOADING",
    SET_DATA = "SET_DATA",
    UPDATE_DATA = "UPDATE_DATA",
    SET_LAST_TIME_UPDATE = "SET_LAST_TIME_UPDATE",
    SET_LAST_PRICE = "SET_LAST_PRICE",
    GET_USER_INFO = "GET_USER_INFO",
    GET_TICKER = "GET_TICKER",
}

/**
 * @prop type Тип экшена.
 * @prop [payload] Данные экшена.
 * @prop [error] Ошибка.
 */
export interface IAction {
    type: string;
    payload?: any;
    error?: any;
}

const reducer = (state = initial, action: IAction): any => {
    switch (action.type) {
        // case EActionTypes.SET_IS_LOADING: {
        //     return {
        //         ...state,
        //         isLoading: action.payload,
        //     };
        // }
        case EActionTypes.SET_DATA: {
            return {...state, data: [...state.data, action.payload].flat()};
        }
        case EActionTypes.SET_LAST_TIME_UPDATE: {
            return {
                ...state,
                lastTimeUpdate: action.payload,
            };
        }
        default:
            return state;
    }
};

const reducer2 = (state = {data: new Map<string, number>()}, action: IAction): any => {
    switch (action.type) {
        case EActionTypes.SET_LAST_PRICE: {
            return {
                ...state,
                data: state.data.set(action.payload.figi, action.payload.price),
            };
        }
        default:
            return state;
    }
}

const tickerReducer = (state = {data: null}, action: IAction): any => {
    switch (action.type) {
        case EActionTypes.GET_TICKER: {
            return {
                ...state,
                data: action.payload,
            };
        }
        default:
            return state;
    }
}

/**
 * @prop type Тип экшена.
 */
export type TAction<T = string> = { type: T };

/**
 * Модель стейта.
 *
 */
export interface IAppState {
    renko: any;
    user: any;
    lastPrice: {data: Map<string, number>};
    ticker: any;
    notifications: INotificationState;
    strategy: any;
}

/**
 * @prop isAdaptive Флаг отображения в адаптивном режиме.
 * @prop settings Настройки модуля РПП.
 * @prop userActions Доступные действия пользователя.
 */
export interface IStrategyState {
    isLoading: boolean;
    data: IStrategy[];
}

const strategyReducer = (state = {isLoading: false, data: []}, action: any): any => {
    switch (action.type) {
        case EActionTypes.SET_IS_LOADING: {
            return {
                ...state,
                isLoading: action.payload,
            };
        }
        case EActionTypes.SET_DATA: {
            return {...state, data: action.payload};
        }

        default:
            return state;
    }
};

const userReducer = (state = {}, action: any): any => {
    switch (action.type) {
        case EActionTypes.GET_USER_INFO: {
            return {
                ...state,
                data: action.payload,
            };
        }

        default:
            return state;
    }
};

/**
 * Настройка redux-стора.
 */
const configureStore = () => {
    return {
        ...createStore<IAppState, TAction, unknown, unknown>(
            combineReducers<IAppState>({
                renko: reducer,
                lastPrice: reducer2,
                ticker: tickerReducer,
                notifications: notificationReducer,
                strategy: strategyReducer,
                user: userReducer,
            }),
            devToolsEnhancer()
        )
    };
};

const store = configureStore();


function Root() {
    return (
        <Provider store={store}>
            <BrowserRouter>
                <Routes>
                    {/*<Route path="/" element={}/>*/}
                    <Route path="/" element={<Admin/>}/>
                </Routes>
            </BrowserRouter>
        </Provider>
    )
}

ReactDOM.render(<Root/>, document.getElementById('root'));