import {IBackTestResultStartegy} from "./models/models";

export const backTestResultStrategy: IBackTestResultStartegy[] = [
    {
        count: 10,
        sumProfit: 100,
        maxProfitOnOrder: 10,
        maxLossOnOrder: -20,
        profitFactor: '2.0'
    },
    {
        count: 12,
        sumProfit: 200,
        maxProfitOnOrder: 10,
        maxLossOnOrder: -20,
        profitFactor: '1.0'
    },
    {
        count: 15,
        sumProfit: 300,
        maxProfitOnOrder: 10,
        maxLossOnOrder: -20,
        profitFactor: '1.5'
    }
]
