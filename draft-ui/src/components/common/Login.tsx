import ModalView from "./ModalView";
import {Button, ButtonGroup, Form} from "react-bootstrap";
import React, {useState} from "react";
import {Service} from "../../services/Service";
import {useActions} from "../../hooks/hooks";
import bcrypt from "bcryptjs-react";
import {isEmpty} from "lodash";

interface IProps {
    userName: string | undefined | null;
    setUserName: (value: string) => void;
}

const Login = (props: IProps) => {
    const actions: Service = useActions(Service)
    const [auth, setAuth] = useState<any>({
        login: '',
        password: '',
        email: '',
        status: !!localStorage.getItem("userName") ? "enter" : "reg"
    });
    const [error, setError] = useState<any>();
    const [validation, setValidation] = useState<any>({
        login: 'Поле обязательно для заполнения!',
        password: 'Поле обязательно для заполнения!',
        email: auth.status === 'reg' ? 'Поле обязательно для заполнения!' : undefined,
    });

    const onChange = (e: any) => {
        const {name, value} = e.target;
        setAuth({...auth, [name]: value});
        if (!value) {
            setValidation({...validation, [name]: 'Поле обязательно для заполнения!'});
            return;
        }
        if (name === 'email' && !/@/.test(value)) {
            setValidation({...validation, [name]: 'Некорректный email'});
            return;
        }
        setValidation({...validation, [name]: undefined});
    }

    const accept = (e: any) => {
        const salt = bcrypt.genSaltSync(10)
        const hash = bcrypt.hashSync(auth.password, salt);
        actions.login({login: auth.login, password: hash, email: auth.email}, auth.status).then((response) => {
                if (auth.status === 'enter') {
                    const hash: string = response.data;
                    bcrypt.compare(auth.password, hash).then((response) => {
                        if (response) {
                            props.setUserName(auth.login);
                            actions.getUserInfo(auth.login);
                        } else {
                            setError("Логин или пароль неверный!")
                        }
                    });
                }else{
                    props.setUserName(auth.login);
                    actions.getUserInfo(auth.login);
                }
                actions.setLoading(false);
                setError(null);
            }
        ).catch((error) => {
            console.log(error);
            actions.setLoading(false);
            setError(error.response.data);
        });
    }

    const content: JSX.Element = (<><Form>
        {error && (<div className="alert alert-danger">
            <strong>{error}</strong>
        </div>)}
        <Form.Control
            type="text"
            name="login"
            value={auth.login}
            onChange={onChange}
            placeholder="Имя пользователя"
            isInvalid={!isEmpty(validation.login)}
        />
        <Form.Control.Feedback type="invalid">{validation.login}</Form.Control.Feedback>
        <br/>
        <Form.Control
            type="text"
            name="password"
            value={auth.password}
            onChange={onChange}
            placeholder="Пароль"
            isInvalid={!isEmpty(validation.password)}
        />
        <Form.Control.Feedback type="invalid">{validation.password}</Form.Control.Feedback>
        {auth.status === "reg" && (<>
                <br/>
                <Form.Control
                    type="text"
                    name="email"
                    onChange={onChange}
                    value={auth.email}
                    placeholder="Email"
                    isInvalid={!isEmpty(validation.email)}
                />
                <Form.Control.Feedback type="invalid">{validation.email}</Form.Control.Feedback>
            </>
        )}
        <br/>
    </Form>
        <Button className="me-3" disabled={Object.values(validation).some((i: any) => {
            return !!i;
        })}
                name="reg"
                onClick={(e: any) => {
                    accept(e)
                }} variant={"outline-info"}>
            {auth.status === "reg" ? 'Регистрация' : 'Вход'}
        </Button>
        <a onClick={() => {
            setValidation({
                ...validation,
                email: auth.status === "reg" ? undefined : "Поле обязательно для заполнения"
            });
            setAuth({...auth, status: auth.status === "reg" ? "enter" : "reg"});
            setError(undefined);
        }} style={{cursor: "pointer", textDecoration: "underline", fontSize:12}}>
            {auth.status === "reg" ? 'Уже есть аккаунт нажмите здесь...' : 'Перейти на форму регистрации...'}
        </a>
    </>);

    return <ModalView
        header={auth.status === "reg" ? 'Регистрация' : 'Вход'}
        show={!props.userName}
        text={content}/>;
}

export default Login;
