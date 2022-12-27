import React from 'react';
import ReactDOM from 'react-dom';
import {combineReducers, createStore, Store} from "redux";
import {devToolsEnhancer} from '@redux-devtools/extension';
import {BrowserRouter, Route, Routes} from "react-router-dom";
import {Provider} from "react-redux";
import Renko from "./components/Renko";

const initial = {
    data: [],
    isLoading: false,
};

export enum EActionTypes {
    SET_IS_LOADING = "SET_IS_LOADING",
    SET_DATA = "SET_DATA",
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
        case EActionTypes.SET_IS_LOADING: {
            return {
                ...state,
                isLoading: action.payload,
            };
        }
        case EActionTypes.SET_DATA: {
            return {...state, data: [...state.data, action.payload].flat()};
        }
        default:
            return state;
    }
};

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
}

/**
 * @prop isAdaptive Флаг отображения в адаптивном режиме.
 * @prop settings Настройки модуля РПП.
 * @prop userActions Доступные действия пользователя.
 */
export interface IStrategyState {
    isLoading: boolean;
}

/**
 * Настройка redux-стора.
 */
const configureStore = () => {
    return {
        ...createStore<IAppState, TAction, unknown, unknown>(
            combineReducers<IAppState>({renko: reducer}),
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
                    <Route path="/" element={<Renko/>}/>
                </Routes>
            </BrowserRouter>
        </Provider>
    )
}

ReactDOM.render(<Root/>, document.getElementById('root'));