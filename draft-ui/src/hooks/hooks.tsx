import React, {useEffect} from "react";
import {useRef} from 'react';
import {useDispatch, shallowEqual} from 'react-redux';
import {Dispatch} from 'redux';
import {TypedUseSelectorHook, useSelector as _useSelector} from 'react-redux';
import {IAppState} from "../index";

// Типизированный useSelector.
const useSelector: TypedUseSelectorHook<IAppState> = _useSelector;

/**
 * Аналог componentDidMount.
 * @param func Колбэк функция.
 */
export const useMountEffect = (func: React.EffectCallback): void => {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    useEffect(func, []);
};

/**
 * Хук для работы с экшенами.
 *
 * @param actionsClass Класс с экшенами.
 * @param params Любые необязательные параметры, передаваемые в конструктор класса.
 */
export function useActions<T>(actionsClass: new (dispatch: Dispatch, ...params: any[]) => T, ...params: any[]): T {
    const dispatch = useDispatch();
    const ref = useRef<T | null>(null);

    if (!ref.current) {
        ref.current = new actionsClass(dispatch, ...params);
    }

    return ref.current;
}

/**
 * Аналог useSelector, в котором вместо строгого сравнения результатов селектора используется shallowEqual.
 * @param selector Функция селектора.
 */
export function useShallowEqualSelector<TSelected>(selector: (state: IAppState) => TSelected): TSelected {
    return useSelector<TSelected>(selector, shallowEqual);
}
