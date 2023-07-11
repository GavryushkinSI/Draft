import isEqual from 'lodash/isEqual';
import {useRef} from 'react';
import {useDispatch} from 'react-redux';
import {Dispatch} from 'redux';
import {TAnyRecord} from "../models/models";
/**
 * Хук для работы с экшенами.
 *
 * @param actionsClass Класс с экшенами.
 * @param params Любые необязательные параметры, передаваемые в конструктор класса.
 */
export function useActions<T>(actionsClass: new (dispatch: Dispatch, ...params: any[]) => T, ...params: any[]): T {
    const dispatch = useDispatch();
    const ref = useRef<T | null>(null);
    const refParams = useRef<TAnyRecord | null>(null);

    if (!ref.current || !isEqual(refParams.current, params)) {
        ref.current = new actionsClass(dispatch, ...params);
        refParams.current = params;
    }

    return ref.current;
}
