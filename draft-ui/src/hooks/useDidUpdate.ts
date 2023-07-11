import React, {useEffect, useRef} from 'react';

/**
 * Аналог componentDidUpdate.
 * @param effect Эффект.
 * @param deps Массив зависимостей.
 */
export function useDidUpdate(effect: React.EffectCallback, deps: React.DependencyList): void {
    const isMounted = useRef(false);

    useEffect(() => {
        if (isMounted.current) {
            effect();
        } else {
            isMounted.current = true;
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, deps);
}
