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
import moment from "moment";
import {stringTruncate} from "../utils/utils";
import Login from "./common/Login";

const Admin: React.FC = () => {
    const actions: Service = useActions(Service);
    const [userName, setUserName] = useState(localStorage.getItem("userName"));
    const dispatch: Dispatch<any> = useDispatch();
    const [expendedSideBar, setExpendedSidebar] = useState(false);
    const user: any = useSelector((state: IAppState) =>
        state.user.data);

    useMountEffect(() => {
        if (userName) {
            actions.getAllStrategyByUserName(userName!, (error: any) => dispatch(addNotification("Info", error)));
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
                <nav className="navbar navbar-expand-lg">
                    <div className="container-fluid">
                        <button style={{backgroundColor: "rgb(30 30 47 / 22%)"}} onClick={() => {
                            setExpendedSidebar(!expendedSideBar);
                        }} type="button" id="sidebarCollapse" className="btn btn-outline-info">
                            <Icon icon={"bi bi-list-task"} size={15} color={"white"} title={'sideBar'}/>
                        </button>
                        <div className="icon" id="bell">
                            <Icon onClick={() => {
                                let box = document.getElementById("box")!;
                                if (box.style.opacity === '1') {
                                    box.style.height = "0px"
                                    box.style.opacity = "0";
                                } else {
                                    box.style.height = "auto"
                                    box.style.opacity = "1";
                                }
                            }} icon={"bi bi-bell"} size={22} color="white" title={''}/>
                            <span>{user?.notifications?.length}</span>
                        </div>
                        <div className="notifications" id="box">
                            <h2>Всего уведомлений: <span className="badge rounded-pill bg-danger">{user?.notifications?.length}</span></h2>
                            {user?.notifications.map((item: any) => {
                                switch (item.type) {
                                    case "info":
                                        return <div className="notifications-item">
                                            <Icon color="lightblue" icon={"bi bi-info-square"} size={32} title={''}/>
                                            <div className="text">
                                                <h6 style={{marginBottom: 0}}
                                                    className="ps-2 pt-1">{stringTruncate(item.message, 60)}</h6>
                                                <p style={{marginBottom: 0}}
                                                   className="ps-2">{item.time}</p>
                                            </div>
                                        </div>
                                    case "error":
                                        return <div className="notifications-item">
                                            <Icon color="red" icon={"bi bi-exclamation-square"} size={32}
                                                  title={''}/>
                                            <div className="text">
                                                <h6 style={{marginBottom: 0}}
                                                    className="ps-2 pt-1">{stringTruncate(item.message, 60)}</h6>
                                                <p style={{marginBottom: 0}}
                                                   className="ps-2">{item.time}</p>
                                            </div>
                                        </div>
                                }
                            })
                            }
                        </div>
                    </div>
                </nav>
                {user?.id && (<p className='ps-3 mb-0 bg-custom-1 text-white'>
                    {`Информацию по счёту ${user?.id}: ${user?.cash} RUB. RIH3 => Последняя цена: ${user.lastPrice} (${user.lastTimeUpdate})`}
                </p>)}
                <p className='ps-3 mb-0 mt-2'>
                    <Button className="me-2" onClick={() => {
                        actions.sendOrder({
                                "direction": "buy",
                                "name": "test1",
                                "userName": "Test",
                                "ticker": "RIH3",
                                "quantity": 1,
                                "consumer": [EConsumer.TEST]
                            },
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
                        actions.sendOrder({
                                "direction": "sell",
                                "name": "test1",
                                "userName": "Test",
                                "ticker": "RIH3",
                                "quantity": -1,
                                "consumer": [EConsumer.TEST]
                            },
                            () => {
                                dispatch(addNotification("Info", 'Успешная продажа'))
                            },
                            (error: any) => dispatch(addNotification("Info", error)))
                    }} variant={"danger"}
                    >
                        <Icon icon={"bi bi-chevron-double-down"} size={15} title={'sell'}/>
                    </Button>
                    <Button className="me-3" onClick={() => {
                        actions.sendOrder({
                                "direction": "hold",
                                "name": "test1",
                                "userName": "Test",
                                "ticker": "RIH3",
                                "quantity": 0,
                                "consumer": [EConsumer.TEST]
                            },
                            () => {
                                dispatch(addNotification("Info", 'Успешное закрытие сделки'))
                            },
                            (error: any) => dispatch(addNotification("Info", error)))
                    }} variant={"info"}>
                        <Icon icon={"bi bi-x-lg"} size={15} title={'Hold'}/>
                    </Button>
                    <Button className="me-1" onClick={() => {
                        actions.clear(userName!, () => {
                                actions.getUserInfo(userName!);
                                dispatch(addNotification("Info", 'Очищен лог!'));
                            },
                            (error: any) => dispatch(addNotification("Info", error)))
                    }} variant={"dark"}>
                        <Icon icon={"bi bi-trash3-fill"} size={15} title={'Clear'}/>
                    </Button>
                    <Button className="me-1" onClick={() => {
                        actions.reconnectStream(() => {
                                dispatch(addNotification("Info", 'Переподключили стрим!'));
                            },
                            (error: any) => dispatch(addNotification("Info", error)))
                    }} variant={"dark"}>
                        <Icon icon={"bi bi-ethernet"} size={15} title={'Clear'}/>
                    </Button>
                </p>
                <MyTable/>
                <Row>
                    <Col>
                        <ListGroup style={{height: 450, overflowY: "auto"}} className="ps-3">
                            {user?.logs?.reverse()?.map((i: any, index: number) => {
                                return <ListGroup.Item className="bg-custom-2"
                                                       key={index}>{`№${index + 1} => ${i}`}</ListGroup.Item>
                            })}
                        </ListGroup>
                    </Col>
                </Row>
                <Login/>
            </div>
        </div>
    );
};

export default Admin;
