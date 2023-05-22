import React, {useState} from "react";
import {Button, Card, CardGroup, Col, Row} from "react-bootstrap";
import Icon from "./common/Icon";
import {useNavigate} from "react-router-dom";
import {ROOT} from "../Route/consts";
import {IArticle} from "../models/models";
import {stringTruncate} from "../utils/utils";
import {includes} from "lodash";
import RowFiled from "./common/RowFiled";
import {Service} from "../services/Service";
import {useActions, useMountEffect} from "../hooks/hooks";
import {AddService} from "../services/AddService";
import {useSelector} from "react-redux";
import {IAppState} from "../index";

interface IProps {
    articles?: IArticle[];
}

const Articles: React.FC<IProps> = (props: IProps) => {
    const navigate = useNavigate();
    const actions: AddService = useActions(AddService);
    const [expendedIds, setExpendedIds] = useState<any[]>([]);
    const user: any = useSelector((state: IAppState) =>
        state.user.data);

    useMountEffect(() => {
        if (user?.notifications?.length === 0) {
            actions.getAllArticles();
        }
    })

    return <Row className="bg-custom-1 text-white-50 vh-100">
        <Col>
            <Row>
                <Col>
                    <Button style={{marginLeft: 13}}>
                        <Icon
                            color={"lightblue"}
                            hoverColor={"lightgreen"}
                            onClick={async () => navigate(ROOT().DRAFT.MAIN_PAGE.PATH)} text={'На главную'}
                            icon={"bi bi-backspace"} size={15} title={''}/>
                    </Button>
                </Col>
            </Row>
            <Row>
                <Col className="ms-4 mt-2">
                    {user?.notifications.map((item: any) => {
                        return <div style={{maxWidth: 1000, border: "1px solid lightgray", overflow: "auto"}}>
                            <div className="ms-2">
                                <h4 style={{color:"lightblue"}} onClick={() => {
                                    if (includes(expendedIds, item.id)) {
                                        setExpendedIds(expendedIds.filter(i => i !== item.id));
                                    } else {
                                        setExpendedIds([...expendedIds, item.id]);
                                    }
                                }} className="articleHeader">
                                    {item.header}
                                </h4>
                                <p style={{fontSize: 10, marginTop: -10, marginBottom: 0}}>
                                    {item.time}
                                </p>
                                <p style={{marginBottom: 2}}>
                                    {includes(expendedIds, item.id) ?
                                        <div dangerouslySetInnerHTML={{__html: item.message}}/> : <>
                                            <div dangerouslySetInnerHTML={{__html: stringTruncate(item.message, 200)}}/>
                                            <br/>
                                            <span style={{cursor: "pointer", textDecorationLine: "underline"}}
                                                  onClick={() => {
                                                      setExpendedIds([...expendedIds, item.id])
                                                  }}>(Читать дальше)</span>
                                        </>
                                    }
                                </p>
                                <hr style={{margin: 0}}/>
                                <span>{`Комментарии(${item?.comments?.length || 0})`}</span>
                            </div>
                        </div>
                    })}
                </Col>
            </Row>
        </Col>
    </Row>
}

export default Articles;
