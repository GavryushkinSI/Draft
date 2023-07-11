import {useEffect, useRef} from 'react';
import {TAnyRecord} from "../models/models";

/**
 * Кастомный хук, который позволяет увидеть какие изменения в props вызывают повторный рендеринг.
 * @param name Имя компонента.
 * @param props Пропсы компонента.
 */
export function useWhyDidYouUpdate<T extends TAnyRecord>(name: string, props: T): void {
    const previousProps = useRef<T>();

    useEffect(() => {
        const previousPropsObject = previousProps.current;

        if (previousPropsObject) {
            const allKeys = Object.keys({...previousPropsObject, ...props});
            const changesObj: TAnyRecord = {};

            allKeys.forEach((key) => {
                if (previousPropsObject[key] !== props[key]) {
                    changesObj[key] = {
                        from: previousPropsObject[key],
                        to: props[key],
                    };
                }
            });

            if (Object.keys(changesObj).length) {
                // eslint-disable-next-line no-console
                console.log('[why-did-you-update]', name, changesObj);
            }
        }

        previousProps.current = props;
    });
}
