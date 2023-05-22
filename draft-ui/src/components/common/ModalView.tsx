import {Button, Col, FloatingLabel, Form, Modal, Row} from "react-bootstrap";
import React, {useState} from "react";
import Icon from "./Icon";
import {IComment} from "../../models/models";

interface IProps {
    user?: string | null;
    show: boolean;
    accept?: () => void;
    cancel?: () => void;
    text: JSX.Element | string;
    id?: string;
    header?: string;
    comments?: IComment[];
    changeNotify?: (id: string, content?: IComment, isRemove?: boolean) => void;
    commentsBlockEnabled?: boolean;
    className?:string;
}

const ModalView = (props: IProps) => {
    const [showEditor, setShowEditor] = useState<boolean>(false);
    const [currentComment, setCurrentComment] = useState<IComment>();
    const renderCommentsBlock = (comments?: IComment[]): JSX.Element => {
        return <>
            <hr style={{margin:0}}/>
            {comments?.map(item => renderComment(item))}
            {showEditor && (<FloatingLabel className="mt-3" controlId="floatingTextarea2" label="Комментарий">
                <Form.Control
                    as="textarea"
                    style={{height: '100px'}}
                    value={currentComment?.content}
                    onChange={(event) => setCurrentComment({...currentComment, content: event.target.value})}
                />
            </FloatingLabel>)}
            <div className="me-2">
                {showEditor && (<Button className="me-2" onClick={() => {
                    props.changeNotify && props.changeNotify(props.id!, {
                        ...currentComment,
                        author: props.user!
                    }, false);
                    setShowEditor(false);
                }
                } variant="outline-success">Опубликовать</Button>)}
                <Button className="mt-2" onClick={() => {
                    setCurrentComment(undefined);
                    showEditor ? setShowEditor(false) : setShowEditor(true)
                }} variant="outline-success">
                    {showEditor ? 'Закрыть' : 'Добавить комментарий'}
                </Button>
            </div>
        </>
    }

    const renderComment = (comment: IComment): JSX.Element => {
        return <div key={comment.id} style={{backgroundColor: "#30c27e1a", borderRadius: 5, marginTop: 10}}>
            <Row>
                <Col>
                    <span className="ps-2">{comment.content}</span>
                    <hr style={{margin: 0}}/>
                </Col>
            </Row>
            <Row>
                <Col style={{position: "relative"}} className="ms-2">
                    {comment.author === props.user && (
                        <>
                            <Icon onClick={() => {
                                setCurrentComment(comment);
                                setShowEditor(true);
                            }} icon={"bi bi-pencil-fill"} size={15} title={'Редактировать'} hoverColor={'green'}/>
                            <span className="me-2"/>
                            <Icon onClick={() => {
                                props.changeNotify && props.changeNotify(props.id!, comment, true);
                            }}
                                  icon={"bi bi-trash3-fill"} size={15} title={'Удалить'} hoverColor={'green'}/>
                            <span className="me-2"/>
                        </>
                    )}
                    {comment.author !== props.user && (<Icon onClick={() => {
                        setCurrentComment({content: comment.author?.concat(', ')});
                        setShowEditor(true);
                    }}
                                                             icon={"bi bi-card-text"} size={15} title={'Ответить'}
                                                             hoverColor={'green'}/>)}
                    <span className="me-3" style={{position: "absolute", right: 0, fontSize: 12}}>
                           <Icon icon={"bi bi-person-fill"} size={15} title={''}/>
                    <span className="me-2" style={{fontSize: 12}}>
                        {comment.author}
                    </span>
                        {comment.date}
                    </span>
                </Col>
            </Row>
        </div>
    }

    return (
        <>
            <Modal className={props.className} show={props.show} onHide={props.cancel && (() => {
                props.cancel?.()
            })}>
                <Modal.Header className="hideCloseBtn" style={{
                    backgroundColor: "#212529",
                    color: "#dee2e6"
                }} closeButton>
                    <Modal.Title>{props.header ? props.header : 'Обратите внимание!'}</Modal.Title>
                </Modal.Header>
                <Modal.Body style={{
                    backgroundColor: "#212529",
                    color: "#dee2e6",
                    overflow: "auto",
                }}>
                    {typeof props.text === "string" ?
                        <div dangerouslySetInnerHTML={{__html: props.text}}/>
                        : props.text}
                    {!!props.commentsBlockEnabled && renderCommentsBlock(props.comments)}
                </Modal.Body>
                {props.accept && (<Modal.Footer style={{
                    backgroundColor: "#212529",
                    color: "#dee2e6"
                }}>
                    {props.cancel && (<Button variant="outline-light" onClick={() => {
                        props.cancel?.();
                    }}>
                        Отмена
                    </Button>)}
                    {props.accept && (<Button variant="outline-success" onClick={() => {
                        props.accept?.();
                    }}>
                        ОК
                    </Button>)}
                </Modal.Footer>)}
            </Modal>
        </>
    );
}

export default ModalView;
