import http from "../utils/http-common";
import {IArticle} from "../models/models";

export class AdminServices {

    constructor(private dispatch: any) {
    }

    public getData() {
        return http.get('/adminTickers');
    }

    public addArticle(article: IArticle, successCallback: () => void, failCallback?: () => void) {
        return http.post('/addArticle', article).then(() => {
            successCallback && successCallback();
        }).catch(() => {
            failCallback && failCallback();
        });
    }

    public getMetrics(successCallback?: () => void, failCallback?: () => void) {
        return http.get('/getMetrics').catch(() => {
            failCallback && failCallback();
        });
    }
}