import React from "react";
import {Table} from "react-bootstrap";
import {IStrategy} from "../models/models";
import {useSelector} from "react-redux";
import {IAppState} from "../index";

interface IPortfolio {
    ticker?: string;
    lots?: number;
}

const Portfolio = () => {
    const portfolio: IPortfolio[] = useSelector((state: IAppState) =>
        state.portfolio.data);
    return <div style={{marginBottom:-15}} className="ps-3 pt-1">
        <h6>
            Портфолио:
        </h6>
        <>
            <Table className={"portfolio-table"} bordered variant="outline">
                <thead>
                <tr key={"header"}>
                    <th key={'№'}>№</th>
                    <th key={'ticker'}>{'Тикер'}</th>
                    <th key={'lots'}>{'Значение'}</th>
                </tr>
                </thead>
                <tbody>
                {portfolio&&portfolio.map((row, index) => {
                    return <tr key={row.ticker}>
                        <td>{index + 1}</td>
                        <td>
                            {row.ticker}
                        </td>
                        <td>
                            {row.lots}
                        </td>
                    </tr>
                })}
                </tbody>
            </Table>
        </>
    </div>
}

export default Portfolio;