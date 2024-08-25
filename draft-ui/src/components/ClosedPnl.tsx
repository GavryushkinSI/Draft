import React, {useEffect, useMemo, useState} from "react";
import {Service} from "../services/Service";
import {useActions} from "../hooks/hooks";
import {useSelector} from "react-redux";
import {IAppState} from "../index";
import {Chart} from "./Chart";
import {useNavigate} from "react-router-dom";
import Icon from "./common/Icon";
import {ROOT} from "../Route/consts";
import {
    Button,
    Col, Container,
    Form,
    Row
} from "react-bootstrap";
import AppError from "./AppError";
import {IStrategy} from "../models/models";

const convertData = (data: any[]): Map<string, any[]> => {
    const map = new Map<string, any[]>();
    let sum = 0;
    let sumFee = 0;
    const dataSum: any[] = [];
    const dataPnlInOrder: any[] = [];
    const dataFee: any[] = [];

    data.forEach((i: any, index): void => {
        sum = sum + i.closedPnl;
        sumFee = sumFee + i.fee;
        const label: string = i.symbol.concat(" : ").concat(i.time!=null?new Date(i.time).toLocaleDateString():"");
        dataSum.push({x: index, y: sum, label});
        dataPnlInOrder.push({x: index, y: i.closedPnl, label, color:i.closedPnl>=0?"green":"red"});
        dataFee.push({x: index, y: sumFee, label});
    });
    map.set("dataSum", dataSum);
    map.set("dataPnlInOrder", dataPnlInOrder);
    map.set("dataFee", dataFee);

    return map;
};

const ClosedPnl: React.FC = () => {
    const actions: Service = useActions(Service);
    const navigate = useNavigate();
    const data: Map<string, any[]> = useSelector((state: IAppState) => state.closedPnl.data);
    const strategy: IStrategy[] = useSelector((state: IAppState) => state.strategy.data);
    const isLoading = useSelector((state: IAppState) => state.closedPnl.isLoading);
    const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
    const commonData: any[] = useMemo(() => {
        return isLoading === true ? data['COMMON'] : [];
    }, [isLoading, data]);

    useEffect(() => {
        actions.getClosedPnl(new Date(date)?.getTime())
    }, []);

    const memoizedData: Map<string, any[]> = useMemo(() => {
        if (isLoading === true) {
            return convertData(commonData || [])
        }
        return null;
    }, [commonData, isLoading]);


    const memoizedDataForSymbol: Map<string, Map<string, any[]>> = useMemo(() => {
        const map = new Map<string, any[]>();
        if (isLoading === true) {
            for (const key in data) {
                if (strategy?.length>0&&strategy.find(i=>i.figi===key)) {
                    const res: Map<string, any[]> = convertData(data[key]);
                    map.set(key, res);
                }
            }
        }
        return map;
    }, [data, isLoading]);


    return <Container fluid>
        <AppError/>
        <Row style={{height: "100vh", position: "relative", backgroundColor: "hwb(0deg 0% 100% / 8%)"}} className="ms-2"><Col>
            <Button style={{marginLeft: -10}}>
                <Icon onClick={async () => navigate(ROOT().DRAFT.MAIN_PAGE.PATH)} text={' MAIN_PAGE'}
                      hoverColor={"lightgreen"}
                      color={"black"}
                      icon={"bi bi-backspace"} size={15} title={''}/>
            </Button>
            <Row className="mb-2">
                <Col>
                    <h6 style={{textAlign: "center", backgroundColor: "black", color: "white"}}>Common_Equity</h6>
                    <div style={{display: "inline-flex"}}>
                        <Form.Control type="date" value={date} onChange={(e) => {
                            setDate(e.target.value);
                        }}/>
                        <Button onClick={() => {
                            actions.getClosedPnl(new Date(date)?.getTime())
                        }} className="ms-2" variant={"dark"}>{"GET"}</Button>
                    </div>
                </Col>
            </Row>
            {isLoading === true ?
                <>
                    <Chart
                        data={memoizedData?.get("dataSum") || []}
                        data2={memoizedData?.get("dataPnlInOrder")}
                        data3={memoizedData?.get("dataFee")}
                    />
                    {Array.from(memoizedDataForSymbol.entries()).map(([outerKey, innerMap]) => (
                        <Row>
                            <Col>
                                <h6 style={{
                                    textAlign: "center",
                                    backgroundColor: "black",
                                    color: "white"
                                }}>{outerKey}</h6>
                                <Chart
                                    data={innerMap?.get("dataSum") || []}
                                    data2={innerMap?.get("dataPnlInOrder")}
                                    data3={innerMap?.get("dataFee")}
                                />
                            </Col>
                        </Row>
                    ))}
                </>
                : <div style={{textAlign: "center"}}>{"Загрузка..."}</div>}
        </Col>
        </Row>
    </Container>
}

export default ClosedPnl;