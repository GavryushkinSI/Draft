export interface Message {
    message: any;
    status: any;
}

export interface IStrategy {
    id: string;
    name: string | undefined;
    producer: EProducer | undefined;
    ticker: string;
    position: number;
    slippage: number;
    consumer: EConsumer[];
    isActive: boolean;
}

export enum EProducer {
    TKS = "TKS",
    ALOR = "ALOR"
}

export enum EConsumer {
    EMAIL = "Почта",
    API = "Апи",
    TEST = "Тест"
}
