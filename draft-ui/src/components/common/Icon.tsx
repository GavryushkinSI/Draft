import * as React from "react";
import {useState} from "react";

interface IProps {
    icon: string;
    size: number;
    title:string;
    text?:string;
    color?:any;
}

const Icon = (props: IProps) => {
    const [isHover, setIsHover] = useState(false);
    return <i title={props.title} style={{fontSize: props.size, fontStyle:"normal", color:props.color, borderRadius:"unset"}} className={props.icon}
              onMouseEnter={() => {
                  setIsHover(true)
              }}
              onMouseLeave={() => {
                  setIsHover(false)
              }}
    >{props.text}</i>
}

export default Icon;