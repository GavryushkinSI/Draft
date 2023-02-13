import {isEmpty, max, min, sum} from "lodash";
import {IBackTestResultStartegy, IStrategy} from "../models/models";
import moment from "moment";
import {closeStrategy, trendMagicStrategy} from "../strategies/strategy";

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
    switch (strategy) {
        case 'strategy1':
            return closeStrategy(data);
        case 'strategy2':
            return trendMagicStrategy(data, paramsTs);
        // case 'strategy3':
        //     return smaStrategy(data);
        default:
            return null;
    }
}

export function calcDataForGraphProfit(strategy:any[]) {
    const total: { id: number; result: any[]; graphResult: any[]; }[]=[];
    strategy.forEach((item:IStrategy, index)=>{
        const orders=item?.orders||[];
        const buy = orders.filter(i => i.direction === 'buy');
        const sell = orders.filter(i => i.direction === 'sell');

        const len = buy.length > sell.length ? sell.length : buy.length;
        let array = [];
        for (let i = 0; i < len; i++) {
            array.push({
                openDate: moment(buy[i].date) > moment(sell[i].date) ? sell[i].date : buy[i].date,
                closeDate: moment(buy[i].date) > moment(sell[i].date) ? buy[i].date : sell[i].date,
                profit: buy[i].price * (-1) + sell[i].price,
            });
        }

        let result: any[] = [];
        array.reduce((res, value) => {
            // @ts-ignore
            if (!res[value.closeDate]) {
                // @ts-ignore
                res[value.closeDate] = {closeDate: value.closeDate, profit: 0};
                // @ts-ignore
                result.push(res[value.closeDate])
            }
            // @ts-ignore
            res[value.closeDate].profit += value.profit;
            return res;
        }, {});

        let graphResult: any[] = [];
        for (let i = 0; i < result.length; i++) {
            if (graphResult[i - 1]?.y) {
                graphResult.push({
                    x: i + 1,
                    label: moment(result[i].closeDate).format('DD-MM hh:mm:ss'),
                    y: graphResult[i - 1].y + result[i].profit
                });
            } else {
                graphResult.push({x: i + 1, label: moment(result[i].closeDate).format('DD-MM hh:mm:ss'), y: result[i].profit});
            }
        }

        total.push({id:index, result, graphResult});
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
export async function copyTextToClipboard(text:string) {
    if ('clipboard' in navigator) {
        return await navigator.clipboard.writeText(text);
    } else {
        return document.execCommand('copy', true, text);
    }
}



