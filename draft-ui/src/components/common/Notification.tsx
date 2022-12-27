import {Alert, Button} from "react-bootstrap";
import * as React from "react";
import {useEffect, useState} from "react";
import Icon from "./Icon";
import {useMountEffect} from "../../hooks/hooks";

interface IProps {
    type: string;
    text?:string;
}

const Notification = (props: IProps) => {
    const [isShow, setShow] = useState(true);

    setTimeout(() => {
        setShow(false)
    }, 5000);

    return <Alert style={{width:350, float:"right"}} show={isShow} variant={props.type}>
        <p>
            <Icon icon={"bi bi-info-square"} size={20} title={''}/>
            {props.text}
        </p>
        <hr/>
        <div className="d-flex justify-content-end">
            <Button onClick={() => {setShow(false)}} variant="outline-success">
                Закрыть
            </Button>
        </div>
    </Alert>
}

export default Notification;
