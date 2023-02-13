import React, { Dispatch } from "react";
import { useDispatch, useSelector } from "react-redux";
import {INotification} from "../../reducers/notificationReducers";
import {IAppState} from "../../index";
import { removeNotification } from "../../actions/notificationActions";
import {Toast} from "react-bootstrap";


const Notifications: React.FC = () => {
    const dispatch: Dispatch<any> = useDispatch();
    const notifications: INotification[] = useSelector((state: IAppState) =>
        state.notifications.notifications);

    function closeNotification(id: number) {
        dispatch(removeNotification(id));
    }

    const notificationList = notifications.map((notification, index) => {
        return (<Toast key={notification.id} className="mb-1" onClose={()=>{closeNotification(notification.id)}} delay={3000} autohide>
            <Toast.Header style={{backgroundColor:"rgb(30 30 47 / 92%)"}}>
                <img src="holder.js/20x20?text=%20" className="rounded me-2" alt="" />
                <strong className="me-auto">{notification.title}</strong>
                <small>{notification.date.toLocaleTimeString(navigator.language, { hour: '2-digit', minute: '2-digit' })}</small>
            </Toast.Header>
            <Toast.Body style={{backgroundColor:"#2e32df12"}}>{notification.text}</Toast.Body>
        </Toast>)
    });

    return (
        <div className="toast-wrapper">
            {notificationList}
        </div>
    );
};

export default Notifications;
