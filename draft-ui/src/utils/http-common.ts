import axios from "axios";

export const webSocketUrl = process?.env?.NODE_ENV === 'development' ? 'http://localhost:9000/ws' : '/ws';
export default axios.create({
    baseURL: process?.env?.NODE_ENV === 'development' ? "http://localhost:9000/app" : "/api",
    headers: {
        "Content-type": "application/json"
    }
});

export enum ETab{
    MAIN='MAIN',

}
