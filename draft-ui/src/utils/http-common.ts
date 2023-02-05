import axios from "axios";

// baseURL:http://localhost:9000/app
//baseURL:/api
export default axios.create({
    baseURL: "http://localhost:9000/app",
    headers: {
        "Content-type": "application/json"
    }
});