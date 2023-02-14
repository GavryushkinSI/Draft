import {EActionTypes} from "../index";
import http from "../utils/http-common";
import {EConsumer, IStrategy} from "../models/models";

export class Service {

    constructor(private dispatch: any) {
    }

    public setData(data: any, command: string) {
        switch (command) {
            case "newCandle":
                this.dispatch({type: EActionTypes.SET_DATA, payload: data});
                this.dispatch({type: EActionTypes.SET_LAST_TIME_UPDATE, payload: data.x});
                break;
            case "newLastPrice":
                this.dispatch({type: EActionTypes.SET_LAST_PRICE, payload: data});
                break;
            default:
                this.dispatch({type: EActionTypes.SET_DATA, payload: data});
        }
    }

    public feedback(text:string, userName:string){
        return http.post(`/feedback/${userName}`, text);
    }

    public login(auth: any, status: any): Promise<any> {
        return http.post(`/login/${status}`, auth);
    }

    public setLoading(isLoading: boolean) {
        this.dispatch({type: EActionTypes.SET_IS_LOADING, payload: isLoading});
    }

    public doTest() {
        http.get('/test').then((i: any) => console.log(i)).catch((errors: any) => console.log(errors));
    }

    public getUserInfo(userName: string) {
        http.get(`/getUserInfo/${userName}`)
            .then((response: any) => {
                    this.dispatch({type: EActionTypes.GET_USER_INFO, payload: response.data[0]});
                }
            ).catch((errors: any) => console.log(errors));
    }

    public reconnectStream(successCallback?: () => void, failCallback?: (error: any) => void) {
        http.get(`/reconnect`).then(() => {
            successCallback && successCallback()
        }).catch((errors: any) => console.log(errors));
    }

    public getAllStrategyByUserName(userName: string, successCallback?: () => void, failCallback?: (error: any) => void) {
        http.get(`/getAllStrategy/${userName}`)
            .then((response: any) => {
                    successCallback && successCallback();
                    this.dispatch({type: EActionTypes.SET_DATA, payload: response.data});
                    this.getUserInfo(userName);
                }
            ).catch((errors: any) => failCallback && failCallback(errors?.message));
    }

    public setDataStrategy(data: any, callback?: () => void) {
        this.dispatch({type: EActionTypes.SET_DATA, payload: data});
        callback && callback();
    }

    public addOrUpdateStrategy(userName: string, strategy: IStrategy, successCallback?: () => void, failCallback?: (error: any) => void) {
        http.post(`/editStrategy/${userName}`, strategy)
            .then((response: any) => {
                successCallback && successCallback();
                this.dispatch({type: EActionTypes.SET_DATA, payload: response.data});
                this.getUserInfo(userName);
            })
            .catch((errors: any) => failCallback && failCallback(errors?.message));
    }

    public removeStrategy(userName: string, name: string, successCallback?: () => void, failCallback?: (error: any) => void) {
        http.post(`/deleteStrategy/${userName}/${name}`, undefined)
            .then((response: any) => {
                    successCallback && successCallback();
                    this.dispatch({type: EActionTypes.SET_DATA, payload: response.data})
                }
            )
            .catch((errors: any) => failCallback && failCallback(errors?.message));
    }

    public sendOrder(strategy: IStrategy, successCallback?: () => void, failCallback?: (error: any) => void) {
        http.post(`/tv`, {...strategy, consumer: [EConsumer.TEST]})
            .then(() => {
                    successCallback && successCallback();
                    this.getUserInfo(strategy.userName!);
                }
            )
            .catch((errors: any) => failCallback && failCallback(errors?.message));
    }

    public clear(userName: string, successCallback?: () => void, failCallback?: (error: any) => void) {
        http.get(`/clear/${userName}`)
            .then(() => {
                    successCallback && successCallback();
                }
            )
            .catch((errors: any) => failCallback && failCallback(errors?.message));
    }

    public getAllTickers(successCallback?: () => void, failCallback?: (error: any) => void){
        http.get(`/getAllTickers`)
            .then((response) => {
                    successCallback && successCallback();
                this.dispatch({type: EActionTypes.GET_TICKER, payload: response.data})
                }
            )
            .catch((errors: any) => failCallback && failCallback(errors?.message));
    }
}
