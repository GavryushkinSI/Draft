import React, {Dispatch, useEffect, useState} from "react";
import Notifications from "./common/Notification";
import {useDispatch, useSelector} from "react-redux";
import MyTable from "./common/MyTable";
import {Service} from "../services/Service";
import {useActions, useMountEffect} from "../hooks/hooks";
import {addNotification} from "../actions/notificationActions";
import {IAppState} from "../index";
import {
    Button,
    Col,
    Container,
    Form,
    ListGroup,
    Nav,
    Navbar,
    Offcanvas,
    Row
} from "react-bootstrap";
import Icon from "./common/Icon";
import {EConsumer, EProducer, IStrategy, Message} from "../models/models";
import {getFieldsFromArray, includeInArray, stringTruncate} from "../utils/utils";
import Login from "./common/Login";
import SockJS from "sockjs-client";
import {over} from "stompjs";
import {webSocketUrl} from "../utils/http-common";
import SelectFilter from "./common/SelectFilter";
import ModalView from "./common/ModalView";
import {ButtonLink} from "@paljs/ui";
import RowFiled from "./common/RowFiled";
import PreLoad from "./common/PreLoad";
import ViewDescriptionNotification from "./common/ViewDescriptionNotification";
import MyToolTip from "./common/MyToolTip";
import {useNavigate} from "react-router-dom";
import {ROOT} from "../Route/consts";
import Gap from "./common/Gap";

