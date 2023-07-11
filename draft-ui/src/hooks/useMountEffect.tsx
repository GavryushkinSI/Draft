import React, {useEffect} from 'react';

/**
 * Аналог componentDidMount.
 * @param func Колбэк функция.
 */
export const useMountEffect = (func: React.EffectCallback): void => {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    useEffect(func, []);
};
