import {Col, ListGroup, Row, Table} from "react-bootstrap";
import React from "react";

interface IProps {
    logs: any[];
}

const OrderLog = (props: IProps) => {
    return <Table style={{color: "rgb(140, 144, 154)"}} className={"ms-3 w-75"} bordered
                  variant="outline">
        <thead>
        <tr key={"header"}/>
        </thead>
        <tbody>
        {props.logs?.reverse()?.map((i: any, index: number) => {
            return <tr><td>
                <td>

                </td>
            </td></tr>
        })}
        </tbody>
    </Table>
}

export default OrderLog;