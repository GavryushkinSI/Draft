import {includes, isEmpty, isEqual, isInteger, max, min, pick, sum, uniqWith} from "lodash";
import {IBackTestResultStartegy, IStrategy} from "../models/models";
import moment from "moment";
// import {closeStrategy, trendMagicStrategy} from "../strategies/strategy";

export function saveRow(editRecord: any, array: Array<any>): Array<any> {
    let copy = array;
    copy = Object.keys(copy).map((index: any) => {
        let item = copy[index];
        if (item.id === editRecord.id) {
            item = editRecord;
        }

        return item;
    });
    return [...copy];
}

export function removeRow(array: Array<any>, selectRecordIndex?: string): Array<any> {
    const copy = array;
    copy.forEach((item, index) => {
        if (item.id === selectRecordIndex) {
            copy.splice(index, 1);
        }
    });
    return [...copy];
}

export function generateUserName(length: number): string {
    let result = '';
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    const charactersLength = characters.length;
    for (let i = 0; i < length; i++) {
        result += characters.charAt(Math.floor(Math.random() * charactersLength));
    }
    return result;
}

export function profitFactor(orders: any[]): string | undefined {
    const array = orders.map(value => {
        return value['result']
    });
    const x = sum(array.filter(i => i > 0));
    const y = Math.abs(sum(array.filter(i => i < 0)));

    return (x / (y | 1))?.toFixed(2);
}

function searchCombination(array1: any[], array2: any[], index: number) {

    if (index === 2) {
        return array1.flatMap(param1 => array2.map(param2 => {
            return {param1, param2};
        }))
    }
    ;

    if (index === 3) {
        return array1.flatMap(param1 => array2.map(param3 => {
            return {...param1, param3};
        }))
    }
    ;

    if (index === 4) {
        return array1.flatMap(param1 => array2.map(param4 => {
            return {...param1, param4};
        }))
    }
    ;

    if (index === 5) {
        return array1.flatMap(param1 => array2.map(param5 => {
            return {...param1, param5};
        }))
    }
    ;

    return [];
}

export function getArrayByStep(param: any) {
    let test = [];
    let x = param.min;
    while (x < param.max) {
        test.push(x);
        if ((x + param.step) < param.max) {
            x = x + param.step;
        } else {
            test.push(param.max);
            x = (x + param.step);
        }
    }

    return test;
}

/**
 * Оценка стратегии по метрикам
 */
export function getStrategyEstimate(orders: any[]): IBackTestResultStartegy {
    const r = orders.map((value) => {
        return value['result']
    });

    return {
        count: orders.length,
        sumProfit: orders.slice(-1)[0]?.profit,
        maxProfitOnOrder: max(r),
        maxLossOnOrder: min(r),
        profitFactor: profitFactor(orders)
    };
}

export function getDataForRenkoChart(brickSize: number, data: any, isTest: boolean = false) {
    let count = 0;
    let newDataPoints: any[] = [];
    let testData = [];
    let oldValue = data[0].y[3];
    let oldValueDate;

    try {
        oldValueDate = moment(data[0].x).toLocaleString();
    } catch (errors) {
        console.log(data[0].x)
    }
    let difference = 0;
    let newValue, dataPoint, xValue, prevData;
    for (let i = 1; i < data.length; i++) {
        dataPoint = data[i].y[3];
        prevData = newDataPoints.slice(-1)[0];
        try {
            xValue = moment(data[i].x).toLocaleString();
        } catch (errors) {
            //console.log("error",CanvasJS.formatDate("2022-12-13 09:00:00", "YYYY-MMM-DD hh:mm:ss", undefined));
        }
        difference = dataPoint - oldValue;
        if (difference > 0 && difference > brickSize) {
            for (let j = 0; j < Math.floor(difference / brickSize); j++) {
                newValue = oldValue + brickSize;
                newDataPoints.push({
                    label: xValue,
                    x: count++,
                    y: [oldValue, newValue, oldValue, newValue],
                    color: "green",
                    // indexLabel:"Покупка"
                });
                testData.push({
                    label: xValue,
                    y: [oldValue, newValue, oldValue, newValue],
                    x: i,
                });
                // }
                oldValue = newValue;
                oldValueDate = data[i].x;
            }
        } else if (difference < 0 && Math.abs(difference) > brickSize) {
            for (let j = 0; j < Math.floor(Math.abs(difference) / brickSize); j++) {
                newValue = oldValue - brickSize;
                newDataPoints.push({
                    label: xValue,
                    x: count++,
                    y: [oldValue, oldValue, newValue, newValue],
                    color: "red",
                });
                testData.push({
                    label: xValue,
                    y: [oldValue, oldValue, newValue, newValue],
                    x: i,
                });
                // }
                oldValue = newValue;
                oldValueDate = data[i].x;
            }
        }
    }

    return newDataPoints;
}

export function getAllCombinations(params: any[]) {
    if (params.length === 1) {
        return getArrayByStep(params[0]).map(i => {
            return {param1: i}
        });
    }
    if (params.length > 1) {
        let index = 1;
        let array = getArrayByStep(params[0]);
        while (index < params.length) {
            array = searchCombination(array, getArrayByStep(params[index++]), index);
        }
        return array;
    }

    return [];
}

export function createChartAndOrdersTable(data: any[] = [], strategy: string, paramsTs: any, isTest: boolean = false) {
    const result: any = applyStrategy(strategy, data, paramsTs);
    return getStrategyEstimate(result.orders);
}

