import {EActionTypes} from "../index";

export class Service {

    constructor(private dispatch:any) {}

    public setData(data: any) {
        this.dispatch({type: EActionTypes.SET_DATA, payload: data});
    }

    public setLoading(isLoading: boolean) {
        this.dispatch({type: EActionTypes.SET_IS_LOADING, payload: isLoading});
    }
}
