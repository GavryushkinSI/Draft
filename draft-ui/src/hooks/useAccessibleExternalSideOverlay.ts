import {useEffect, useRef, useState} from 'react';

// Таймаут по истечении которого скрываем оверлей от скрин-ридера.
const HIDE_TIMEOUT = 1000;

/**
 * Хук для скрытия от скрин-ридера оверлеев, подключенных через UFSLauncher.
 * @param isOpened Флаг открыт ли оверлей.
 */
export function useAccessibleExternalSideOverlay(isOpened: boolean): {isHidden: boolean} {
    const [isHidden, setHidden] = useState(!isOpened);
    const timeoutId = useRef<ReturnType<typeof setTimeout>>();

    useEffect(() => {
        if (isOpened) {
            timeoutId.current && clearTimeout(timeoutId.current);
            setHidden(false);
        } else {
            timeoutId.current = setTimeout(() => setHidden(true), HIDE_TIMEOUT);
        }
    }, [isOpened]);

    return {
        isHidden: !isOpened && isHidden,
    };
}