function applyStrategy(strategy: string, data: any[], paramsTs: any, isCommission: boolean = false) {
    // switch (strategy) {
    //     case 'strategy1':
    //         return closeStrategy(data);
    //     case 'strategy2':
    //         return trendMagicStrategy(data, paramsTs);
    //     // case 'strategy3':
    //     //     return smaStrategy(data);
    //     default:
    //         return null;
    // }
}

export const convertData = (data: any[]): Map<string, any[]> => {
    const map = new Map<string, any[]>();
    let sum = 0;
    let sumFee = 0;
    const dataSum: any[] = [];
    const dataPnlInOrder: any[] = [];
    const dataFee: any[] = [];

    uniqWith(data,isEqual).forEach((i: any, index): void => {
        sum = sum + i.closedPnl;
        sumFee = sumFee + i.fee;
        const label: string = i.symbol.concat(": ").concat(i?.size).concat(" - ").concat(i.time!=null?moment(new Date(i.time)).locale('ru').format('DD.MM.YYYY HH:mm'):"");
        dataSum.push({x: index, y: sum, label, color:sum>=0?"green":"red"});
        dataPnlInOrder.push({x: index, y: i.closedPnl, label, color:i.closedPnl>=0?"green":"red"});
        dataFee.push({x: index, y: sumFee, label});
    });
    map.set("dataSum", dataSum);
    map.set("dataPnlInOrder", dataPnlInOrder);
    map.set("dataFee", dataFee);

    return map;
};

interface IOrder{
    price:number;
    quantity?:number;
    direction?:string;
    date?:string;
    orderLinkId:string;
}

export function calcDataForGraphProfit(strategy: any[]) {
    const total: { id: number; result: any[]; graphResult: any[];graphResultWithFee:any[] }[] = [];

    strategy.forEach((item: IStrategy, index) => {
        const orders:IOrder[] = item?.orders || [];
        const buy:IOrder[] = orders.filter(i => i.direction === 'buy');
        const sell:IOrder[] = orders.filter(i => i.direction === 'sell');

        const len = buy.length > sell.length ? sell.length : buy.length;
        let array = [];
        for (let i = 0; i < len; i++) {
            console.log("buy",buy[i], "\nsell",sell[i]);
            array.push({
                openDate: moment(buy[i].date) > moment(sell[i].date) ? sell[i].date : buy[i].date,
                closeDate: moment(buy[i].date) > moment(sell[i].date) ? buy[i].date : sell[i].date,
                profit: buy[i].price * (-1) + sell[i].price,
                fee: (buy[i].price  + sell[i].price)*0.001*(-1),
                orderLinkId:buy[i].orderLinkId.concat(sell[i].orderLinkId)
            });
        }

        let result: any[] = [];
        array.reduce((res, value) => {
            // @ts-ignore
            if (!res[value.closeDate]) {
                // @ts-ignore
                res[value.closeDate] = {closeDate: value.closeDate, profit: 0, profitWithFee: 0};
                // @ts-ignore
                result.push(res[value.closeDate]);
            }
            // @ts-ignore
            res[value.closeDate].profit += value.profit*item.minLot;
            // @ts-ignore
            res[value.closeDate].profitWithFee +=(value.profit+value.fee)*item.minLot;
            return res;
        }, {});

        let graphResult: any[] = [];
        let graphResultWithFee: any[] = [];
        for (let i = 0; i < result.length; i++) {
            let time = moment(result[i].closeDate).toDate();
            let label= moment(new Date(time)).locale('ru').format('DD.MM.YYYY HH:mm:ss');
            if (graphResult[i - 1]?.y) {
                graphResult.push({
                    x: i + 1,
                    label,
                    y: graphResult[i - 1].y + result[i].profit,
                    color: (graphResult[i - 1].y + result[i].profit)>0?"green":"red",
                });
                graphResultWithFee.push({
                    x: i + 1,
                    label,
                    y: graphResultWithFee[i - 1].y + result[i].profitWithFee,
                    color: (graphResultWithFee[i - 1].y + result[i].profitWithFee)>0?"lime":"pink",
                });
            } else {
                graphResult.push({x: i + 1, label, y: result[i].profit, color:(result[i].profit>0?"green":"red")});
                graphResultWithFee.push({x: i + 1, label, y: result[i].profitWithFee, color:(result[i].profitWithFee>0?"lime":"pink")});
            }
        }

        total.push({id: index, result, graphResult, graphResultWithFee});
    });

    return total;
}

/**
 * Обрезать строку
 * @param str строка
 * @param length количество символов после которых обрезать строку
 */
export function stringTruncate(str: string, length: number): string {
    const dots = str.length > length ? '...' : '';
    return str.substring(0, length) + dots;
}

export async function copyTextToClipboard(text: string) {
    let textarea = document.createElement("textarea");
    textarea.textContent = text;
    textarea.style.position = "fixed";  // Prevent scrolling to bottom of page in Microsoft Edge.
    document.body.appendChild(textarea);
    textarea.select();
    try {
        return document.execCommand("copy");  // Security exception may be thrown by some browsers.
    } catch (ex) {
        console.warn("Copy to clipboard failed.", ex);
        return prompt("Copy to clipboard: Ctrl+C, Enter", text);
    } finally {
        document.body.removeChild(textarea);
    }
}

export function getFieldsFromArray(array: any[], fields: string[]):any[] {
   return array?.map(user => pick(user, fields));
}

export function includeInArray(array:any[], value:any){
    return includes(array, value);
}

export function formatNumber(value:any, fraction:number=4){
    if(!!value){
        const isInteger:boolean=!String(value).includes(".");
        return isInteger?value:value.toFixed(fraction);
    }
}

