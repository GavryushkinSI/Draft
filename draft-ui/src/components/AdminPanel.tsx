import React, {useState} from "react";
import {useActions} from "../hooks/hooks";
import {AdminServices} from "../services/AdminServices";
import {Button} from "react-bootstrap";
import MyEditor from "./common/MyEditor";

const AdminPanel: React.FC = () => {
    const actions: AdminServices = useActions(AdminServices);
    const [data, setData] = useState<any>();
    const user = localStorage.getItem("userName");

    return user === 'Admin' ? (<>
        <h5 style={{backgroundColor: "lightblue"}}><span className="ms-3">{'Админ панель'}</span></h5>
        <Button
            className="ms-2"
            variant={"info"}
            onClick={() => {
                actions.getData().then((response) => {
                    setData(response.data);
                })
            }}>
            {'Получить'}
        </Button>
        <h6 className="ms-2">{'Данные по тикерам:'}</h6>
        {data?.length > 0 && (<ul>
            {data.map((i: any, index: number) => {
                return <li
                    key={index}>{i.figi + ' => ' + i.price + ' => ' + i.updateTime}</li>
            })}
        </ul>)}
        <MyEditor isCreateArticleMode/>
    </>) : (<>{'403 FORBIDDEN'}</>);
}

export default AdminPanel;