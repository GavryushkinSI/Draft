import {useCallback, useEffect, useRef} from 'react';

/**
 * Хук для проверки смонтирован ли компонент.
 */
export function useIsMounted(): () => boolean {
    const isMountedRef = useRef(true);
    const isMounted = useCallback(() => isMountedRef.current, []);

    useEffect(() => {
        return () => {
            isMountedRef.current = false;
        };
    }, []);

    return isMounted;
}
