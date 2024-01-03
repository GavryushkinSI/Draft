import React, {Dispatch} from "react";
import {IStrategy} from "../../models/models";
import MyToolTip from "./MyToolTip";
import Icon from "./Icon";
import {useActions} from "../../hooks/hooks";
import {Service} from "../../services/Service";
import {useDispatch} from "react-redux";
import {addNotification} from "../../actions/notificationActions";

interface IProps {
    row: IStrategy;
}

const NameComponent: React.FC<IProps> = ({row}: IProps) => {

    const actions = useActions(Service);
    const dispatch: Dispatch<any> = useDispatch();

    return <span>{row?.description ?
        (<MyToolTip style={{
            marginTop: "-4px",
            borderBottom: "1px dashed black",
            color: "black"
        }} text={row?.description} textInner={row?.name + " "}/>) : row?.name! + " "}
        {row.errorData?.message ?
            (<MyToolTip
                text={row.errorData.message}
                textInner={<Icon
                    onClick={() => actions.clearError(
                        row,
                        "Admin")
                    }
                    icon={'bi bi-exclamation-square'}
                    size={18}
                    title={''}
                    text={''}
                    color={'green'}
                />}/>)
            : null}
    </span>
}

export default NameComponent;