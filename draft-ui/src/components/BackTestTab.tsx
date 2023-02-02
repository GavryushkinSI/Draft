import * as React from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css"
import {IBackTestResultStartegy} from "../models/models";
import {Button, Col, Form, InputGroup, ProgressBar, Row, Table} from "react-bootstrap";
import {useEffect, useMemo, useState} from "react";
import {useMountEffect} from "../hooks/hooks";
import {STRATEGY_MAP} from "../backtest/params";
import Icon from "./common/Icon";
import '../styles/common.css';
import ModalView from "./common/ModalView";
import {cloneDeep, toNumber} from "lodash"
import {createChartAndOrdersTable, getAllCombinations, getDataForRenkoChart, saveRow} from "../utils/utils";

interface IProps {
    nameStrategy: string;
    data?: any[];
}

const BackTestTab = (props: IProps) => {
    const [testStrategy, setTestStrategy] = useState<any>();
    const [startTest, setStartTest] = useState(false);
    const [percentage, setPercentage] = useState(0);
    const [index, setIndex] = useState(0);
    const [showSetStrategy, setShowSetStrategy] = useState(false);
    const [estimateStrategy, setEstimateStrategy] = useState<any>([])

    useEffect(()=>{
        if (props.nameStrategy) {
            const paramTestStrategy = STRATEGY_MAP[props.nameStrategy];
            setTestStrategy(paramTestStrategy);
        }
    },[props.nameStrategy])

    useEffect(() => {
        if (startTest) {
            backTestExecute();
        } else {
            setStartTest(false);
        }
    }, [startTest, index])

    const renderProgressBar = () => {
        return (
            <Row className="ms-3 me-2 mt-3">
                <Col>
                    <ProgressBar now={percentage} variant="success" label={`${percentage}%`} striped/>
                </Col></Row>);
    }

    const backTestExecute = () => {
        setTimeout(() => {
            const combinations = getAllCombinations(testStrategy?.param?.filter((i: any) => i.type === 'step'));
            if (index === combinations.length) {
                setStartTest(false);
                setPercentage(100);
            } else {
                const params = combinations[index];
                const data = getDataForRenkoChart(params.param1, props.data);
                const newIndex = index + 1;
                const newPercentage = Math.round((index / combinations.length) * 100);
                const x = createChartAndOrdersTable(data, testStrategy?.name, params);
                const newEstimate = [...estimateStrategy, x];
                setEstimateStrategy(newEstimate);
                setPercentage(newPercentage);
                setIndex(newIndex);
            }
        }, 5);
    }

    const renderEstimateStrategyTable = (): false | JSX.Element => {
        return percentage === 100 && (<Row className="ms-3 mt-2" style={{height: 250, overflow: "auto"}}>
            <Col>
                <Table striped hover variant="dark">
                    <thead>
                    <tr style={{position: "sticky", top: 0}}>
                        <th>№</th>
                        <th>Количество сделок</th>
                        <th>Суммарный доход</th>
                        <td>Максимальная прибыль от сделки</td>
                        <td>Максимальный убыток от сделки</td>
                        <td>Профит фактор</td>
                    </tr>
                    </thead>
                    <tbody>
                    {estimateStrategy.map((value: IBackTestResultStartegy, index: number) => {
                        return <tr key={index}>
                            <td>{index + 1}</td>
                            <td>
                                {value.count}
                            </td>
                            <td>
                                {value.sumProfit}
                            </td>
                            <td>
                                {value.maxProfitOnOrder}
                            </td>
                            <td>
                                {value.maxLossOnOrder}
                            </td>
                            <td>
                                {value.profitFactor}
                            </td>
                        </tr>
                    })}
                    </tbody>
                </Table>
            </Col>
        </Row>)
    }

    const renderSetStrategy = useMemo(() => {
        let content: JSX.Element[] = [];
        let key: number = 0;
        testStrategy?.param.forEach((item: any, index: number) => {
            if (item.type === 'step') {
                content.push(
                    <InputGroup key={key++} className="mb-3">
                        <InputGroup.Text>{item.name}</InputGroup.Text>
                        <InputGroup.Text>{'Мин.'}</InputGroup.Text>
                        <Form.Control onChange={(e: any) => {
                            const min = toNumber(e.target.value);
                            const clone = cloneDeep(testStrategy);
                            const paramChanging = clone.param.find((i: any) => i.name === item.name);
                            setTestStrategy({...clone, param: saveRow({...paramChanging, min}, clone.param)});
                        }} value={item.min}/>
                        <InputGroup.Text>{'Макс.'}</InputGroup.Text>
                        <Form.Control value={item.max}
                                      onChange={(e: any) => {
                                          const max = toNumber(e.target.value);
                                          const clone = cloneDeep(testStrategy);
                                          const paramChanging = clone.param.find((i: any) => i.name === item.name);
                                          setTestStrategy({
                                              ...clone,
                                              param: saveRow({...paramChanging, max}, clone.param)
                                          });
                                      }}
                        />
                        <InputGroup.Text>{'Шаг'}</InputGroup.Text>
                        <Form.Control value={item.step} onChange={(e: any) => {
                            const step = toNumber(e.target.value);
                            const clone = cloneDeep(testStrategy);
                            const paramChanging = clone.param.find((i: any) => i.name === item.name);
                            setTestStrategy({...clone, param: saveRow({...paramChanging, step}, clone.param)});
                        }}/>
                    </InputGroup>)
                content.push(<br key={key++}/>);
            }
            if (item.type === 'one') {
                content.push(<InputGroup className="mb-3" key={key++}>
                    <InputGroup.Text>{item.name}</InputGroup.Text>
                    <Form.Control value={item.value}
                                  onChange={(e: any) => {
                                      const value = toNumber(e.target.value);
                                      const clone = cloneDeep(testStrategy);
                                      const paramChanging = clone.param.find((i: any) => i.name === item.name);
                                      setTestStrategy({
                                          ...clone,
                                          param: saveRow({...paramChanging, value}, clone.param)
                                      });
                                  }}
                    />
                </InputGroup>);
                content.push(<br key={key++}/>);
            }
            if (item.type === 'checkBox') {
                content.push(<Form.Check
                    type={'checkbox'}
                    label={`${item.name}`}
                    key={key++}
                    onChange={(e: any) => {
                        const value = e.target.value;
                        const clone = cloneDeep(testStrategy);
                        const paramChanging = clone.param.find((i: any) => i.name === item.name);
                        setTestStrategy({...clone, param: saveRow({...paramChanging, value}, clone.param)});
                    }}
                />);
                content.push(<br key={key++}/>);
            }
        });
        return <ModalView
            show={showSetStrategy}
            header={`Параметры: ${testStrategy?.name}`}
            accept={() => {
                setShowSetStrategy(false)
            }}
            cancel={() => {
                setShowSetStrategy(false)
            }}
            text={(<>{content}</>)}/>
    }, [testStrategy, showSetStrategy]);

    const renderStartBtn = () => {
        return <Row style={{display: " inline-flex"}} className="ms-3 mt-2">
            <Col>
                <Button onClick={() => {
                    const newValue: boolean = !startTest;
                    setStartTest(newValue);
                    setPercentage(0);
                    setIndex(0);
                    setEstimateStrategy([]);
                }} variant="outline-light">
                    <Icon color={startTest ? 'red' : '#71AC66'}
                          icon={startTest ? 'bi bi-stop-fill' : 'bi bi-caret-right-fill'} size={15}
                          title={''} text={''}/>
                </Button>
            </Col>
            <Col style={{whiteSpace: "nowrap", paddingTop: 7, paddingRight: 0, paddingLeft: 0}}
                 className=" text-white-50">
                {`Стратегия: ${testStrategy?.name}`}
            </Col>
            <Col>
                <Button onClick={() => {
                    setShowSetStrategy(true)
                }} style={{
                    backgroundColor: "#212529",
                    paddingTop: 7,
                }}>
                    <Icon icon={'bi bi-gear'} size={15} title={''} text={''}/>
                </Button>
            </Col>
        </Row>
    }

    return <>
        {renderSetStrategy}
        {renderStartBtn()}
        {startTest && renderProgressBar()}
        {renderEstimateStrategyTable()}

    </>
}

export default BackTestTab;

