import * as React from "react";
import {useEffect, useMemo, useRef, useState} from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css"
import {Button, ButtonGroup, Col, Form, FormText, InputGroup, Row, Tab, Table, Tabs} from "react-bootstrap";
import {useActions, useMountEffect, useShallowEqualSelector} from "../hooks/hooks";
import {Service} from "../services/Service";
import {IAppState} from "../index";
import {cloneDeep, filter, max, min, toNumber} from "lodash"
import moment from 'moment';
import SockJS from "sockjs-client";
import {over} from 'stompjs';
import {Message} from "../models/models";
import CanvasJSReact from '../libraries/canvasjs.stock.react';
import '../styles/common.css';
import {generateUserName, getStrategyEstimate, profitFactor, saveRow} from "../utils/utils";
import BackTestTab from "./BackTestTab";
import ModalView from "./common/ModalView";
import {STRATEGY_MAP} from "../backtest/params";
// import {calcMagicTrend} from "../strategies/strategy";
import Icon from "./common/Icon";

const SMA = require('technicalindicators').SMA;
const ATR = require('technicalindicators').ATR;
const CCI = require('technicalindicators').CCI;
const userName = generateUserName(5);
const CanvasJSStockChart = CanvasJSReact.CanvasJSStockChart;

interface ISelectedState {
    renko: any;
    lastTimeUpdate:string;
    lastPrice:any;
}

