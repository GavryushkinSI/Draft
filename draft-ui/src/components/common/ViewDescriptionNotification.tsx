import ModalView from "./ModalView";

interface IProps{
    type?:string;
    show:boolean;
    message?:any;
    header?:any;
    cancel?:()=>void;
    id?:string;
}

const ViewDescriptionNotification =(props: IProps)=>{
    return <ModalView header={props.header} show={props.show} text={props.message} cancel={props.cancel}/>
}

export default ViewDescriptionNotification;