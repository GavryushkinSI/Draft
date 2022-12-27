import ModalView from "./ModalView";
import RowFiled from "./RowFiled";

interface IProps{
    show:boolean;
}

const Settings =(props:IProps)=>{
    const content=():JSX.Element=>{
        return <>
            <RowFiled>
                {''}
            </RowFiled>
        </>
    }
    return <ModalView show={props.show}
                      accept={()=>{}}
                      cancel={()=>{}}
                      text={content()}/>
}

export default Settings;
