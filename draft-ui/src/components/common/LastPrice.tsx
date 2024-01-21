import MyToolTip from "./MyToolTip";
import React, {useMemo} from "react";
import {useSelector} from "react-redux";
import {IAppState} from "../../index";
import {IStrategy} from "../../models/models";

interface IProps{
    row:IStrategy;
}

const LastPrice: React.FC<IProps>=({row}:IProps)=>{
    const lastPrice: Map<string, any> = useSelector((state: IAppState) => state.lastPrice.data);

   return lastPrice.get(row.figi!) ?
            (<MyToolTip style={{
                marginTop: "-4px",
                borderBottom: "1px dashed black",
                color: "black"
            }} placement={"top"} text={`updateTime: ${lastPrice.get(row.figi!)?.time}`}
                        textInner={lastPrice.get(row.figi!)?.price}/>) : <>{'wait...'}</>
}

export default LastPrice;