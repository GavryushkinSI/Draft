import ModalView from "./ModalView";
import {Form} from "react-bootstrap";
import React, {useState} from "react";
import {Service} from "../../services/Service";
import {useActions} from "../../hooks/hooks";
// const bcrypt = require('bcrypt')

const Login = () => {
    const actions: Service = useActions(Service)
    const [userName, setUserName] = useState(localStorage.getItem("userName"))
    const [auth, setAuth] = useState<any>({login: 'Test', password: '', email: ''});
    const [error, setError] = useState<any>();

    const onChange = (e: any) => {
        setAuth({...auth, [e.target.name]: e.target.value});
    }

    const content: JSX.Element = (<Form>
        {error && (<div className="alert alert-danger">
            <strong>{error}</strong>
        </div>)}
        <Form.Control
            type="text"
            name="login"
            value={auth.login}
            onChange={onChange}
            className="mb-2"
            placeholder="Имя"/>
        {/*<Form.Control*/}
        {/*    type="text"*/}
        {/*    name="password"*/}
        {/*    value={auth.password}*/}
        {/*    onChange={onChange}*/}
        {/*    className="mb-2"*/}
        {/*    placeholder="Пароль"/>*/}
        {/*<Form.Control*/}
        {/*    type="text"*/}
        {/*    name="email"*/}
        {/*    onChange={onChange}*/}
        {/*    value={auth.email}*/}
        {/*    placeholder="Почта"/>*/}
    </Form>);

    const accept = () => {
        // const salt = bcrypt.genSaltSync(10)
        // const hash = bcrypt.hashSync(auth.password, salt);
        actions.login(auth).then(() => {
                localStorage.setItem("userName", auth.login);
                setUserName(auth.login);
            }
        ).catch((error) => {
            if (error.response.data.message === 'AUTH_FAIL') {
                setError("Указан неверный пароль!");
            }
        });
    }

    return <ModalView
        header={'Введите имя: '}
        show={!userName}
        accept={() => {
            accept()
        }}
        text={content}/>;
}

export default Login;
