import React, {Dispatch, useState} from "react";
import Notifications from "./common/Notification";
import {useDispatch, useSelector} from "react-redux";
import MyTable from "./common/MyTable";
import {Service} from "../services/Service";
import {useActions, useMountEffect} from "../hooks/hooks";
import {addNotification} from "../actions/notificationActions";
import {IAppState} from "../index";
import {Button, Col, Form, ListGroup, Row} from "react-bootstrap";
import Icon from "./common/Icon";
import ModalView from "./common/ModalView";
import {EConsumer} from "../models/models";

const Admin: React.FC = () => {
    const actions: Service = useActions(Service);
    const [userName, setUserName] =useState(localStorage.getItem("userName"));
    const dispatch: Dispatch<any> = useDispatch();
    const [expendedSideBar, setExpendedSidebar] = useState(true);
    const user: any = useSelector((state: IAppState) =>
        state.user.data);

    useMountEffect(() => {
        if(userName){
            actions.getAllStrategyByUserName(userName!,(error: any) => dispatch(addNotification("Info", error)));
        }
    });

    return (
        <div className="wrapper">
            <Notifications/>
            <nav id="sidebar" className={expendedSideBar ? "" : "active"}>
                <div className="sidebar-header">
                    <h5>Tview_Parser</h5>
                    <div style={{fontSize: 11}}>v.1.000.000</div>
                </div>
            </nav>

            <div id="content">

                <nav className="navbar navbar-expand-lg navbar-dark bg-secondary">
                    <div className="container-fluid">
                        <button style={{backgroundColor: "rgb(30 30 47 / 22%)"}} onClick={() => {
                            setExpendedSidebar(!expendedSideBar);
                        }} type="button" id="sidebarCollapse" className="btn btn-info">
                            <i className="fas fa-align-left"></i>
                            <span>☰</span>
                        </button>
                        <button className="btn btn-dark d-inline-block d-lg-none ml-auto" type="button"
                                data-toggle="collapse" data-target="#navbarSupportedContent"
                                aria-controls="navbarSupportedContent" aria-expanded="false"
                                aria-label="Toggle navigation">
                            <i className="fas fa-align-justify"></i>
                        </button>

                        <div className="collapse navbar-collapse" id="navbarSupportedContent">
                            <ul className="nav navbar-nav ml-auto">
                                <li className="nav-item active">
                                    {/*<a className="nav-link" href="#">Page</a>*/}
                                </li>
                            </ul>
                        </div>
                    </div>
                </nav>
                {user?.id && (<p className='ps-3 mb-0 bg-dark text-white'>
                    {`Информацию по счёту ${user?.id}: ${user?.cash} RUB. RIH3 => Последняя цена: ${user.lastPrice}.`}
                </p>)}
                <p className='ps-3 mb-0 mt-2'>
                    <Button className="me-2" onClick={() => {
                        actions.sendOrder({"direction": "buy",
                            "name":"test1",
                            "userName":"Test",
                            "ticker": "RIH3",
                            "quantity": 1,
                            "consumer": [EConsumer.TEST]},
                            () => {
                                dispatch(addNotification("Info", 'Успешная покупка'))
                            },
                            (error: any) => {
                                dispatch(addNotification("Info", error))
                            })
                    }} variant={"success"}>
                        <Icon icon={"bi bi-chevron-double-up"} size={15} title={'buy'}/>
                    </Button>
                    <Button className="me-2" onClick={() => {
                        actions.sendOrder({"direction": "sell",
                            "name":"test1",
                            "userName":"Test",
                            "ticker": "RIH3",
                            "quantity": -1,
                            "consumer": [EConsumer.TEST]},
                            () => {
                                dispatch(addNotification("Info", 'Успешная продажа'))
                            },
                            (error: any) => dispatch(addNotification("Info", error)))
                    }} variant={"danger"}
                    >
                        <Icon icon={"bi bi-chevron-double-down"} size={15} title={'sell'}/>
                    </Button>
                    <Button className="me-3" onClick={() => {
                        actions.sendOrder({"direction": "hold",
                            "name":"test1",
                            "userName":"Test",
                            "ticker": "RIH3",
                            "quantity": 0,
                            "consumer": [EConsumer.TEST]},
                            () => {
                                dispatch(addNotification("Info", 'Успешное закрытие сделки'))
                            },
                            (error: any) => dispatch(addNotification("Info", error)))
                    }} variant={"secondary"}>
                        <Icon icon={"bi bi-x-lg"} size={15} title={'Hold'}/>
                    </Button>
                    <Button className="me-1" onClick={() => {
                        actions.clear(userName!,() => {
                                actions.getUserInfo(userName!);
                                dispatch(addNotification("Info", 'Очистили!'));
                            },
                            (error: any) => dispatch(addNotification("Info", error)))
                    }} variant={"secondary"}>
                        <Icon icon={"bi bi-trash3-fill"} size={15} title={'Clear'}/>
                    </Button>
                </p>
                <MyTable/>
                <Row>
                    <Col>
                        <ListGroup style={{height:250, overflowY:"auto"}} className="ps-3">
                            {user?.logs?.reverse()?.map((i: any, index: number) => {
                                return <ListGroup.Item className="bg-secondary"
                                                       key={index}>{`№${index + 1} => ${i}`}</ListGroup.Item>
                            })}
                        </ListGroup>
                    </Col>
                </Row>
                {!userName&&(<ModalView
                    header={'Введите имя:'}
                    show={true}
                    accept={()=>localStorage.setItem("userName",'Test')}
                    cancel={()=>{}}
                    text={ <Form.Control
                        // defaultValue={generateUserName(5)}
                        type="text"
                        placeholder="Имя"/>}/>)}
            </div>
        </div>
    );
};

export default Admin;
