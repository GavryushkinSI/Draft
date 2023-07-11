import {DependencyList, useEffect, useRef} from 'react';
import {useIsMounted} from "./useIsMounted";

const lightboxId = 'accessible-lightbox';

/**
 * @prop role ARIA роль.
 * @prop tabIndex Значение аттрибута tabindex.
 * @prop id Идентификатор.
 */
interface IAccessibilityAttrs {
    role: string;
    tabIndex: number;
    id: string;
}

/**
 * Хук, который устанавливает фокус на лайтбокс при его открытии.
 * Также не позволяет фокусу покинуть лайтбокс.
 * @param deps Зависимости, при изменении которых фокус падает на лайтбокс.
 */
export function useAccessibleLightbox(deps: DependencyList): IAccessibilityAttrs {
    const isMounted = useIsMounted();
    const lightbox = useRef<HTMLDivElement>();

    useEffect(() => {
        lightbox.current = document.querySelector('#' + lightboxId) as HTMLDivElement;

        if (lightbox.current) {
            lightbox.current.focus();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, deps);

    useEffect(() => {
        const handleFocus = () => {
            if (!isMounted()) return;

            const currentActiveElement = document.activeElement;

            if (lightbox.current && currentActiveElement && !lightbox.current.contains(currentActiveElement)) {
                lightbox.current.focus();
            }
        };

        document.addEventListener('focus', handleFocus, true);

        return () => document.removeEventListener('focus', handleFocus, true);
    }, [isMounted]);

    return {role: 'dialog', tabIndex: -1, id: lightboxId};
}
