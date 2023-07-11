import {useRef} from 'react';

/**
 * Хук для получения уникального идентификатора.
 */
export const useUniqueId = (() => {
    let id = 0;

    return (prefix = '') => {
        const idRef = useRef<number>();

        if (!idRef.current) {
            idRef.current = id++;
        }

        return `${prefix}${idRef.current}`;
    };
})();
