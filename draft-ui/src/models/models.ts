import {max, min} from "lodash";
import {profitFactor} from "../utils/utils";

export interface Message {
    message: any;
    status: any;
    command: any;
}

export interface IAccount {
    id: string;
    cash: string;
    figi: string;
    balance: number;
}

export interface IStrategy {
    id?: string;
    userName?: string;
    name?: string | undefined;
    producer?: EProducer | undefined;
    ticker?: string;
    quantity?: number;
    slippage?: number;
    consumer?: EConsumer[];
    isActive?: boolean;
    direction?: string;
    figi?: string;
    orders?: any[];
    priceTv?: string;
    currentPosition?: number;
    description?: string;
}

export enum EProducer {
    TKS = "Tinkoff_Invest",
    ALOR = "ALOR"
}

export enum EConsumer {
    EMAIL = "Почта",
    API = "Апи",
    TEST = "test"
}

export interface IBackTestResultStartegy {
    count: number;
    sumProfit: number;
    maxProfitOnOrder: number;
    maxLossOnOrder: number;
    profitFactor: string | undefined;
}

export interface IComment {
    id?: string;
    number?:number;
    author?: string;
    date?: string;
    content?: string;
}

export interface IArticle {
    id?: string;
    type?: string;
    typeView?: string;
    forAdmin?: boolean;
    header?: string;
    message?: string;
    time?: string;
    blockCommentEnabled?: boolean;
    comments?: IComment[];
}