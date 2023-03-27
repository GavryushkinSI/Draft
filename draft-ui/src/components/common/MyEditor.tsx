import React, {Dispatch, useState} from "react";
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';
import {Button, Col, FloatingLabel, Form, Row} from "react-bootstrap";
import RowFiled from "./RowFiled";
import {AdminServices} from "../../services/AdminServices";
import {useActions} from "../../hooks/hooks";
import {useDispatch} from "react-redux";
import {addNotification} from "../../actions/notificationActions";
import {IArticle} from "../../models/models";
import {Editor} from "draft-js";

interface IProps {
    isCreateArticleMode?: boolean;
    sendArticle?: () => void;
    isEditor?: boolean;
}

const MyEditor = (props: IProps) => {
    const actions: AdminServices = useActions(AdminServices);
    const dispatch: Dispatch<any> = useDispatch();
    const [value, setValue] = useState('');
    const [otherParamArticle, setOtherParamArticle] = useState<any>({header: '', blockCommentEnabled: false})
    const [addNewArticle, setAddArticle] = useState<boolean>(false);

    const addArticle = () => {
        const notification: IArticle = {
            message: value,
            blockCommentEnabled: otherParamArticle.blockCommentEnabled,
            header: otherParamArticle.header
        };
        void actions.addArticle(notification, () => {
            setValue('');
            dispatch(addNotification("Удачно...", "Статья добавлена!"));
        }, () => {
            dispatch(addNotification("Ошибка...", "Не удалось добавить статью!"));
        });
        setAddArticle(false);
    }

    const modules = {
        toolbar: [
            [{'header': '1'}, {'header': '2'}, {'font': []}],
            [{size: []}],
            ['bold', 'italic', 'underline', 'strike', 'blockquote'],
            [{'list': 'ordered'}, {'list': 'bullet'},
                {'indent': '-1'}, {'indent': '+1'}],
            ['link', 'image', 'video', 'code-block'],
            ['clean'], ['clean'],
            [{color: []}, {background: []}],
            [{font: []}],
            [{align: []}],
        ],
        clipboard: {
            matchVisual: false,
        }
    }
    /*
     * Quill editor formats
     * See https://quilljs.com/docs/formats/
     */
    const formats = [
        'header', 'font', 'size',
        'bold', 'italic', 'underline', 'strike', 'blockquote',
        'list', 'bullet', 'indent',
        'link', 'image', 'video', 'color', 'align',
    ]

    return (
        <>
            <Row>
                <Col>
                    <Button className="ms-2" variant="secondary" onClick={() => {
                        addNewArticle ? setAddArticle(false) : setAddArticle(true)
                    }}>{addNewArticle ? 'Отменить' : 'Создать статью'}</Button>
                </Col>
            </Row>
            <RowFiled>
                {addNewArticle && (
                    <>
                        {props.isCreateArticleMode && (<h5>Создание новой статьи</h5>)}
                        <FloatingLabel className="mb-3" controlId="floatingTextarea2" label="Заголовок">
                            <Form.Control
                                as="textarea"
                                value={otherParamArticle.header}
                                onChange={(event => setOtherParamArticle({
                                    ...otherParamArticle,
                                    header: event.target.value
                                }))}
                            />
                        </FloatingLabel>
                        <Form.Check
                            type="switch"
                            className="mb-3"
                            id="custom-switch"
                            label="Доступность комментариев"
                            value={otherParamArticle.blockCommentEnabled}
                            onChange={() => setOtherParamArticle({
                                ...otherParamArticle,
                                blockCommentEnabled: !otherParamArticle.blockCommentEnabled
                            })}
                        />
                        <ReactQuill
                            className="mb-2"
                            modules={modules}
                            formats={formats}
                            theme="snow"
                            value={value}
                            onChange={setValue}/>
                        {props.isCreateArticleMode &&
                            (<Button
                                variant={"success"}
                                onClick={addArticle}>
                                {'Добавить статью'}
                            </Button>)}
                    </>)}
            </RowFiled>
        </>
    );
}

export default MyEditor;