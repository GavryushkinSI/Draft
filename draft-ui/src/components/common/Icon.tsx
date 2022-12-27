import * as React from "react";
import {useState} from "react";

interface IProps {
    icon: string;
    size: number;
    title:string;
    text?:string;
}

const Icon = (props: IProps) => {
    const [isHover, setIsHover] = useState(false);
    return <i title={props.title} style={{fontSize: props.size, fontStyle:"normal", color: isHover ? "white" : "#8c909a", cursor: isHover?"pointer":"default"}} className={props.icon}
              onMouseEnter={() => {
                  setIsHover(true)
              }}
              onMouseLeave={() => {
                  setIsHover(false)
              }}
    >{props.text}</i>
}

export default Icon;