import * as React from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css"
import {
    Button,
    ButtonGroup,
    Col,
    Form,
    Row,
    Table
} from "react-bootstrap";
import {useActions, useMountEffect, useShallowEqualSelector} from "../hooks/hooks";
import {Service} from "../services/Service";
import {IAppState} from "../index";
import {sum, toNumber, takeRight, filter} from "lodash"
import {useEffect, useRef, useState} from "react";
import moment from 'moment';
import SockJS from "sockjs-client";
import {over} from 'stompjs';
import {Message} from "../models/models";
import CanvasJSReact from '../libraries/canvasjs.stock.react';
import '../styles/common.css';

const SMA = require('technicalindicators').SMA;
const ATR = require('technicalindicators').ATR;
const CCI = require('technicalindicators').CCI;
const CanvasJSStockChart = CanvasJSReact.CanvasJSStockChart;

interface ISelectedState {
    renko: any
}

let stompClient: any = null;
const Renko = () => {
    const actions: Service = useActions(Service);

    const {renko} = useShallowEqualSelector<ISelectedState>(selectState);
    const [viewTicker, setViewTicker] = useState('FUTRTS032300');
    const [startDate, setStartDate] = useState(moment(new Date()).subtract(3, "day").format());
    const [brickSize, setBrickSize] = useState(150);
    const [dataCalc, setDataCalc] = useState([]);
    let chartRef: any = useRef();

    const connect = async () => {
        let Sock = new SockJS('http://localhost:8080/ws');
        stompClient = over(Sock);
        stompClient.debug = null;
        await stompClient.connect({}, onConnected, (error: any) => {
            console.log(error)
        });
    }

    useMountEffect(() => {
        if (stompClient === null) {
            connect().then(() => {
                setTimeout(() => {
                    getDataForTicker(viewTicker)
                }, 500)
            });
        }

        return () => {
            stompClient.unsubscribe('/user/' + 'test' + '/private');
        }
    })

    useEffect(() => {
        if (renko.data.length > 2) {
            renderRenko(brickSize, renko.data);
        }
    }, [renko.data, brickSize])

    const getDataForTicker = (ticker: string) => {
        const message = {
            senderName: 'test',
            status: 'JOIN',
            message: ticker,
            command: 'SUBSCRIPTION_ON_TICKER',
        };
        stompClient.send("/app/message", {}, JSON.stringify(message));
        setViewTicker(ticker);
    }

    const onConnected = () => {
        stompClient.subscribe('/user/' + 'test' + '/private', onMessageReceived);
        userJoin();
    }

    const onMessageReceived = (payload: any) => {
        const payloadData: Message = JSON.parse(payload.body);
        switch (payloadData.status) {
            case "JOIN":
                actions.setData(payloadData.message);
                break;
            case "MESSAGE":
                break;
        }
    }

    const userJoin = () => {
        const message = {
            senderName: 'test',
            status: 'JOIN',
            command: 'ADD_USER',
        };
        stompClient.send("/app/message", {}, JSON.stringify(message));
    }

    const renderRenko = (brickSize: number, data: any) => {
        let count = -1;
        let newDataPoints = [];
        let testData = [];
        let oldValue = data[0].y[3];
        let oldValueDate;

        try {
            oldValueDate = moment(data[0].x).toLocaleString();
        } catch (errors) {
            console.log(data[0].x)
        }
        let difference = 0;
        let newValue, dataPoint, xValue, prevData;
        for (let i = 1; i < data.length; i++) {
            dataPoint = data[i].y[3];
            prevData = newDataPoints.slice(-1)[0];
            try {
                xValue = moment(data[i].x).toLocaleString();
            } catch (errors) {
                //console.log("error",CanvasJS.formatDate("2022-12-13 09:00:00", "YYYY-MMM-DD hh:mm:ss", undefined));
            }
            difference = dataPoint - oldValue;
            if (difference > 0 && difference > brickSize) {
                for (let j = 0; j < Math.floor(difference / brickSize); j++) {
                    newValue = oldValue + brickSize;
                    //console.log(CanvasJS.formatDate(oldValueDate, "YYYY-MM-DD hh:mm:ss", undefined),CanvasJS.formatDate(data[i].x, "YYYY-MM-DD hh:mm:ss", undefined));
                    // if (CanvasJS.formatDate(oldValueDate, "YYYY-MM-DD hh:mm:ss", undefined) === CanvasJS.formatDate(data[i].x, "YYYY-MM-DD hh:mm:ss", undefined)) {
                    //     newDataPoints.push({label: xValue, y: [oldValue, newValue], color: "#0700e4"});
                    //     testData.push({label: xValue, y: [oldValue, newValue, oldValue, newValue], color: "#0700e4"});
                    // } else {
                    newDataPoints.push({
                        label: xValue,
                        x: count++,
                        y: [oldValue, newValue, oldValue, newValue],
                        color: "green",
                        // indexLabel:"Buy"
                    });
                    testData.push({
                        label: xValue,
                        y: [oldValue, newValue, oldValue, newValue],
                        x: i,
                    });
                    // }
                    oldValue = newValue;
                    oldValueDate = data[i].x;
                }
            } else if (difference < 0 && Math.abs(difference) > brickSize) {
                for (let j = 0; j < Math.floor(Math.abs(difference) / brickSize); j++) {
                    newValue = oldValue - brickSize;
                    // if (CanvasJS.formatDate(oldValueDate, "YYYY-MM-DD hh:mm:ss", undefined) === CanvasJS.formatDate(data[i].x, "YYYY-MM-DD hh:mm:ss", undefined)) {
                    //     newDataPoints.push({label: xValue, y: [oldValue, newValue], color: "#0700e4"});
                    //     testData.push({label: xValue, y: [oldValue, oldValue, newValue, newValue], color: "#0700e4"});
                    // } else {
                    newDataPoints.push({
                        label: xValue,
                        x: count++,
                        y: [oldValue, oldValue, newValue, newValue],
                        color: "red",
                    });
                    testData.push({
                        label: xValue,
                        y: [oldValue, oldValue, newValue, newValue],
                        x: i,
                    });
                    // }
                    oldValue = newValue;
                    oldValueDate = data[i].x;
                }
            }
        }

        //console.log("Last time:", newDataPoints.slice(-1)[0]?.label);
        // @ts-ignore
        setDataCalc(newDataPoints);
    }

    const calcProfit = (data: any, isCommission: boolean = false) => {
        let closePrice = null;
        let result = [];
        let orders = [];
        let count = 0;
        const valueOfCommission: number = isCommission ? 50 : 0;
        for (let i = 0; i < data.length; i++) {
            if (i === 0) {
                if (data[i].y[1] > data[i].y[0]) {
                    closePrice = data[i].y[1];
                    orders.push({time: data[i].label, direction: "Buy"});
                    count++;
                }
                if (data[i].y[1] < data[i].y[0]) {
                    closePrice = data[i].y[1];
                    orders.push({time: data[i].label, direction: "Sell"});
                    count++;
                }
            } else {
                if (data[i].y[1] > data[i].y[0] && data[i - 1].y[1] < data[i - 1].y[0]) {
                    result.push(closePrice - data[i].y[1]);
                    closePrice = data[i].y[1];
                    orders.push({time: data[i].label, direction: "Buy"});
                    count++;
                }
                if (data[i].y[1] < data[i].y[0] && data[i - 1].y[1] > data[i - 1].y[0]) {
                    result.push(data[i].y[1] - closePrice);
                    closePrice = data[i].y[1];
                    orders.push({time: data[i].label, direction: "Sell"});
                    count++;
                }
            }
        }

        const chooseData = takeRight(orders, 10);
        let m = takeRight(result, 10);
        m[0] = 0;

        return (
            <Row>
                <Col className="mt-3 ms-3 me-3" style={{height: 250, overflow: "auto"}}>
                    <Table className="w-50" striped bordered hover variant="dark">
                        <thead>
                        <tr>
                            <th>№</th>
                            <th>Время</th>
                            <th>Направление</th>
                            <td>Результат сделки</td>
                            <td>Общий результат</td>
                        </tr>
                        </thead>
                        <tbody>
                        {chooseData.map((value, index) => {
                            return <tr key={index}>
                                <td>{index + 1}</td>
                                <td>
                                    {value.time}
                                </td>
                                <td>
                                    {value.direction}
                                </td>
                                <td>
                                    {m[index]}
                                </td>
                                <td>
                                    {sum(m)}
                                </td>
                            </tr>
                        })}
                        </tbody>
                    </Table>
                </Col>
            </Row>
        );
    }

    const calcMagicTrend = (data: any[]) => {
        const nz = (array: any[]) => {
            const value = array.slice(-1)[0];
            return !!value ? value.y : 0;
        }
        const cciPeriod = 20;
        const coeff = 1.5;
        const atrPeriod = 5;
        let atr = new ATR({high: [], low: [], close: [], period: atrPeriod});
        let cci = new CCI({high: [], low: [], close: [], period: cciPeriod});
        let magicTrend: any[] = [];
        data.forEach((item, index) => {
                let candle = item.y[1] >= item.y[0] ? {
                    high: item.y[1],
                    low: item.y[0],
                    close: item.y[1]
                } : {high: item.y[0], low: item.y[1], close: item.y[1]};
                let currentAtr = atr.nextValue(candle);
                if (currentAtr) {
                    let upT = candle.low - currentAtr * coeff
                    let downT = candle.high + currentAtr * coeff;
                    let currentCCI = cci.nextValue(candle);
                    magicTrend.push({
                        label: item.label, x: item.x, y: currentCCI >= 0 ?
                            (upT < magicTrend.slice(-1)[0] ? nz(magicTrend) : upT)
                            : (downT > nz(magicTrend) ? nz(magicTrend) : downT)
                    });
                } else {
                    magicTrend.push({label: item.label, x: item.x, y: null});
                }
            }
        );
        return magicTrend;
    }

    const calcSMA = (period: number, data: any[]) => {
        let result: any[] = [];
        let sma = new SMA({period, values: []});
        data.forEach((item, index) => {
            let val = sma.nextValue(item.y[1])
            result.push({label: item.label, x: item.x, y: val ? val : null});
        });

        return result;
    }

    const createChart = () => {
        const dataPoints = filter(dataCalc, (item: any) => moment(item.label).isSameOrAfter(startDate, undefined));
        //const test = filter(calcMagicTrend(dataCalc), (item: any) => moment(item.label).isSameOrAfter(startDate, undefined));

        let options = {
            title: {
                text: ''
            },
            backgroundColor: "#212529",
            charts: [{
                axisX: {
                    lineThickness: 3,
                    tickLength: 0,
                    labelFontColor: "white",
                    labelFormatter: function (e: any) {
                        return "";
                        // moment(e.label).format("YYYY-MMM-DD");
                    },
                },
                axisY: {
                    labelFontColor: "white"
                },
                data: [
                    {
                        type: "candlestick",
                        risingColor: "green",
                        fallingColor: "red",
                        dataPoints: dataPoints,
                    },
                    // {
                    //     type: "line",
                    //     lineColor: "white",
                    //     dataPoints: test,
                    // },
                ]
            }],
            rangeSelector: {
                enabled: false,
            },
            navigator: {}
        };

        return options;
    }

    return (<>
        <Row>
            <Col className="mt-3 me-3 ms-3 col-md-1">
                <Form.Select onChange={(value: any) => getDataForTicker(value.target.value)}
                             aria-label="Default select example">
                    <option value="FUTRTS032300">RIH3</option>
                    <option value="FUTSI0323000">SiH3</option>
                </Form.Select>
            </Col>
            <Col style={{width:'36%'}} className="col-md-5">
                <Form.Group className="mt-3 ms-3 me-3" controlId="dob">
                    <ButtonGroup>
                        <Button variant="outline-dark" onClick={() => {
                            setStartDate(moment(new Date()).subtract(5, "hours").format())
                        }}>
                            {'5 Часов'}
                        </Button>
                        <Button variant="outline-dark" onClick={() => {
                            setStartDate(moment(new Date()).subtract(1, "day").format())
                        }}>
                            {'1 День'}
                        </Button>
                        <Button variant="outline-dark" onClick={() => {
                            setStartDate(moment(new Date()).subtract(3, "day").format())
                        }}>
                            {'3 Дня'}
                        </Button>
                        <Button variant="outline-dark" onClick={() => {
                            setStartDate(moment(new Date()).subtract(2, "weeks").format())
                        }}>
                            {'2 Недели'}
                        </Button>
                        <Button variant="outline-dark" onClick={() => {
                            setStartDate(moment(new Date()).subtract(1, "month").format())
                        }}>
                            {'1 Месяц'}
                        </Button>
                        <Button variant="outline-dark" onClick={() => {
                            setStartDate(moment(new Date()).subtract(3, "months").format())
                        }}>
                            {'3 Месяца'}
                        </Button>
                    </ButtonGroup>
                </Form.Group>
            </Col>
            <Col className="mt-3 me-3 col-md-1">
                <Form.Select value={brickSize} onChange={(value: any) => setBrickSize(toNumber(value.target.value))}
                             aria-label="Default select example">
                    <option value="75">75</option>
                    <option value="100">100</option>
                    <option value="150">150</option>
                    <option value="200">200</option>
                    <option value="250">250</option>
                    <option value="300">300</option>
                    <option value="350">350</option>
                </Form.Select>
            </Col>
        </Row>
        {renko.data?.length > 2 &&
            (<div className="mt-3 ms-3 me-3">
                <CanvasJSStockChart ref={chartRef} options={createChart()}/>
            </div>)
        }
        {dataCalc.length > 0 && calcProfit(dataCalc)}
    </>);
}

/**
 * Селектор редакс состояния.
 * @param state Состояние приложения
 */
function selectState({renko}: IAppState): ISelectedState {
    return {
        renko: renko
    };
}

export default Renko;