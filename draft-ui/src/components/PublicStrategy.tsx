import React from "react";
import {Button, Col, Row} from "react-bootstrap";
import {useNavigate} from "react-router-dom";
import Icon from "./common/Icon";
import {ROOT} from "../Route/consts";

const PublicStrategy: React.FC = () => {
    const navigate = useNavigate();
    return <Row style={{height: "100vh", position: "relative"}} className="ms-2 mt-2 bg-custom-1"><Col
        style={{textShadow: "3px 3px 2px rgba(56, 54, 201, 0.8)"}}>
        <Button>
            <Icon onClick={async () => navigate(ROOT().DRAFT.MAIN_PAGE.PATH)} text={'На главную'}
                  hoverColor={"lightgreen"}
                  icon={"bi bi-backspace"} size={22} title={''}/>
        </Button>
        <h1>Публичные стратегии</h1>
        <h6>В данном разделе отображаются стратегии пользователей</h6>
        <div style={{position: "absolute", top: "50%", left: "35%", fontSize: 30, color: "white"}}>СТРАНИЦА В
            РАЗРАБОТКЕ...
        </div>
    </Col>
    </Row>
}

export default PublicStrategy;