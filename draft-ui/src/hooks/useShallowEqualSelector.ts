import {shallowEqual} from 'react-redux';
// import {useSelector} from 'Common/Hooks/useSelector';
// import {IAppState} from 'Store/ModelState';

// /**
//  * Аналог useSelector, в котором вместо строгого сравнения результатов селектора используется shallowEqual.
//  * @param selector Функция селектора.
//  */
// export function useShallowEqualSelector<TSelected>(selector: (state: IAppState) => TSelected): TSelected {
//     return useSelector<TSelected>(selector, shallowEqual);
// }
