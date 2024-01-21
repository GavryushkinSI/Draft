import React from "react";
import {Toast} from "react-bootstrap";
import {useSelector} from "react-redux";
import {IAppState} from "../index";
import {Service} from "../services/Service";
import {useActions} from "../hooks/hooks";

const AppError: React.FC = () => {
    const actions: Service = useActions(Service);
    const appError: string = useSelector((state: IAppState) => state.appError?.data);

    return appError ?
        (<Toast
            bg={'primary'}
            style={{position: "fixed", width: "75%", zIndex: 1000}}
            className="d-inline-block m-1 text-white"
            onClose={() => {
                actions.clickError()
            }}>
            <Toast.Header>
                <strong className="me-auto">APP_ERROR_INFO</strong>
            </Toast.Header>
            <Toast.Body>
                {appError}
            </Toast.Body>
        </Toast>) : null;
}

export default AppError;