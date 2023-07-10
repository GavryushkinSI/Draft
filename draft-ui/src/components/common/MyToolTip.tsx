import {Button, OverlayTrigger, Tooltip} from "react-bootstrap";
import Icon from "./Icon";
import {ButtonLink} from "@paljs/ui";
import React from "react";

interface IProps {
    text: string|any;
    textInner?: string|any;
    style?: any;
    placement?:string;
}

const MyToolTip = (props: IProps) => {
    const renderTooltip = (pr: any) => (
        <Tooltip id="button-tooltip" {...pr}>
            {props.text}
        </Tooltip>
    );

    return (
        <OverlayTrigger
            // @ts-ignore
            placement={props.placement?props.placement:"right"}
            delay={{show: 250, hide: 400}}
            overlay={props.text ? renderTooltip : <></>}
        >
            <ButtonLink style={props.style}>
                {!props.textInner ? <Icon icon={'bi bi-question-circle'} size={15} title={''}/> : props.textInner}
            </ButtonLink>
        </OverlayTrigger>
    );
}

export default MyToolTip;