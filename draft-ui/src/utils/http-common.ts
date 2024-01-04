import axios from "axios";

export const webSocketUrl = typeof process !== 'undefined' && process.env.NODE_ENV === 'development' ? 'http://localhost:9000/ws' : '/ws';
export default axios.create({
    baseURL: typeof process !== 'undefined' && process.env.NODE_ENV === 'development' ? "http://localhost:9000/app" : "/api",
    headers: {
        "Content-type": "application/json"
    }
});

export enum ETab{
    MAIN='MAIN',

}
