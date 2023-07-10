import http from "../utils/http-common";
import {IArticle} from "../models/models";

export class AddService {

    constructor(private dispatch: any) {
    }

    public getAllArticles(): void {
       http.get('/getAllArticles');
    }

}