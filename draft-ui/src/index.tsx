import React from "react";
import ReactDOM from 'react-dom';
import {combineReducers, createStore} from "redux";
import {devToolsEnhancer} from '@redux-devtools/extension';
import {BrowserRouter, Route, Routes, useNavigate} from "react-router-dom";
import {Provider} from "react-redux";
import "./styles/common.css";
import "./styles/sideBar.css";
import "./styles/developPage.css";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css"
import notificationReducer, {INotificationState} from "./reducers/notificationReducers";
import {IStrategy} from "./models/models";
import "./styles/notification.css";
import {ROOT} from "./Route/consts";
import Admin from "./components/Admin";

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
    CHANGE_VIEWED_NOTIFY_IDS = "CHANGE_VIEWED_NOTIFY_IDS",
    GET_TICKER = "GET_TICKER",
    SET_PORTFOLIO = "SET_PORTFOLIO",
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

const reducer2 = (state = {data: new Map<string, any>()}, action: IAction): any => {
    switch (action.type) {
        case EActionTypes.SET_LAST_PRICE: {
            return {
                ...state,
                data: state.data.set(action.payload.figi, {
                    price: action.payload.price,
                    time: action.payload.updateTime,
                }),
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
    lastPrice: { data: Map<string, any> };
    ticker: any;
    notifications: INotificationState;
    strategy: any;
    portfolio: any;
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

const portfolioReducer = (state = [], action: IAction): any => {
    switch (action.type) {
        case EActionTypes.SET_PORTFOLIO:
            return {
                ...state,
                data: action.payload,
            };

        default:
            return state;
    }
}

const userReducer = (state = {}, action: any): any => {
    switch (action.type) {
        case EActionTypes.GET_USER_INFO: {
            return {
                ...state,
                data: action.payload,
            };
        }

        case EActionTypes.CHANGE_VIEWED_NOTIFY_IDS: {
            // @ts-ignore
            const updatedData = {...state.data, viewedNotifyIds: action.payload};
            return {
                ...state, data: updatedData
            }
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
                portfolio: portfolioReducer,
            }),
            devToolsEnhancer()
        )
    };
};

const store = configureStore();

const MainPage = React.lazy(() => import(/* webpackChunkName: "MainPage" */ './components/Admin'));
const AdminPanel = React.lazy(() => import(/* webpackChunkName: "AdminPanel" */ './components/AdminPanel'));
const Articles = React.lazy(() => import(/* webpackChunkName: "Articles" */ './components/Articles'));
const PublicStrategy = React.lazy(() => import(/* webpackChunkName: "PublicStrategy" */ './components/PublicStrategy'));

function Root() {
    return (
        <Provider store={store}>
            <React.Suspense fallback={<div>{'Загрузка...'}</div>}>
                <BrowserRouter>
                    <Routes>
                        <Route path={ROOT().DRAFT.MAIN_PAGE.PATH} element={<MainPage/>}/>
                        <Route path={ROOT().DRAFT.ADMIN_PANEL.PATH} element={<AdminPanel/>}/>
                        <Route path={ROOT().DRAFT.PAGE_WITH_ARTICLES.PATH} element={<Articles/>}/>
                        <Route path={ROOT().DRAFT.PUBLIC_STRATEGY.PATH} element={<PublicStrategy/>}/>
                    </Routes>
                </BrowserRouter>
            </React.Suspense>
        </Provider>
    )
}

ReactDOM.render(<Root/>, document.getElementById('root'));