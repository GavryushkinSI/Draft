import ModalView from "./ModalView";
import {useSelector} from "react-redux";
import {IAppState} from "../../index";
import {Service} from "../../services/Service";
import {useActions} from "../../hooks/hooks";
import {IComment} from "../../models/models";

interface IProps {
    type?: string;
    show: boolean;
    message?: any;
    comments?: any;
    header?: any;
    cancel?: () => void;
    id?: string;
    user?: string | null;
    commentsBlockEnabled?:boolean;
    className?:string;
}

const ViewDescriptionNotification = (props: IProps) => {
    const user: any = useSelector((state: IAppState) => state.user.data);
    const actions: Service = useActions(Service);

    const changeNotify = (id: string, content?: IComment, isRemove?: boolean) => {
        actions.changeNotify(id, user, content, isRemove);
    }

    return <ModalView className={props.className} commentsBlockEnabled={props.commentsBlockEnabled} changeNotify={changeNotify} user={props.user} id={props.id} header={props.header}
                      comments={props.comments} show={props.show} text={props.message} cancel={props.cancel}/>
}

export default ViewDescriptionNotification;