import React, {CSSProperties} from 'react';
import Select from 'react-select';
import {useSelector} from "react-redux";
import {IAppState} from "../../index";

interface IProps {
    onChange?: (e: any) => void;
    value?: any;
    errorMsg?: string;
    style?: CSSProperties;
    options?: any;
    placeholder?: string;
    producer?:string;
}

const SelectFilter = (props: IProps) => {
    const options = useSelector((state: IAppState) =>
        state.ticker.data);

    const defaultProps: CSSProperties = {minWidth: 300, maxHeight: 38, marginBottom: 40, marginTop: 15}

    return (
        <div style={props.style || defaultProps} className="me-3">
            <Select
                name="ticker"
                value={props.value}
                isClearable
                isSearchable
                options={props.options || (props.producer==='BYBIT'?options?.BYBIT:options?.TKS)}
                placeholder={props.placeholder || 'Выберите тикер'}
                onChange={(e: any) => {
                    props.onChange && props.onChange({target: {value: e?.value, name: 'ticker'}})
                }}
            />
            {props.errorMsg && (
                <span style={{fontSize: ".875em"}} className="text-danger">
          {props.errorMsg && props.errorMsg}
        </span>
            )}
        </div>
    );
}

export default SelectFilter;