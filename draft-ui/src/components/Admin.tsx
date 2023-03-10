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
import {EConsumer, IStrategy, Message} from "../models/models";
import {stringTruncate} from "../utils/utils";
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

let stompClient: any = null;
const Admin: React.FC = () => {
    const actions: Service = useActions(Service);
    const [userName, setUserName] = useState(localStorage.getItem("userName"));
    const dispatch: Dispatch<any> = useDispatch();
    const [expendedSideBar, setExpendedSidebar] = useState(false);
    const [showLogs, setShowLogs] = useState(true);
    const [handleStrategy, setHandleStrategy] = useState<any>();
    const [showFeedBack, setFeedBack] = useState(false);
    const [textFeedBack, setTextFeedBack] = useState<any>();
    const [showDisclaimer, setShowDisclaimer] = useState(false);
    const [notifyView, setNotifyView] = useState<any>({
        show: false,
        id: undefined,
        type: "modal",
        header: undefined,
        message: undefined
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
                actions.setData(payloadData.message, payloadData.command, ()=>{
                    if (payloadData?.command === 'strategy') {
                        dispatch(addNotification("Info", "???????????? ????????????"));
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
            dispatch(addNotification("Info", "?????????? ?????????? ????????????????????!...?????????????????????????? ????????????????"));
        });
    }

    useMountEffect(() => {
        if (userName) {
            actions.getAllStrategyByUserName(userName!, () => {
                actions.getAllTickers();
            }, (error: any) => dispatch(addNotification("Info", error)));
        }

        window.addEventListener("resize", function (e: any) {
            setSize(window.innerWidth);
        });

        document.addEventListener("click", (e: any) => {
            let box = document.getElementById("box")!;
            let bell = document.getElementById("bell")!;
            if (!(e.composedPath().includes(box) || e.composedPath().includes(bell))) {
                if (box.style.opacity === '1') {
                    box.style.height = "0px"
                    box.style.opacity = "0"
                    box.style.zIndex = "0"
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
            }
        }
    }, [userName]);

    return <Container fluid>
        <Notifications/>
        {notifyView.show && (
            <ViewDescriptionNotification
                type={notifyView.type}
                id={notifyView.id}
                message={notifyView.message}
                header={notifyView.header}
                show={notifyView.show}
                cancel={() => {
                    setNotifyView({...notifyView, show: false})
                }}/>
        )}
        {isLoading && (<PreLoad/>)}
        <ModalView accept={() => {
            void actions.feedback(textFeedBack, userName!, () => {
                dispatch(addNotification("Info", '?????? ???????????? ????????????! ???????????????? ????????????...'));
                setFeedBack(false);
            });
        }} cancel={() => setFeedBack(false)} header={'???????????? ????????????:'} show={showFeedBack} text={
            <Form>
                <Form.Group className="mb-3" controlId="exampleForm.ControlTextarea1">
                    <Form.Control value={textFeedBack} onChange={(e: any) => {
                        setTextFeedBack(e.target.value)
                    }} placeholder={'???????????? ?????? ??????????????????????...'} as="textarea" rows={3}/>
                </Form.Group>
            </Form>}/>
        <Row style={{flexWrap: "nowrap"}}>
            {expendedSideBar && (
                <Offcanvas className="s" show={expendedSideBar} onHide={() => setExpendedSidebar(!expendedSideBar)}>
                    <Offcanvas.Header className="text-white"
                                      style={{backgroundColor: "#212529", borderBottom: "2px solid black"}} closeButton>
                        <Offcanvas.Title>
                            <Row>
                                <Col>
                                    <h5>TView_Parser</h5>
                                </Col>
                            </Row>
                            <Row>
                                <Col style={{paddingLeft: 110, marginTop: -16}} className="ms-2">
                                    <span style={{fontSize: 11}}>v.1.000.000</span>
                                </Col>
                            </Row>
                        </Offcanvas.Title>
                    </Offcanvas.Header>
                    <Offcanvas.Body className="bg-custom-1">
                        <Row>
                            <Col>
                                <Button className="ps-1 pt-1" onClick={() => {
                                    actions.clear(userName!, () => {
                                            actions.getUserInfo(userName!);
                                            dispatch(addNotification("Info", '???????????? ??????!'));
                                        },
                                        (error: any) => dispatch(addNotification("Info", error)))
                                }}>
                                    <Icon icon={"bi bi-trash3-fill"} size={15} title={'Clear'}/>
                                    {" ???????????????? ??????"}
                                </Button>
                                {userName === 'Admin' && (<Button className="ps-1" onClick={() => {
                                    actions.reconnectStream(() => {
                                            dispatch(addNotification("Info", '???????????????????????????? ??????????!'));
                                        },
                                        (error: any) => dispatch(addNotification("Info", error)))
                                }}>
                                    <Icon icon={"bi bi-ethernet"} size={15} title={'Clear'}/>
                                    {" ???????????????????????????? ??????????"}
                                </Button>)}
                                {userName === 'Admin' && (<Button className="ps-1" onClick={() => {
                                    actions.saveDataInTable(userName!, () => {
                                            dispatch(addNotification("Info", '??????????????!'));
                                        },
                                        (error: any) => dispatch(addNotification("Info", error)))
                                }}>
                                    <Icon icon={"bi bi-save"} size={15} title={'Save'}/>
                                    {" ?????????????????? ???????????? ?? ????"}
                                </Button>)}
                            </Col>
                        </Row>
                    </Offcanvas.Body>
                </Offcanvas>)}
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
                                        <Nav.Link>
                                            <Button variant={"outline-primary"} onClick={() => {
                                                setExpendedSidebar(!expendedSideBar)
                                            }}>
                                                <Icon icon={"bi bi-list-task"} size={22} color={"white"}
                                                      title={'sideBar'}/>
                                            </Button>
                                        </Nav.Link>
                                        <Nav.Link style={{paddingTop: 19}} className="text-white">
                                            {`???? ${userName || ''}`}
                                        </Nav.Link>
                                    </Nav>
                                    <Form className="d-flex">
                                        <ButtonLink onClick={() => {
                                            setFeedBack(!showFeedBack)
                                        }}>
                                            <Icon icon={"bi bi-question-circle me-2"} color={"white"}
                                                  size={22}
                                                  title={'???????????? ????????????'}
                                                  hoverColor={'lightgreen'}/>
                                        </ButtonLink>
                                        <ButtonLink id="bell" className="icon me-4" onClick={() => {
                                            let box = document.getElementById("box")!;
                                            if (box.style.opacity === '1') {
                                                box.style.height = "0px"
                                                box.style.opacity = "0"
                                                box.style.zIndex = "0"
                                            } else {
                                                box.style.height = "auto"
                                                box.style.maxHeight = "500px"
                                                box.style.opacity = "1";
                                                box.style.overflowY = "auto";
                                                box.style.zIndex = "1000"
                                            }
                                        }}>
                                            <Icon icon={"bi bi-bell"}
                                                  size={22}
                                                  color="white"
                                                  title={''}
                                                  hoverColor={'lightgreen'}/>
                                            <span>{user?.notifications?.length || 0}</span>
                                        </ButtonLink>
                                        <ButtonLink className="icon" onClick={() => {
                                            localStorage.clear()
                                            actions.setDataStrategy([], () => {
                                                setUserName(null)
                                            });
                                        }}>
                                            <Icon icon={'bi bi-box-arrow-right me-1'}
                                                  color="white"
                                                  size={22} title={'??????????'}
                                                  hoverColor={'lightgreen'}/>
                                            <span>{userName ? '??????????' : "??????????"}</span>
                                        </ButtonLink>
                                        <div className="notifications" id="box">
                                            <h2>?????????? ??????????????????????: <span
                                                className="badge rounded-pill bg-danger">{user?.notifications?.length}</span>
                                            </h2>
                                            {user?.notifications?.reverse()?.map((item: any) => {
                                                switch (item.type) {
                                                    case "info":
                                                        return <div className="notifications-item" onClick={() => {
                                                            if (item.typeView === "modal") {
                                                                setNotifyView({
                                                                    ...notifyView,
                                                                    show: true,
                                                                    id: item.id,
                                                                    message: item.message,
                                                                    header: item.header,
                                                                })
                                                            }
                                                        }}>
                                                            <Icon color="lightblue" icon={"bi bi-info-square"} size={32}
                                                                  title={''}/>
                                                            <div className="text">
                                                                <h6 style={{marginBottom: 0}}
                                                                    className="ps-2 pt-1">{stringTruncate(item.message, 60)}</h6>
                                                                <p style={{marginBottom: 0}}
                                                                   className="ps-2">{item.time}</p>
                                                            </div>
                                                        </div>
                                                    case "info_success":
                                                        return <div className="notifications-item" onClick={() => {
                                                            if (item.typeView === "modal") {
                                                                setNotifyView({
                                                                    ...notifyView,
                                                                    show: true,
                                                                    id: item.id,
                                                                    message: item.message,
                                                                    header: item.header,
                                                                })
                                                            }
                                                        }
                                                        }>
                                                            <Icon color="lightgreen" icon={"bi bi-info-square"}
                                                                  size={32} title={''}/>
                                                            <div className="text">
                                                                <h6 style={{marginBottom: 0}}
                                                                    className="ps-2 pt-1">{stringTruncate(item.message, 60)}</h6>
                                                                <p style={{marginBottom: 0}}
                                                                   className="ps-2">{item.time}</p>
                                                            </div>
                                                        </div>
                                                    case "error":
                                                        return <div className="notifications-item" onClick={() => {
                                                            if (item.typeView === "modal") {
                                                                setNotifyView({
                                                                    ...notifyView,
                                                                    show: true,
                                                                    id: item.id,
                                                                    message: item.message,
                                                                    header: item.header,
                                                                })
                                                            }
                                                        }}>
                                                            <Icon color="red" icon={"bi bi-exclamation-square"}
                                                                  size={32}
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
                                    </Form>
                                </Navbar.Collapse>
                            </Container>
                        </Navbar>
                    </Col>
                </Row>
                <RowFiled isAdaptive={size < 917}>
                    <SelectFilter placeholder={'???????????????? ??????????????????'} value={handleStrategy}
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
                                dispatch(addNotification("Info", '???????????????? ?????????????????? ?????? ???????????????????? ???????????? ????????????!..'))
                                return;
                            }
                            actions.sendOrder({
                                    "direction": "buy",
                                    "name": found.name,
                                    "userName": userName!,
                                    "ticker": found.ticker,
                                    "quantity": (found?.currentPosition || 0) + 1,
                                    "consumer": [EConsumer.TEST]
                                }
                                , () => {
                                    dispatch(addNotification("Info", '???????????????? ??????????????'))
                                }
                                , (error: any) => {
                                    dispatch(addNotification("Info", error))
                                })
                        }}
                                variant={"outline-success"}>
                            <Icon icon={"bi bi-chevron-double-up"} size={15} title={'buy'}/>
                            {" ????????????"}
                        </Button>
                        <Button className="me-2" onClick={() => {
                            const found = strategy.find(i => i.name === handleStrategy?.value)!;
                            if (!found) {
                                dispatch(addNotification("Info", '???????????????? ?????????????????? ?????? ???????????????????? ???????????? ????????????!..'))
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
                                    dispatch(addNotification("Info", '???????????????? ??????????????'))
                                },
                                (error: any) => dispatch(addNotification("Info", error)))
                        }} variant={"outline-danger"}
                        >
                            <Icon icon={"bi bi-chevron-double-down"} size={15} title={'sell'}/>
                            {" ??????????????"}
                        </Button>
                        <Button className="me-3" onClick={() => {
                            const found = strategy.find(i => i.name === handleStrategy?.value)!;
                            if (!found) {
                                dispatch(addNotification("Info", '???????????????? ?????????????????? ?????? ???????????????????? ???????????? ????????????!..'))
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
                                    dispatch(addNotification("Info", '???????????????? ???????????????? ????????????'))
                                },
                                (error: any) => dispatch(addNotification("Info", error)))
                        }} variant={"outline-primary"}>
                            <Icon icon={"bi bi-x-lg"} size={15} title={'Hold'}/>
                            {" ??????????????"}
                        </Button>
                    </>
                </RowFiled>
                <Row id="myTable">
                    <Col>
                        <MyTable/>
                    </Col>
                </Row>
                {/*{userName === 'Admin' && (<Row>*/}
                {/*    <Col style={{maxWidth:220}} className="ps-4 pt-2">*/}
                {/*        <Button variant={"outline-dark"} onClick={() => {*/}
                {/*            actions.getCountStreams()*/}
                {/*        }}>{'???????????????????? ??????????????'}</Button>*/}
                {/*    </Col>*/}
                {/*    <Col className="pt-2">*/}
                {/*        <Button variant={"outline-dark"} onClick={() => {*/}
                {/*            actions.unsubscribed()*/}
                {/*        }}>{'?????????????????? ??????????'}</Button>*/}
                {/*    </Col>*/}
                {/*</Row>)}*/}
                <Row>
                    <Col className="ps-4">
                        <div onClick={(e: any) => {
                            // setShowLogs(!showLogs)
                        }} className="text-outline custom-text"
                        >{'????????:'
                            // `????????????????/???????????? ???????? ${showLogs ? '???' : '???'}`
                        }</div>
                    </Col>
                </Row>
                {showLogs && (<Row>
                    <Col>
                        <ListGroup style={{maxHeight: 450, overflowY: "auto", paddingLeft: 10}}>
                            {user?.logs?.reverse()?.map((i: any, index: number) => {
                                return <ListGroup.Item style={{color: "#8C909A"}} className="bg-custom-2"
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
                            }}>{'?????????? ???? ??????????????????????????????'}</span>
                        </ButtonLink>
                    </Col>

                </Row>
                <Offcanvas className="bg-custom-2" placement="bottom" show={showDisclaimer} onHide={() => {
                    setShowDisclaimer(false)
                }}>
                    <Offcanvas.Header closeButton>
                        <Offcanvas.Title className="text-white">?????????? ???? ??????????????????????????????</Offcanvas.Title>
                    </Offcanvas.Header>
                    <Offcanvas.Body className="text-white font-size-sm">
                        {
                            '????????????????????, ???????????????????????? ???? ???????? ??????????, ?????????????????????????????? ???????????? ?? ?????????????????????????????? ?????????? ?? ???? ???????????? ?????????????????????????????? ?????? ???????????????????? ??????????.\n' +
                            '???????????????? ?????????????????? ???? ???????????????????????? ???????????? ??????????????, ?? ???? ???????????? ?????????? ?? ???????????? ?? ???????? ???????????????? ?????????????? ????, ?????????? ?????????????????????????? ?? ??????????.\n' +
                            '?????? ???? ?????????????? ???????????????????? ??????????????????, ???????? ???? ?????????????????? ???? ?????????????????? ???????????????? ????????????????????, ?? ?????????????? ???? ??????????????????, ?? ?????????????? ?????????? ???????????????????????????? ??????????????. \n'}
                        <br/>
                        <br/>
                        {'???????? ???? ???? ?????????????????? ?????????????????? ?????? ??????????, ?????? ?????????????? ???????????????????? ???? ?????????????????????? ?????????????????????????? ?? ???????????? ?????????????????????? ????????????????????????.'}
                    </Offcanvas.Body>
                </Offcanvas>
            </Col>
        </Row>
    </Container>
};

export default Admin;
