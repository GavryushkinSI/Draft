import * as React from "react";
import {CSSProperties, useState} from "react";

interface IProps {
    icon: string;
    size: number;
    title:string;
    text?:string;
    color?:any;
    onClick?:any;
    className?:any;
    hoverColor?:any;
}

const Icon = (props: IProps) => {
    const [isHover, setIsHover] = useState(false);
    return <i onClick={props.onClick} title={props.title} style={{fontSize: props.size, fontStyle:"normal", color:props.hoverColor&&isHover?props.hoverColor:props.color, borderRadius:"unset"}} className={props.icon}
              onMouseEnter={() => {
                  setIsHover(true)
              }}
              onMouseLeave={() => {
                  setIsHover(false)
              }}
    >{props.text}</i>
}

export default Icon;