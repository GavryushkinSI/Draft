import {ADD_NOTIFICATION, REMOVE_NOTIFICATION} from "../actions/notificationActions";

const initialState: INotificationState = {
    notifications: []
};

export interface INotification {
    id: number,
    date: Date,
    title: string,
    text: string
}

export interface INotificationState {
    notifications: INotification[];
}

function notificationReducer(state: INotificationState = initialState, action: any): INotificationState {
    switch (action.type) {
        case ADD_NOTIFICATION: {
            let maxId: number= Math.max.apply(Math, state.notifications.map(o =>  o.id));
            if(maxId === -Infinity) { maxId = 0; }
            let newItem = {
                id: maxId + 1,
                date: new Date(),
                title: action.title,
                text: action.text
            };
            return {...state, notifications: [...state.notifications, newItem]};
        }
        case REMOVE_NOTIFICATION: {
            return {...state, notifications: state.notifications
                    .filter(Notification => Notification.id !== action.id)};
        }
        default:
            return state;
    }
}


export default notificationReducer;