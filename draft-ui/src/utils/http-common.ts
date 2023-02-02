import axios from "axios";

// baseURL:http://localhost:9000/app
//baseURL:/api
export default axios.create({
    baseURL: "/api",
    headers: {
        "Content-type": "application/json"
    }
});