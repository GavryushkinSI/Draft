import React, {Dispatch, useEffect, useState} from "react";
import Notifications from "./common/Notification";
import {useDispatch, useSelector} from "react-redux";
import MyTable from "./common/MyTable";
import {Service} from "../services/Service";
import {useActions, useMountEffect} from "../hooks/hooks";
import {addNotification} from "../actions/notificationActions";
import {IAppState} from "../index";
import {Accordion, Button, Col, Form, ListGroup, Row} from "react-bootstrap";
import Icon from "./common/Icon";
import {EConsumer, IStrategy, Message} from "../models/models";
import {stringTruncate} from "../utils/utils";
import Login from "./common/Login";
import SockJS from "sockjs-client";
import {over} from "stompjs";
import {webSocketUrl} from "../utils/http-common";
import SelectFilter from "./common/SelectFilter";
import {useResolvedPath} from "react-router-dom";
import ModalView from "./common/ModalView";

let stompClient: any = null;
const Admin: React.FC = () => {
    const actions: Service = useActions(Service);
    const [userName, setUserName] = useState(localStorage.getItem("userName"));
    const dispatch: Dispatch<any> = useDispatch();
    const [expendedSideBar, setExpendedSidebar] = useState(false);
    const [showLogs, setShowLogs] = useState(false);
    const [handleStrategy, setHandleStrategy] = useState<any>();
    const [showFeedBack, setFeedBack] = useState(false);
    const user: any = useSelector((state: IAppState) =>
        state.user.data);
    const strategy: IStrategy[] = useSelector((state: IAppState) =>
        state.strategy.data);

    const userJoin = () => {
        const message = {
            senderName: userName,
            status: 'JOIN',
            command: 'OPEN_WEBSOCKET',
        };
        stompClient.send("/app/message", {}, JSON.stringify(message));
    }

    const onConnected = () => {
        stompClient.subscribe('/user/' + userName + '/private', onMessageReceived);
        userJoin();
    }

    const onMessageReceived = (payload: any) => {
        const payloadData: Message = JSON.parse(payload.body);
        switch (payloadData.status) {
            case "JOIN":
                actions.setDataStrategy(payloadData.message);
                break;
            case "MESSAGE":
                break;
        }
    }

    const connect = async () => {
        let Sock = new SockJS(webSocketUrl);
        stompClient = over(Sock);
        stompClient.debug = null;
        await stompClient.connect({}, onConnected, (error: any) => {
            console.log("error", error)
        });
    }

    useMountEffect(() => {
        if (userName) {
            actions.getAllStrategyByUserName(userName!, () => {
                actions.getAllTickers();
                document.addEventListener("click", (e: any) => {
                    let box = document.getElementById("box")!;
                    let bell = document.getElementById("bell")!;
                    if (!(e.composedPath().includes(box) || e.composedPath().includes(bell))) {
                        if (box.style.opacity === '1') {
                            box.style.height = "0px"
                            box.style.opacity = "0"
                        }
                    }
                });
            }, (error: any) => dispatch(addNotification("Info", error)));
        }

        return () => {
            stompClient?.unsubscribe('/user/' + userName + '/private');
        }
    })

    useEffect(() => {
        if (userName) {
            if (stompClient === null) {
                connect().then(() => {
                    actions.getAllStrategyByUserName(userName!, () => {
                        actions.getAllTickers();
                    }, (error: any) => dispatch(addNotification("Info", error)));
                });
            }
        }
    }, [userName]);

    return (
        <div className="wrapper">
            <Notifications/>
            <ModalView cancel={() => setFeedBack(false)} header={'Задать вопрос:'} show={showFeedBack} text={
                <Form>
                    <Form.Group className="mb-3" controlId="exampleForm.ControlTextarea1">
                        <Form.Control placeholder={'Вопрос или предложение...'} as="textarea" rows={3}/>
                    </Form.Group>
                </Form>}/>
            <nav id="sidebar" className={expendedSideBar ? "" : "active"}>
                <div className="sidebar-header">
                    <h5>Tview_Parser</h5>
                    <div style={{fontSize: 11}}>v.1.000.000</div>
                </div>
                <Button className="ps-3" onClick={() => {
                    actions.clear(userName!, () => {
                            actions.getUserInfo(userName!);
                            dispatch(addNotification("Info", 'Очищен лог!'));
                        },
                        (error: any) => dispatch(addNotification("Info", error)))
                }}>
                    <Icon icon={"bi bi-trash3-fill"} size={15} title={'Clear'}/>
                    {" Очистить лог"}
                </Button>
                <Button className="ps-3" onClick={() => {
                    actions.reconnectStream(() => {
                            dispatch(addNotification("Info", 'Переподключили стрим!'));
                        },
                        (error: any) => dispatch(addNotification("Info", error)))
                }}>
                    <Icon icon={"bi bi-ethernet"} size={15} title={'Clear'}/>
                    {" Переподключить стрим"}
                </Button>
            </nav>

            <div id="content">
                <nav className="navbar navbar-expand-lg">
                    <div className="container-fluid">
                        <button style={{backgroundColor: "rgb(30 30 47 / 22%)"}} onClick={() => {
                            setExpendedSidebar(!expendedSideBar);
                        }} type="button" id="sidebarCollapse" className="btn btn-outline-info">
                            <Icon icon={"bi bi-list-task"} size={15} color={"white"} title={'sideBar'}/>
                        </button>
                        {userName && (<span style={{color: "white", position: "absolute", marginLeft: 50}}>
                            {`👤 Пользователь: ${userName}`}
                        </span>)}
                        <div className="icon" id="bell">
                            <Icon onClick={() => {
                                setFeedBack(!showFeedBack)
                            }} icon={"bi bi-question-circle me-2"} color={"white"} size={22} title={'Задать вопрос'}
                                  hoverColor={'lightgreen'}/>
                            <Icon onClick={() => {
                                let box = document.getElementById("box")!;
                                if (box.style.opacity === '1') {
                                    box.style.height = "0px"
                                    box.style.opacity = "0"
                                } else {
                                    box.style.height = "auto"
                                    box.style.maxHeight = "500px"
                                    box.style.opacity = "1";
                                    box.style.overflowY = "auto";
                                }
                            }} icon={"bi bi-bell"} size={22} color="white" title={''} hoverColor={'lightgreen'}/>
                            <span>{user?.notifications?.length || 0}</span>
                            {userName && (<a onClick={() => {
                                localStorage.clear();
                                actions.setDataStrategy([], () => {
                                    setUserName(null)
                                });
                            }} style={{color: "white", cursor: "pointer"}} className="ms-4">
                                <Icon icon={'bi bi-box-arrow-right me-1'} size={22} title={'Выйти'}
                                      hoverColor={'lightgreen'}/>
                                <span>{'Выйти'}</span>
                            </a>)}
                        </div>
                        <div className="notifications" id="box">
                            <h2>Всего уведомлений: <span
                                className="badge rounded-pill bg-danger">{user?.notifications?.length}</span></h2>
                            {user?.notifications?.reverse()?.map((item: any) => {
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
                                    case "info_success":
                                        return <div className="notifications-item">
                                            <Icon color="lightgreen" icon={"bi bi-info-square"} size={32} title={''}/>
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
                <div className='mb-0 mt-2 ps-3'>
                    <SelectFilter placeholder={'Выберите стратегию'} value={handleStrategy}
                                  options={strategy?.map(i => {
                                      return {value: i.name, label: i.name}
                                  }) || []}
                                  style={{maxWidth: 240, minWidth: 240, display: "inline-block"}}
                                  onChange={(e: any) => {
                                      setHandleStrategy(e.target.value ? {
                                          value: e.target.value,
                                          label: e.target.value
                                      } : null)
                                  }}/>
                    <Button className="me-2" onClick={() => {
                        const found = strategy?.find(i => i.name === handleStrategy?.value)!;
                        if (!found) {
                            dispatch(addNotification("Info", 'Выберите стратегию для проведения ручной сделки!..'))
                            return;
                        }
                        actions.sendOrder({
                                "direction": "buy",
                                "name": found.name,
                                "userName": userName!,
                                "ticker": found.ticker,
                                "quantity": (found?.currentPosition || 0) + 1,
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
                        {" Купить"}
                    </Button>
                    <Button className="me-2" onClick={() => {
                        const found = strategy.find(i => i.name === handleStrategy?.value)!;
                        if (!found) {
                            dispatch(addNotification("Info", 'Выберите стратегию для проведения ручной сделки!..'))
                            return;
                        }
                        actions.sendOrder({
                                "direction": "sell",
                                "name": found.name,
                                "userName": userName!,
                                "ticker": found.ticker,
                                "quantity": (found?.currentPosition || 0) - 1,
                                "consumer": [EConsumer.TEST]
                            },
                            () => {
                                dispatch(addNotification("Info", 'Успешная продажа'))
                            },
                            (error: any) => dispatch(addNotification("Info", error)))
                    }} variant={"danger"}
                    >
                        <Icon icon={"bi bi-chevron-double-down"} size={15} title={'sell'}/>
                        {" Продать"}
                    </Button>
                    <Button className="me-3" onClick={() => {
                        const found = strategy.find(i => i.name === handleStrategy?.value)!;
                        if (!found) {
                            dispatch(addNotification("Info", 'Выберите стратегию для проведения ручной сделки!..'))
                            return;
                        }
                        actions.sendOrder({
                                "direction": "hold",
                                "name": found.name,
                                "userName": userName!,
                                "ticker": found.ticker,
                                "quantity": 0,
                                "consumer": [EConsumer.TEST]
                            },
                            () => {
                                dispatch(addNotification("Info", 'Успешное закрытие сделки'))
                            },
                            (error: any) => dispatch(addNotification("Info", error)))
                    }} variant={"info"}>
                        <Icon icon={"bi bi-x-lg"} size={15} title={'Hold'}/>
                        {" Закрыть"}
                    </Button>
                </div>
                <div className={"test"}>
                    <MyTable/>
                </div>
                <Row>
                    <Col>
                        <a onClick={(e: any) => {
                            setShowLogs(!showLogs)
                        }} className="ms-3 text-dark custom-text"
                           href={'#'}>{`Показать/Скрыть логи ${showLogs ? '▽' : '△'}`}</a>
                    </Col>
                </Row>
                {showLogs && (<Row>
                    <Col>
                        <ListGroup style={{height: 450, overflowY: "auto", color: "rgb(140, 144, 154)"}}
                                   className="ps-3">
                            {user?.logs?.reverse()?.map((i: any, index: number) => {
                                return <ListGroup.Item className="bg-custom-2" key={index}>{`${i}`}</ListGroup.Item>
                            })}
                        </ListGroup>
                    </Col>
                </Row>)}
                <Login userName={userName} setUserName={(value: string) => {
                    localStorage.setItem("userName", value);
                    setUserName(value);
                }}/>
            </div>
        </div>
    );
};

export default Admin;
