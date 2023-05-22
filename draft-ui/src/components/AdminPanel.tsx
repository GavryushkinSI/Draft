import React, {useState} from "react";
import {useActions} from "../hooks/hooks";
import {AdminServices} from "../services/AdminServices";
import {Button, ButtonGroup, Col, Row} from "react-bootstrap";
import MyEditor from "./common/MyEditor";
import Icon from "./common/Icon";
import {ROOT} from "../Route/consts";
import {useNavigate} from "react-router-dom";

const AdminPanel: React.FC = () => {
    const actions: AdminServices = useActions(AdminServices);
    const [addNewArticle, setAddArticle] = useState<boolean>(false);
    const [data, setData] = useState<any>();
    const user = localStorage.getItem("userName");
    const navigate = useNavigate();

    return user === 'Admin' ? (<div className="bg-custom-2" style={{height:"100vh"}}>
        <h5 className="bg-custom-1 text-white"><span className="ms-3">{'Админ панель'}</span></h5>
        <Button className="ms-2 mb-2" variant="dark">
            <Icon
                hoverColor={"lightgreen"}
                onClick={async () => navigate(ROOT().DRAFT.MAIN_PAGE.PATH)} text={'На главную'} icon={"bi bi-backspace"}
                size={15} title={''}/>
        </Button>
        <br/>
        <Row>
            <Col>
                <ButtonGroup className="bg-custom-2">
                    <Button className="ms-2" variant="secondary" onClick={() => {
                        addNewArticle ? setAddArticle(false) : setAddArticle(true)
                    }}>{addNewArticle ? 'Отменить' : 'Создать статью'}</Button>
                    <Button
                        className="ms-2 me-2"
                        variant={"success"}
                        onClick={() => {
                            actions.getData().then((response) => {
                                setData(response.data);
                            })
                        }}>
                        {'GET_TICKERS_INFO'}
                    </Button>
                </ButtonGroup>
            </Col>
        </Row>
        <h6 className="me-5 ms-2">{'Данные по тикерам:'}</h6>
        {data?.length > 0 && (<ul>
            {data.map((i: any, index: number) => {
                return <li
                    key={index}>{i.figi + ' => ' + i.price + ' => ' + i.updateTime}</li>
            })}
        </ul>)}
        <Row>
            <Col className="me-5">
                <MyEditor isCreateArticleMode addNewArticle={addNewArticle} cancel={() => {
                    setAddArticle(false)
                }} renderHeader/>
            </Col>
        </Row>
    </div>) : (<Row style={{height: "100vh", position: "relative"}}>
        <Col style={{textShadow: "3px 3px 2px rgba(56, 54, 201, 0.8)"}}>
            <div style={{position: "absolute", top: "50%", left: "50%", fontSize: 30, color: "white"}}>
                ДОСТУП К ЭТОЙ СТРАНИЦЕ ЗАПРЕЩЁН...
            </div>
        </Col>
    </Row>);
}

export default AdminPanel;