let stompClient: any = null;
const Admin: React.FC = () => {
    const navigate = useNavigate();
    const actions: Service = useActions(Service);
    const [userName, setUserName] = useState(localStorage.getItem("userName"));
    const dispatch: Dispatch<any> = useDispatch();
    const [showLogs, setShowLogs] = useState(true);
    const [nameStrategyFilter, setNameStrategyFilter] = useState<string>("");
    const [handleStrategy, setHandleStrategy] = useState<any>();
    const [showFeedBack, setFeedBack] = useState(false);
    const [textFeedBack, setTextFeedBack] = useState<any>();
    const [showDisclaimer, setShowDisclaimer] = useState(false);
    const [notifyView, setNotifyView] = useState<any>({
        show: false,
        id: undefined,
        type: "modal",
        header: undefined,
        message: undefined,
        comments: undefined,
    });
    const user: any = useSelector((state: IAppState) =>
        state.user.data);
    const strategy: IStrategy[] = useSelector((state: IAppState) =>
        state.strategy.data);
    const isLoading: boolean = useSelector((state: IAppState) =>
        state.strategy.isLoading);
    const [size, setSize] = useState<any>(window.innerWidth);

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
                actions.setData(payloadData.message, payloadData.command, () => {
                    if (payloadData?.command === 'strategy') {
                        dispatch(addNotification("Info", "Прошла сделка"));
                    }
                });
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
            dispatch(addNotification("Info", "Обрыв сокет соединения!...Обновите страницу!"));
        });
    }

    useMountEffect(() => {
        if (userName) {
            actions.getAllStrategyByUserName(userName!, () => {
                actions.getAllTickers();
            }, (error: any) => dispatch(addNotification("Info", error)));
        }

        let box = document.getElementById("box")!;
        box.style.display = "none"
        box.style.zIndex = "0"

        window.addEventListener("resize", function (e: any) {
            setSize(window.innerWidth);
        });

        document.addEventListener("click", (e: any) => {
            let bell = document.getElementById("bell")!;
            if (!(e.composedPath().includes(box) || e.composedPath().includes(bell))) {
                if (box.style.opacity === '1') {
                    box.style.height = "0px"
                    box.style.opacity = "0"
                    box.style.zIndex = "0"
                    box.style.display = "none"
                }
            }
        });

        return () => {
            // document.removeEventListener("click");
            stompClient?.unsubscribe('/user/' + userName + '/private');
        }
    });

    useEffect(() => {
        if (userName) {
            if (stompClient === null) {
                connect().then(() => {
                    actions.getAllStrategyByUserName(userName!, () => {
                        actions.getAllTickers();
                    }, (error: any) => dispatch(addNotification("Info", error)));
                });
            } else {
                actions.getAllStrategyByUserName(userName!, () => {
                    actions.getAllTickers();
                }, (error: any) => dispatch(addNotification("Info", error)));
            }
        }
    }, [userName]);

    const renderViewNotify = () => {
        const finder = user?.notifications.find((item: any) => item.id === notifyView.id);
        return notifyView.show && (
            <ViewDescriptionNotification
                className={"myQuill"}
                user={userName}
                type={notifyView.type}
                id={notifyView.id}
                message={notifyView.message}
                header={notifyView.header}
                show={notifyView.show}
                comments={finder?.comments}
                commentsBlockEnabled={finder?.blockCommentEnabled}
                cancel={() => {
                    setNotifyView({...notifyView, show: false});
                    actions.setViewedNotifyIds([notifyView.id], userName!);
                }}/>
        )
    }

    return <Container fluid>
        <Notifications/>
        {notifyView?.show && renderViewNotify()}
        {isLoading && (<PreLoad/>)}
        <ModalView accept={() => {
            void actions.feedback(textFeedBack, userName!, () => {
                dispatch(addNotification("Info", 'Ваш вопрос принят! Ожидайте ответа...'));
                setFeedBack(false);
            });
        }} cancel={() => setFeedBack(false)} header={'Задать вопрос:'} show={showFeedBack} text={
            <Form>
                <Form.Group className="mb-3" controlId="exampleForm.ControlTextarea1">
                    <Form.Control value={textFeedBack} onChange={(e: any) => {
                        setTextFeedBack(e.target.value)
                    }} placeholder={'Вопрос или предложение...'} as="textarea" rows={3}/>
                </Form.Group>
            </Form>}/>
        <Row style={{flexWrap: "nowrap"}}>
            <Col style={{maxWidth: 40}} className="bg-custom-1">
                <Row>
                    <Col>
                        <div className="main_logo"
                             style={{color: "white", textShadow: "3px 3px 2px #0d6efd", marginTop: 40}}>TV
                        </div>
                        <hr style={{color: "white", textShadow: "3px 3px 2px #0d6efd", marginTop: 25}}/>
                    </Col>
                </Row>
                <MyToolTip text={'Публичные стратегии'} textInner={<Icon
                    onClick={async () => {
                        navigate(ROOT().DRAFT.PUBLIC_STRATEGY.PATH)
                    }}
                    hoverColor={"lightgreen"} icon={"bi bi-robot"} size={17} title={''}/>}/>
                <Gap/>
                <MyToolTip text={'Очистить лог'} textInner={<Icon onClick={() => {
                    actions.clear(userName!, () => {
                        actions.getUserInfo(userName!)
                    });
                    dispatch(addNotification("Info", 'Очищен лог!'));
                }} hoverColor={"lightgreen"} icon={"bi bi-trash3-fill"} size={17} title={''}/>}/>
                <Gap/>
                {userName === "Admin" && (<>
                    <MyToolTip text={'Переподключить стрим'} textInner={<Icon onClick={() => {
                        actions.reconnectStream(() => {
                            dispatch(addNotification("Info", 'Переподключили стрим!'));
                        }, (error: any) => dispatch(addNotification("Info", error)));
                    }
                    } hoverColor={"lightgreen"} icon={"bi bi-ethernet"} size={17} title={''}/>}/>
                    <Gap/>
                    <MyToolTip text={'Сохранить в БД'} textInner={<Icon onClick={
                        () => {
                            actions.saveDataInTable(userName!, () => {
                                    dispatch(addNotification("Info", 'Успешно!'))
                                },
                                (error: any) => dispatch(addNotification("Info", error)));
                        }
                    } hoverColor={"lightgreen"} icon={"bi bi-save"} size={17} title={''}/>}/>
                    <Gap/>
                    <MyToolTip text={'Перейти в админ-панель'} textInner={<Icon onClick={async () => {
                        navigate(ROOT().DRAFT.ADMIN_PANEL.PATH)
                    }} hoverColor={"lightgreen"} icon={"bi bi-list-columns"} size={17} title={''}/>}/>
                    <Gap/>
                </>)}
                <MyToolTip text={'Статьи'} textInner={<Icon onClick={async () => {
                    navigate(ROOT().DRAFT.PAGE_WITH_ARTICLES.PATH)
                }} hoverColor={"lightgreen"} icon={"bi bi-book"} size={17} title={''}/>}/>
            </Col>
            <Col className="bg-custom-2" style={{height: "100vh"}}>
                <Row style={{flexWrap: "nowrap"}}>
                    <Col style={{paddingTop: 20}}>
                        <Navbar style={{maxHeight: 60}} className="bg-custom-1">
                            <Container fluid>
                                <Navbar.Toggle aria-controls="navbarScroll"/>
                                <Navbar.Collapse id="navbarScroll">
                                    <Nav
                                        className="me-auto my-2 my-lg-0"
                                        navbarScroll
                                    >
                                        {!!userName && (<Nav.Link className="text-white">
                                            <Icon color={"lightblue"} hoverColor={"lightgreen"} icon={"bi bi-person"}
                                                  title={"Имя пользователя"} size={22}/>
                                            <span style={{color: "lightblue"}}>{userName}</span>
                                        </Nav.Link>)}
                                    </Nav>
                                    <Form className="d-flex">
                                        <ButtonLink onClick={() => {
                                            setFeedBack(!showFeedBack)
                                        }}>
                                            <Icon icon={"bi bi-question-circle me-2"} color={"white"}
                                                  size={22}
                                                  title={'Задать вопрос'}
                                                  hoverColor={'lightgreen'}/>
                                        </ButtonLink>
                                        <ButtonLink id="bell" className="icon me-4" onClick={() => {
                                            let box = document.getElementById("box")!;
                                            if (box.style.opacity === '1') {
                                                box.style.height = "0px"
                                                box.style.opacity = "0"
                                                box.style.zIndex = "0"
                                                box.style.display = "none"
                                            } else {
                                                box.style.height = "auto"
                                                box.style.maxHeight = "500px"
                                                box.style.opacity = "1";
                                                box.style.overflowY = "auto";
                                                box.style.zIndex = "1000"
                                                box.style.display = "block"
                                            }
                                        }}>
                                            <Icon icon={"bi bi-bell"}
                                                  size={22}
                                                  color="white"
                                                  title={''}
                                                  hoverColor={'lightgreen'}/>
                                            <span>{(user?.notifications?.length - (user?.viewedNotifyIds?.length || 0)) || 0}</span>
                                        </ButtonLink>
                                        <ButtonLink className="icon" onClick={() => {
                                            localStorage.clear()
                                            actions.setDataStrategy([], () => {
                                                setUserName(null)
                                            });
                                        }}>
                                            <Icon icon={'bi bi-box-arrow-right me-1'}
                                                  color="white"
                                                  size={22} title={'Выйти'}
                                                  hoverColor={'lightgreen'}/>
                                            <span>{userName ? 'Выйти' : "Войти"}</span>
                                        </ButtonLink>
                                        <div className="notifications" id="box">
                                            <h2>Всего уведомлений: <span
                                                className="badge rounded-pill bg-danger">{user?.notifications?.length}</span>
                                            </h2>
                                            {user?.notifications?.map((item: any) => {
                                                let notViewed: boolean = !includeInArray(user?.viewedNotifyIds, item.id);
                                                switch (item.type) {
                                                    case "info":
                                                        return <div
                                                            style={{backgroundColor: notViewed ? "rgb(30 30 47 / 58%)" : ""}}
                                                            className="notifications-item" onClick={() => {
                                                            if (item.typeView === "modal") {
                                                                setNotifyView({
                                                                    ...notifyView,
                                                                    show: true,
                                                                    id: item.id,
                                                                    message: item.message,
                                                                    header: item.header,
                                                                    comments: item.comments,
                                                                })
                                                            }
                                                        }}>
                                                            <Icon color="lightblue" icon={"bi bi-info-square"} size={32}
                                                                  title={''}/>
                                                            <div className="text">
                                                                <h6 style={{marginBottom: 0}}
                                                                    className="ps-2 pt-1">{stringTruncate(item.header, 60)}</h6>
                                                                <p style={{marginBottom: 0}}
                                                                   className="ps-2">{item.time}</p>
                                                            </div>
                                                        </div>
                                                    case "info_success":
                                                        return <div
                                                            style={{backgroundColor: notViewed ? "rgb(30 30 47 / 58%)" : ""}}
                                                            className="notifications-item" onClick={() => {
                                                            if (item.typeView === "modal") {
                                                                setNotifyView({
                                                                    ...notifyView,
                                                                    show: true,
                                                                    id: item.id,
                                                                    message: item.message,
                                                                    header: item.header,
                                                                    comments: item.comments,
                                                                })
                                                            }
                                                        }
                                                        }>
                                                            <Icon color="lightgreen" icon={"bi bi-info-square"}
                                                                  size={32} title={''}/>
                                                            <div className="text">
                                                                <h6 style={{marginBottom: 0}}
                                                                    className="ps-2 pt-1">{stringTruncate(item.header, 60)}</h6>
                                                                <p style={{marginBottom: 0}}
                                                                   className="ps-2">{item.time}</p>
                                                            </div>
                                                        </div>
                                                    case "error":
                                                        return <div
                                                            style={{backgroundColor: notViewed ? "rgb(30 30 47 / 58%)" : ""}}
                                                            className="notifications-item" onClick={() => {
                                                            if (item.typeView === "modal") {
                                                                setNotifyView({
                                                                    ...notifyView,
                                                                    show: true,
                                                                    id: item.id,
                                                                    message: item.message,
                                                                    header: item.header,
                                                                    comments: item.comments,
                                                                })
                                                            }
                                                        }}>
                                                            <Icon color="red" icon={"bi bi-exclamation-square"}
                                                                  size={32}
                                                                  title={''}/>
                                                            <div className="text">
                                                                <h6 style={{marginBottom: 0}}
                                                                    className="ps-2 pt-1">{stringTruncate(item.header, 60)}</h6>
                                                                <p style={{marginBottom: 0}}
                                                                   className="ps-2">{item.time}</p>
                                                            </div>
                                                        </div>
                                                }
                                            })
                                            }
                                        </div>
                                    </Form>
                                </Navbar.Collapse>
                            </Container>
                        </Navbar>
                    </Col>
                </Row>
                <RowFiled isAdaptive={size < 917}>
                    <SelectFilter placeholder={'Выберите стратегию'} value={handleStrategy}
                                  options={strategy?.map(i => {
                                      return {value: i.name, label: i.name}
                                  }) || []}
                                  style={{
                                      maxWidth: 240,
                                      minWidth: 240,
                                      display: "inline-block",
                                      marginBottom: size < 917 ? 8 : 0
                                  }}
                                  onChange={(e: any) => {
                                      setHandleStrategy(e.target.value ? {
                                          value: e.target.value,
                                          label: e.target.value
                                      } : null)
                                  }}/>
                    <>
                        <Button className="me-2" onClick={() => {
                            const found = strategy?.find(i => i.name === handleStrategy?.value)!;
                            if (!found) {
                                dispatch(addNotification("Info", 'Выберите стратегию для проведения ручной сделки!..'))
                                return;
                            }
                            actions.sendOrder({
                                    "direction": "buy",
                                    "producer":found.producer,
                                    "name": found.name,
                                    "userName": userName!,
                                    "ticker": found.ticker,
                                    "quantity": found.minLot!,
                                }
                                , () => {}
                                , (error: any) => {
                                    dispatch(addNotification("Info", error))
                                })
                        }}
                                variant={"outline-success"}>
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
                                    "producer":found.producer,
                                    "name": found.name,
                                    "userName": userName!,
                                    "ticker": found.ticker,
                                    "quantity": found.minLot!,
                                },
                                () => {},
                                (error: any) => dispatch(addNotification("Info", error)))
                        }} variant={"outline-danger"}
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
                                    "direction": (found?.currentPosition||0)>0?"sell":"buy",
                                    "producer":found.producer,
                                    "name": found.name,
                                    "userName": userName!,
                                    "ticker": found.ticker,
                                    "quantity": found?.currentPosition || 0
                                },
                                () => {},
                                (error: any) => dispatch(addNotification("Info", error)))
                        }} variant={"outline-primary"}>
                            <Icon icon={"bi bi-x-lg"} size={15} title={'Hold'}/>
                            {" Закрыть"}
                        </Button>
                    </>
                </RowFiled>
                <Row id="myTable">
                    <Col>
                        <MyTable/>
                    </Col>
                </Row>
                <Row>
                    <Col className="ps-4">
                        <div onClick={(e: any) => {
                            // setShowLogs(!showLogs)
                        }} className="text-outline custom-text"
                        >{'Логи (priceTv - цена срабатывания ордера на Tradingview): '}</div>
                    </Col>
                </Row>
                <Row>
                    <Col>
                <Form.Select onChange={(e) => {
                    setNameStrategyFilter(e.target.value === 'all' ? "" : e.target.value)
                }} className="ms-2 mb-1" name="filter_strategy" style={{width: 240}}>
                    <option key={'12345'} value={'all'}>{'Все'}</option>
                    {strategy?.map((i) => {
                        return <option key={i.id} value={i.name}>{i.name}</option>
                    })}
                </Form.Select>
                    </Col>
                </Row>
                {showLogs && (<Row>
                    <Col>
                        <ListGroup style={{maxHeight: 200, overflowY: "auto", paddingLeft: 10}}>
                            {user?.logs?.filter((i: any) => i.startsWith(nameStrategyFilter))?.map((i: any, index: number) => {
                                return <ListGroup.Item style={{color: "#8C909A", cursor: "pointer"}}
                                                       className="bg-custom-2"
                                                       key={index}>{`${i}`}</ListGroup.Item>
                            })}
                        </ListGroup>
                    </Col>
                </Row>)}
                <Login userName={userName} setUserName={(value: string) => {
                    localStorage.setItem("userName", value);
                    setUserName(value);
                }}/>
                <Row style={{
                    position: "absolute",
                    bottom: 0
                }} className="mt-auto">
                    <Col>
                        <ButtonLink onClick={() => setShowDisclaimer(true)}>
                            <span style={{
                                color: "lightgray",
                                cursor: "pointer",
                                paddingLeft: 10,
                            }}>{'Отказ от ответственности'}</span>
                        </ButtonLink>
                    </Col>

                </Row>
                <Offcanvas className="bg-custom-3" placement="bottom" show={showDisclaimer} onHide={() => {
                    setShowDisclaimer(false)
                }}>
                    <Offcanvas.Header closeButton>
                        <Offcanvas.Title className="text-white">Отказ от ответственности</Offcanvas.Title>
                    </Offcanvas.Header>
                    <Offcanvas.Body className="text-white font-size-sm">
                        {
                            'Информация, содержащаяся на этом сайте, предоставляется только в образовательных целях и не должна рассматриваться как финансовый совет.\n' +
                            'Торговля сопряжена со значительным риском убытков, и вы должны знать о рисках и быть готовыми принять их, чтобы инвестировать в рынки.\n' +
                            'Вам не следует заниматься торговлей, если вы полностью не понимаете характер транзакций, в которые вы вступаете, и степень вашей подверженности убыткам. \n'}
                        <br/>
                        <br/>
                        {'Если вы не полностью понимаете эти риски, вам следует обратиться за независимой консультацией к своему финансовому консультанту.'}
                    </Offcanvas.Body>
                </Offcanvas>
            </Col>
        </Row>
    </Container>
};

export default Admin;
