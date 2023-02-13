import axios from "axios";

export const {NODE_ENV} = process.env;
export const webSocketUrl = NODE_ENV === 'development' ? 'http://localhost:9000/ws' : '/ws';
export default axios.create({
    baseURL: NODE_ENV === 'development' ? "http://localhost:9000/app" : "/api",
    headers: {
        "Content-type": "application/json"
    }
});