let stompClient: any = null;
const Renko = () => {
    const actions: Service = useActions(Service);

    const {renko, lastTimeUpdate, lastPrice} = useShallowEqualSelector<ISelectedState>(selectState);
    const [viewTicker, setViewTicker] = useState('FUTRTS032300');
    const [startDate, setStartDate] = useState(moment(new Date()).subtract(3, "days").format());
    const [brickSize, setBrickSize] = useState(150);
    const [dataCalc, setDataCalc] = useState<any>([]);
    const [strategy, setStrategy] = useState<any>();
    const [showSetStrategy, setShowSetStrategy] = useState(false);

    // const connect = async () => {
    //     actions.doTest();
    //     let Sock = new SockJS('/ws');
    //     stompClient = over(Sock);
    //     stompClient.debug = null;
    //     await stompClient.connect({}, onConnected, (error: any) => {
    //         console.log("error", error)
    //     });
    // }
    //
    // useMountEffect(() => {
    //     if (stompClient === null) {
    //         connect().then(() => {
    //             setTimeout(() => {
    //                 getDataForTicker(viewTicker);
    //                 const paramTestStrategy = STRATEGY_MAP['strategy2'];
    //                 setStrategy(paramTestStrategy);
    //             }, 500)
    //         });
    //     }
    //
    //     return () => {
    //         stompClient?.unsubscribe('/user/' + userName + '/private');
    //     }
    // })
    //
    // useEffect(() => {
    //     if (renko.data.length > 2) {
    //         renderRenko(brickSize, renko.data);
    //     }
    // }, [renko.data, brickSize])
    //
    // const renderSetStrategy = useMemo(() => {
    //     let content: JSX.Element[] = [];
    //     let key: number = 0;
    //     strategy?.param.forEach((item: any, index: number) => {
    //         if (item.type === 'step') {
    //             content.push(
    //                 <InputGroup key={key++} className="mb-3">
    //                     <InputGroup.Text>{item.name}</InputGroup.Text>
    //                     <InputGroup.Text>{'DefaultValue:'}</InputGroup.Text>
    //                     <Form.Control onChange={(e: any) => {
    //                         const defaultValue = toNumber(e.target.value);
    //                         const clone = cloneDeep(strategy);
    //                         const paramChanging = clone.param.find((i: any) => i.name === item.name);
    //                         setStrategy({...clone, param: saveRow({...paramChanging, defaultValue}, clone.param)});
    //                     }} value={item.defaultValue}/>
    //                 </InputGroup>)
    //         }
    //     });
    //     return <ModalView
    //         show={showSetStrategy}
    //         header={`Параметры: ${strategy?.name}`}
    //         accept={() => {
    //             setShowSetStrategy(false)
    //         }}
    //         cancel={() => {
    //             setShowSetStrategy(false)
    //         }}
    //         text={(<>{content}</>)}/>
    // }, [strategy, showSetStrategy]);
    //
    // const getDataForTicker = (ticker: string) => {
    //     const message = {
    //         senderName: userName,
    //         status: 'JOIN',
    //         message: ticker,
    //         command: 'SUBSCRIPTION_ON_TICKER',
    //     };
    //
    //     stompClient.send("/app/message", {}, JSON.stringify(message));
    //     setViewTicker(ticker);
    // }
    //
    // const onConnected = () => {
    //     stompClient.subscribe('/user/' + userName + '/private', onMessageReceived);
    //     userJoin();
    // }
    //
    // const onMessageReceived = (payload: any) => {
    //     const payloadData: Message = JSON.parse(payload.body);
    //     switch (payloadData.status) {
    //         case "JOIN":
    //             actions.setData(payloadData.message, payloadData.command);
    //             break;
    //         case "MESSAGE":
    //             break;
    //     }
    // }
    //
    // const userJoin = () => {
    //     const message = {
    //         senderName: userName,
    //         status: 'JOIN',
    //         command: 'ADD_USER',
    //     };
    //     stompClient.send("/app/message", {}, JSON.stringify(message));
    // }
    //
    // const renderRenko = (brickSize: number, data: any, isTest: boolean = false) => {
    //     let count = 0;
    //     let newDataPoints: any[] = [];
    //     let testData = [];
    //     let oldValue = data[0].y[3];
    //     let oldValueDate;
    //
    //     try {
    //         oldValueDate = moment(data[0].x).toLocaleString();
    //     } catch (errors) {
    //         console.log(data[0].x)
    //     }
    //     let difference = 0;
    //     let newValue, dataPoint, xValue, prevData;
    //     for (let i = 1; i < data.length; i++) {
    //         dataPoint = data[i].y[3];
    //         prevData = newDataPoints.slice(-1)[0];
    //         try {
    //             xValue = moment(data[i].x).toLocaleString();
    //         } catch (errors) {
    //             //console.log("error",CanvasJS.formatDate("2022-12-13 09:00:00", "YYYY-MMM-DD hh:mm:ss", undefined));
    //         }
    //         difference = dataPoint - oldValue;
    //         if (difference > 0 && difference > brickSize) {
    //             for (let j = 0; j < Math.floor(difference / brickSize); j++) {
    //                 newValue = oldValue + brickSize;
    //                 //console.log(CanvasJS.formatDate(oldValueDate, "YYYY-MM-DD hh:mm:ss", undefined),CanvasJS.formatDate(data[i].appl, "YYYY-MM-DD hh:mm:ss", undefined));
    //                 // if (CanvasJS.formatDate(oldValueDate, "YYYY-MM-DD hh:mm:ss", undefined) === CanvasJS.formatDate(data[i].appl, "YYYY-MM-DD hh:mm:ss", undefined)) {
    //                 //     newDataPoints.push({label: xValue, y: [oldValue, newValue], color: "#0700e4"});
    //                 //     testData.push({label: xValue, y: [oldValue, newValue, oldValue, newValue], color: "#0700e4"});
    //                 // } else {
    //                 newDataPoints.push({
    //                     label: xValue,
    //                     x: count++,
    //                     y: [oldValue, newValue, oldValue, newValue],
    //                     color: "green",
    //                     // indexLabel:"Покупка"
    //                 });
    //                 testData.push({
    //                     label: xValue,
    //                     y: [oldValue, newValue, oldValue, newValue],
    //                     x: i,
    //                 });
    //                 // }
    //                 oldValue = newValue;
    //                 oldValueDate = data[i].x;
    //             }
    //         } else if (difference < 0 && Math.abs(difference) > brickSize) {
    //             for (let j = 0; j < Math.floor(Math.abs(difference) / brickSize); j++) {
    //                 newValue = oldValue - brickSize;
    //                 // if (CanvasJS.formatDate(oldValueDate, "YYYY-MM-DD hh:mm:ss", undefined) === CanvasJS.formatDate(data[i].appl, "YYYY-MM-DD hh:mm:ss", undefined)) {
    //                 //     newDataPoints.push({label: xValue, y: [oldValue, newValue], color: "#0700e4"});
    //                 //     testData.push({label: xValue, y: [oldValue, oldValue, newValue, newValue], color: "#0700e4"});
    //                 // } else {
    //                 newDataPoints.push({
    //                     label: xValue,
    //                     x: count++,
    //                     y: [oldValue, oldValue, newValue, newValue],
    //                     color: "red",
    //                 });
    //                 testData.push({
    //                     label: xValue,
    //                     y: [oldValue, oldValue, newValue, newValue],
    //                     x: i,
    //                 });
    //                 // }
    //                 oldValue = newValue;
    //                 oldValueDate = data[i].x;
    //             }
    //         }
    //     }
    //
    //     if (isTest) {
    //         return newDataPoints;
    //     } else {
    //         setDataCalc(newDataPoints);
    //     }
    // }
    //
    // const applyStrategy = (strategyChart:any, data: any[], isCommission: boolean = false) => {
    //     const p=strategy.param.map((i:any)=>{return i.defaultValue});
    //     const params={param2:p[1], param3:p[2], param4:p[3]};
    //     switch (strategyChart.name) {
    //         case 'strategy1':
    //             return closeStrategy(data);
    //         case 'strategy2':
    //             return trendMagicStrategy(data, params);
    //         // case 'strategy3':
    //         //     return smaStrategy(data);
    //         default:
    //             return null;
    //     }
    // }
    //
    // const closeStrategy = (data: any, isCommission: boolean = false) => {
    //     let dataWithMarkerOrders = cloneDeep(data);
    //     let closePrice = null;
    //     let orders = [];
    //     let equity: any[] = [];
    //     let count = 0;
    //     let profit: number = 0;
    //     const valueOfCommission: number = isCommission ? 50 : 0;
    //     for (let i = 0; i < dataWithMarkerOrders.length; i++) {
    //         if (i === 0) {
    //             if (dataWithMarkerOrders[i].y[3] > dataWithMarkerOrders[i].y[0]) {
    //                 closePrice = dataWithMarkerOrders[i].y[3];
    //                 orders.push({time: dataWithMarkerOrders[i].label, direction: "Покупка", result: 0, profit: 0});
    //                 equity.push({label: dataWithMarkerOrders[i].label, x: dataWithMarkerOrders[i].x, y: 0});
    //                 count++;
    //                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬆"};
    //             }
    //             if (dataWithMarkerOrders[i].y[3] < dataWithMarkerOrders[i].y[0]) {
    //                 closePrice = dataWithMarkerOrders[i].y[3];
    //                 orders.push({time: dataWithMarkerOrders[i].label, direction: "Продажа", result: 0, profit: 0});
    //                 equity.push({label: dataWithMarkerOrders[i].label, x: dataWithMarkerOrders[i].x, y: 0});
    //                 count++;
    //                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬇"};
    //             }
    //         } else {
    //             if (dataWithMarkerOrders[i].y[3] > dataWithMarkerOrders[i].y[0] && dataWithMarkerOrders[i - 1].y[3] < dataWithMarkerOrders[i - 1].y[0]) {
    //                 let result = closePrice - dataWithMarkerOrders[i].y[3];
    //                 closePrice = dataWithMarkerOrders[i].y[3];
    //                 profit += result;
    //                 orders.push({
    //                     time: dataWithMarkerOrders[i].label,
    //                     direction: "Покупка",
    //                     result,
    //                     profit,
    //                 });
    //                 count++;
    //                 equity.push({label: dataWithMarkerOrders[i].label, x: dataWithMarkerOrders[i].x, y: profit});
    //                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬆"}
    //             }
    //             if (dataWithMarkerOrders[i].y[3] < dataWithMarkerOrders[i].y[0] && dataWithMarkerOrders[i - 1].y[3] > dataWithMarkerOrders[i - 1].y[0]) {
    //                 let result = dataWithMarkerOrders[i].y[3] - closePrice;
    //                 closePrice = dataWithMarkerOrders[i].y[3];
    //                 profit += result;
    //                 orders.push({
    //                     time: dataWithMarkerOrders[i].label,
    //                     direction: "Продажа",
    //                     result,
    //                     profit,
    //                 });
    //                 equity.push({label: dataWithMarkerOrders[i].label, x: dataWithMarkerOrders[i].x, y: profit});
    //                 count++;
    //                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬇"};
    //             }
    //         }
    //     }
    //
    //     return {orders, dataWithMarkerOrders, equity, indicator: undefined};
    // }
    //
    // const trendMagicStrategy = (data: any[], params:any, isCommission: boolean = false) => {
    //     let dataWithMarkerOrders = cloneDeep(data);
    //     const indicator = calcMagicTrend(data, params);
    //     let closePrice = null;
    //     let orders = [];
    //     let equity: any[] = [];
    //     let count = 0;
    //     let profit: number = 0;
    //     const valueOfCommission: number = isCommission ? 50 : 0;
    //     for (let i = 0; i < dataWithMarkerOrders.length; i++) {
    //         if (orders.length === 0) {
    //             if (dataWithMarkerOrders[i].y[3] > indicator[i].y) {
    //                 closePrice = dataWithMarkerOrders[i].y[3];
    //                 orders.push({time: dataWithMarkerOrders[i].label, direction: "Покупка", result: 0, profit: 0});
    //                 equity.push({label: dataWithMarkerOrders[i].label, x: dataWithMarkerOrders[i].x, y: 0});
    //                 count++;
    //                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬆"};
    //             }
    //             if (dataWithMarkerOrders[i].y[3] < indicator[i].y) {
    //                 closePrice = dataWithMarkerOrders[i].y[3];
    //                 orders.push({time: dataWithMarkerOrders[i].label, direction: "Продажа", result: 0, profit: 0});
    //                 equity.push({label: dataWithMarkerOrders[i].label, x: dataWithMarkerOrders[i].x, y: 0});
    //                 count++;
    //                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬇"};
    //             }
    //         } else {
    //             if (dataWithMarkerOrders[i].y[3] > indicator[i].y && dataWithMarkerOrders[i - 1].y[3] < indicator[i - 1].y) {
    //                 let result = closePrice - dataWithMarkerOrders[i].y[3];
    //                 closePrice = dataWithMarkerOrders[i].y[3];
    //                 profit += result;
    //                 orders.push({
    //                     time: dataWithMarkerOrders[i].label,
    //                     direction: "Покупка",
    //                     result,
    //                     profit,
    //                 });
    //                 count++;
    //                 equity.push({label: dataWithMarkerOrders[i].label, x: dataWithMarkerOrders[i].x, y: profit});
    //                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬆", profit};
    //             }
    //             if (dataWithMarkerOrders[i].y[3] < indicator[i].y && dataWithMarkerOrders[i - 1].y[3] > indicator[i - 1].y) {
    //                 let result = dataWithMarkerOrders[i].y[3] - closePrice;
    //                 closePrice = dataWithMarkerOrders[i].y[3];
    //                 profit += result;
    //                 orders.push({
    //                     time: dataWithMarkerOrders[i].label,
    //                     direction: "Продажа",
    //                     result,
    //                     profit,
    //                 });
    //                 count++;
    //                 equity.push({label: dataWithMarkerOrders[i].label, x: dataWithMarkerOrders[i].x, y: profit});
    //                 dataWithMarkerOrders[i] = {...dataWithMarkerOrders[i], indexLabel: "⬇", profit};
    //             }
    //         }
    //     }
    //
    //     return {orders, dataWithMarkerOrders, equity, indicator};
    // }
    //
    // const renderBackTestTab = useMemo(() => {
    //     return <BackTestTab nameStrategy={strategy} data={renko.data}/>;
    // }, [strategy, renko.data]);
    //
    // const renderTabs = (orders: any[]) => {
    //     return <Tabs
    //         defaultActiveKey="home"
    //         id="uncontrolled-tab-example"
    //         className="mt-3 me-3 ms-3"
    //     >
    //         <Tab eventKey="home" title="Общие">
    //             {renderCommonInfo(orders)}
    //         </Tab>
    //         <Tab eventKey="profile" title="Сделки">
    //             {renderOrderTable(orders)}
    //         </Tab>
    //         <Tab eventKey="contact" title="Тестер">
    //             {renderBackTestTab}
    //         </Tab>
    //     </Tabs>
    // }
    //
    // const renderOrderTable = (orders: any[]) => {
    //     return (
    //         <Row>
    //             <Col className="ms-4 mt-2" style={{height: 250, overflow: "auto"}}>
    //                 <Table  style={{color: "rgb(140, 144, 154)"}} className={"ms-3 w-75"} bordered variant="outline">
    //                     <thead>
    //                     <tr style={{position: "sticky", top: 0}}>
    //                         <th>№</th>
    //                         <th>Время</th>
    //                         <th>Направление</th>
    //                         <td>Результат сделки</td>
    //                         <td>Результат нарастающим итогом</td>
    //                     </tr>
    //                     </thead>
    //                     <tbody>
    //                     {orders.map((value, index) => {
    //                         return <tr style={{color: "white", backgroundColor: "#41464b6b"}} key={index}>
    //                             <td>{index + 1}</td>
    //                             <td>
    //                                 {value.time}
    //                             </td>
    //                             <td>
    //                                 {value.direction}
    //                             </td>
    //                             <td>
    //                                 {value.result}
    //                             </td>
    //                             <td>
    //                                 {value.profit}
    //                             </td>
    //                         </tr>
    //                     })}
    //                     </tbody>
    //                 </Table>
    //             </Col>
    //         </Row>
    //     );
    // }
    //
    // const renderCommonInfo = (orders: any[]) => {
    //     return <Row>
    //         <Col className="ms-4 mt-2 text-white">
    //             {`Наименование стратегии: ${strategy?.name}`}
    //             <br/>
    //             {`Всего сделок: ${orders.length}`}
    //             <br/>
    //             {`Профит за период: ${orders.slice(-1)[0]?.profit}`}
    //             <br/>
    //             {`Максимальная прибыль: ${max(orders.map((value) => {
    //                 return value['result']
    //             }))}`}
    //             <br/>
    //             {`Максимальный убыток: ${min(orders.map((value) => {
    //                 return value['result']
    //             }))}`}
    //             <br/>
    //             {`Профит фактор: ${profitFactor(orders)}`}
    //         </Col>
    //     </Row>
    // }
    //
    // const calcSMA = (period: number, data: any[]) => {
    //     let result: any[] = [];
    //     let sma = new SMA({period, values: []});
    //     data.forEach((item, index) => {
    //         let val = sma.nextValue(item.y[1])
    //         result.push({label: item.label, x: item.x, y: val ? val : null});
    //     });
    //
    //     return result;
    // }
    //
    // const createChartAndOrdersTable = (data: any[] = [], isTest: boolean = false) => {
    //     const dataPoints = filter(data.length > 0 ? data : dataCalc, (item: any) => moment(item.label).isSameOrAfter(startDate, undefined));
    //     const result: any = applyStrategy(strategy, dataPoints);
    //     if (!isTest) {
    //         const dataChart = [];
    //         dataChart.push({
    //             type: "candlestick",
    //             risingColor: "green",
    //             fallingColor: "red",
    //             dataPoints: result.dataWithMarkerOrders,
    //             indexLabelPlacement: "inside",
    //             indexLabelFontColor: "#ced4da",
    //             fillOpacity: .5,
    //         });
    //
    //         if (result.indicator) {
    //             dataChart.push({type: "line", dataPoints: result.indicator})
    //         }
    //
    //         let options = {
    //             title: {
    //                 text: ''
    //             },
    //             rangeChanged: function (e: any) {
    //                 console.log(e);
    //             },
    //             // backgroundColor:"rgb(30, 30, 47)",
    //             backgroundColor: "#212529",
    //             charts: [{
    //                 axisX: {
    //                     lineThickness: 1,
    //                     tickLength: 0,
    //                     labelFontColor: "white",
    //                     // interlacedColor: "#F8F1E4",
    //                     labelFormatter: function (e: any) {
    //                         return "";
    //                         // moment(e.label).format("YYYY-MMM-DD");
    //                     },
    //                 },
    //                 axisY: {
    //                     labelFontColor: "white"
    //                 },
    //                 data: dataChart,
    //             },
    //                 {
    //                     height: 150,
    //                     axisX:{
    //                         labelFormatter: function (e: any) {
    //                             return "";
    //                             // moment(e.label).format("YYYY-MMM-DD");
    //                         },
    //                     },
    //                     axisY: {
    //                         includeZero: false,
    //                     },
    //                     data: [{
    //                         type: "splineArea",
    //                         color: "#71AC66",
    //                         dataPoints: result.equity,
    //                     }]
    //                 }
    //             ],
    //             rangeSelector: {
    //                 enabled: false,
    //             },
    //             navigator: {
    //                 dynamicUpdate: true,
    //             }
    //         };
    //
    //         return renko.data?.length > 2 &&
    //             (<>
    //                 <Col className="mt-3 ms-3 me-3 col-10">
    //                     <CanvasJSStockChart options={options}/>
    //                 </Col>
    //                 {renderTabs(result.orders)}
    //             </>)
    //     } else {
    //         return getStrategyEstimate(result.orders);
    //     }
    // }
    //
    // return (<>
    //     <Row>
    //         <Col style={{display: "inline-flex"}} className="mt-3 ms-4">
    //             <Form.Select style={{width: "6%"}} className="me-3"
    //                          onChange={(value: any) => getDataForTicker(value.target.value)}
    //                          aria-label="Default select example">
    //                 <option value="FUTRTS032300">RIH3</option>
    //                 <option value="FUTSI0323000">SiH3</option>
    //             </Form.Select>
    //             <Form.Group className="me-3" controlId="dob">
    //                 <ButtonGroup>
    //                     <Button variant="outline-dark" onClick={() => {
    //                         setStartDate(moment(new Date()).subtract(5, "hours").format())
    //                     }}>
    //                         {'5H'}
    //                     </Button>
    //                     <Button variant="outline-dark" onClick={() => {
    //                         setStartDate(moment(new Date()).subtract(1, "day").format())
    //                     }}>
    //                         {'1D'}
    //                     </Button>
    //                     <Button active variant="outline-dark" onClick={() => {
    //                         setStartDate(moment(new Date()).subtract(3, "day").format())
    //                     }}>
    //                         {'3D'}
    //                     </Button>
    //                     <Button variant="outline-dark" onClick={() => {
    //                         setStartDate(moment(new Date()).subtract(2, "weeks").format())
    //                     }}>
    //                         {'2W'}
    //                     </Button>
    //                     <Button variant="outline-dark" onClick={() => {
    //                         setStartDate(moment(new Date()).subtract(1, "month").format())
    //                     }}>
    //                         {'1M'}
    //                     </Button>
    //                     <Button variant="outline-dark" onClick={() => {
    //                         setStartDate(moment(new Date()).subtract(3, "months").format())
    //                     }}>
    //                         {'3M'}
    //                     </Button>
    //                 </ButtonGroup>
    //             </Form.Group>
    //             <Form.Select style={{width: "6%"}} className="me-3" value={brickSize}
    //                          onChange={(value: any) => setBrickSize(toNumber(value.target.value))}
    //                          aria-label="Default select example">
    //                 <option value="10">10</option>
    //                 <option value="75">75</option>
    //                 <option value="100">100</option>
    //                 <option value="130">130</option>
    //                 <option value="150">150</option>
    //                 <option value="200">200</option>
    //                 <option value="250">250</option>
    //                 <option value="300">300</option>
    //                 <option value="350">350</option>
    //             </Form.Select>
    //             <Form.Select style={{width: "8%"}}
    //                          className="me-3"
    //                          onChange={(value: any) => setStrategy(value.target.value)}
    //                          aria-label="Default select example">
    //                 <option value="strategy1">strategy1</option>
    //                 <option value="strategy2">strategy2</option>
    //                 <option value="strategy3">strategy3</option>
    //             </Form.Select>
    //             <Button onClick={() => {
    //                 setShowSetStrategy(true)
    //             }} style={{
    //                 backgroundColor: "#212529",
    //                 paddingTop: 7,
    //             }}>
    //                 <Icon icon={'bi bi-gear'} size={15} title={''} text={''}/>
    //             </Button>
    //         </Col>
    //     </Row>
    //     <Row className="me-2 ms-2">
    //         <Col>
    //             <Form.Text>
    //                 {lastTimeUpdate && (`Последняя время обновления: ${lastTimeUpdate}. Последняя цена: ${lastPrice.data}.`)}
    //             </Form.Text>
    //         </Col>
    //     </Row>
    //     {strategy?.name&&createChartAndOrdersTable()}
    //     {strategy?.name&&renderSetStrategy}
    // </>);

    return null;
}

/**
 * Селектор редакс состояния.
 * @param state Состояние приложения
 */
function selectState({renko, lastPrice}: IAppState): ISelectedState {
    return {
        renko,
        lastTimeUpdate:renko.lastTimeUpdate,
        lastPrice,
    };
}

export default Renko;