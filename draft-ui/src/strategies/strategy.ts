// import {cloneDeep} from "lodash";
//
// const SMA = require('technicalindicators').SMA;
// const ATR = require('technicalindicators').ATR;
// const CCI = require('technicalindicators').CCI;
//
// export function closeStrategy(data: any[], isCommission: boolean = false){
//     let dataWithMarkerOrders = cloneDeep(data);
//     let closePrice = null;
//     let orders = [];
//     let equity: any[] = [];
//     let count = 0;
//     let profit: number = 0;
//     const valueOfCommission: number = isCommission ? 50 : 0;
//     for (let i = 0; i < dataWithMarkerOrders.length; i++) {
//         if (i === 0) {
//             if (dataWithMarkerOrders[i].y[3] > dataWithMarkerOrders[i].y[0]) {
//                 closePrice = dataWithMarkerOrders[i].y[3];
//                 orders.push({time: dataWithMarkerOrders[i].label, direction: "Покупка", result: 0, profit: 0});
//                 equity.push({label: dataWithMarkerOrders[i].label, x: dataWithMarkerOrders[i].x, y: 0});
//                 count++;
//                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬆"};
//             }
//             if (dataWithMarkerOrders[i].y[3] < dataWithMarkerOrders[i].y[0]) {
//                 closePrice = dataWithMarkerOrders[i].y[3];
//                 orders.push({time: dataWithMarkerOrders[i].label, direction: "Продажа", result: 0, profit: 0});
//                 equity.push({label: dataWithMarkerOrders[i].label, x: dataWithMarkerOrders[i].x, y: 0});
//                 count++;
//                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬇"};
//             }
//         } else {
//             if (dataWithMarkerOrders[i].y[3] > dataWithMarkerOrders[i].y[0] && dataWithMarkerOrders[i - 1].y[3] < dataWithMarkerOrders[i - 1].y[0]) {
//                 let result = closePrice - dataWithMarkerOrders[i].y[3];
//                 closePrice = dataWithMarkerOrders[i].y[3];
//                 profit += result;
//                 orders.push({
//                     time: dataWithMarkerOrders[i].label,
//                     direction: "Покупка",
//                     result,
//                     profit,
//                 });
//                 count++;
//                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬆"}
//             }
//             if (dataWithMarkerOrders[i].y[3] < dataWithMarkerOrders[i].y[0] && dataWithMarkerOrders[i - 1].y[3] > dataWithMarkerOrders[i - 1].y[0]) {
//                 let result = dataWithMarkerOrders[i].y[3] - closePrice;
//                 closePrice = dataWithMarkerOrders[i].y[3];
//                 profit += result;
//                 orders.push({
//                     time: dataWithMarkerOrders[i].label,
//                     direction: "Продажа",
//                     result,
//                     profit,
//                 });
//                 equity.push({label: dataWithMarkerOrders[i].label, x: dataWithMarkerOrders[i].x, y: profit});
//                 count++;
//                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬇"};
//             }
//         }
//     }
//
//     return {orders, dataWithMarkerOrders, equity, indicator: undefined};
// }
//
// export function calcMagicTrend(data: any[], params:any){
//     const nz = (array: any[]) => {
//         const value = array.slice(-1)[0];
//         return !!value ? value.y : 0;
//     }
//
//     const atrPeriod = params.param2 || 5;
//     const coeff = params.param3 || 2.0;
//     const cciPeriod = params.param4 || 20;
//     let atr = new ATR({high: [], low: [], close: [], period: atrPeriod});
//     let cci = new CCI({high: [], low: [], close: [], period: cciPeriod});
//     let magicTrend: any[] = [];
//     data.forEach((item, index) => {
//             let candle = item.y[1] >= item.y[0] ? {
//                 high: item.y[1],
//                 low: item.y[0],
//                 close: item.y[1]
//             } : {high: item.y[0], low: item.y[1], close: item.y[1]};
//             let currentAtr = atr.nextValue(candle);
//             if (currentAtr) {
//                 let upT = candle.low - currentAtr * coeff
//                 let downT = candle.high + currentAtr * coeff;
//                 let currentCCI = cci.nextValue(candle);
//                 magicTrend.push({
//                     label: item.label, x: item.x, y: currentCCI >= 0 ?
//                         (upT < magicTrend.slice(-1)[0] ? nz(magicTrend) : upT)
//                         : (downT > nz(magicTrend) ? nz(magicTrend) : downT)
//                 });
//             } else {
//                 magicTrend.push({label: item.label, x: item.x, y: null});
//             }
//         }
//     );
//     return magicTrend;
// }
//
// export function trendMagicStrategy(data: any[], params:any, isCommission: boolean = false){
//     let dataWithMarkerOrders = cloneDeep(data);
//     const indicator = calcMagicTrend(data, params);
//     let closePrice = null;
//     let orders = [];
//     let equity: any[] = [];
//     let count = 0;
//     let profit: number = 0;
//     const valueOfCommission: number = isCommission ? 50 : 0;
//     for (let i = 0; i < dataWithMarkerOrders.length; i++) {
//         if (orders.length === 0) {
//             if (dataWithMarkerOrders[i].y[3] > indicator[i].y) {
//                 closePrice = dataWithMarkerOrders[i].y[3];
//                 orders.push({time: dataWithMarkerOrders[i].label, direction: "Покупка", result: 0, profit: 0});
//                 count++;
//                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬆"};
//             }
//             if (dataWithMarkerOrders[i].y[3] < indicator[i].y) {
//                 closePrice = dataWithMarkerOrders[i].y[3];
//                 orders.push({time: dataWithMarkerOrders[i].label, direction: "Продажа", result: 0, profit: 0});
//                 count++;
//                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬇"};
//             }
//         } else {
//             if (dataWithMarkerOrders[i].y[3] > indicator[i].y && dataWithMarkerOrders[i - 1].y[3] < indicator[i - 1].y) {
//                 let result = closePrice - dataWithMarkerOrders[i].y[3];
//                 closePrice = dataWithMarkerOrders[i].y[3];
//                 profit += result;
//                 orders.push({
//                     time: dataWithMarkerOrders[i].label,
//                     direction: "Покупка",
//                     result,
//                     profit,
//                 });
//                 count++;
//                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬆", profit};
//             }
//             if (dataWithMarkerOrders[i].y[3] < indicator[i].y && dataWithMarkerOrders[i - 1].y[3] > indicator[i - 1].y) {
//                 let result = dataWithMarkerOrders[i].y[3] - closePrice;
//                 closePrice = dataWithMarkerOrders[i].y[3];
//                 profit += result;
//                 orders.push({
//                     time: dataWithMarkerOrders[i].label,
//                     direction: "Продажа",
//                     result,
//                     profit,
//                 });
//                 count++;
//                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬇", profit};
//             }
//         }
//     }
//
//     return {orders, dataWithMarkerOrders, equity, indicator};
// }

export const n={};