import React, {Dispatch, useMemo, useState} from "react";
import Icon from "./Icon";
import {Button, ButtonGroup, Col, Form, Offcanvas, Row, Table} from "react-bootstrap";
import {EConsumer, EProducer, IStrategy} from "../../models/models";
import ModalView from "./ModalView";
import {useDispatch, useSelector} from "react-redux";
import {addNotification} from "../../actions/notificationActions";
import {IAppState} from "../../index";
import {useActions} from "../../hooks/hooks";
import {Service} from "../../services/Service";
import {isEmpty} from "lodash";
import {calcDataForGraphProfit, copyTextToClipboard} from "../../utils/utils";
import {Chart} from "../Chart";
import SelectFilter from "./SelectFilter";

const columns = [
    {field: 'id', fieldName: '№'},
    {field: 'name', fieldName: 'Наименование'},
    {field: 'producer', fieldName: 'Поставщик'},
    {field: 'ticker', fieldName: 'Тикер'},
    {field: 'consumer', fieldName: 'Получатель'},
    {field: 'status', fieldName: 'Статус'},
    {field: 'currentPosition', fieldName: 'Текущая позиция'},
    {filed: 'fixProfit', fieldName: 'Тек. доход'},
    {field: '', fieldName: ''},
];

const MyTable: React.FC = () => {
    const userName = localStorage.getItem("userName");
    const [showGraph, setShowGraph] = useState<any>({show: false, idStrategy: ""});
    const [showDescriptionModal, setShowDescriptionModal] = useState<any>({show: false, name: undefined});
    const actions = useActions(Service);
    const dispatch: Dispatch<any> = useDispatch();
    const [hoverTable, setHoverTable] = useState(-1);
    const [clickTable, setClickTable] = useState(-1);
    const [editedRow, setEditedRow] = useState<any>();
    const [showRemoveModal, setIsRemoveModal] = useState(false);
    const [showModal, setIsShowModal] = useState(false);
    const [validation, setValidation] = useState<any>({
        name: 'Поле обязательно для заполнения!',
        ticker: 'Поле обязательно для заполнения!'
    });
    const strategy: IStrategy[] = useSelector((state: IAppState) =>
        state.strategy.data);

    const handleChangeEdit = (e: any, rowId?: any) => {
        const {name, value} = e.target;
        console.log(name, value);
        if (name === 'isActive') {
            let changedRow: IStrategy = strategy.find(i => i.id === rowId)!;
            actions.addOrUpdateStrategy(userName!, {...changedRow, [name]: !changedRow?.isActive}, () => {
                dispatch(addNotification("Info", `Запись успешно сохранена!`))
            }, (error: any) => dispatch(addNotification("Info", error)));
        } else {
            if (!value) {
                setValidation({...validation, [name]: 'Поле обязательно для заполнения!'});
                setEditedRow({...editedRow, [name]: value});
                return;
            }
            if (name === 'name' && (!!strategy.find(i => i.name === value))) {
                setValidation({...validation, [name]: 'Такое наименование уже есть!'});
                return;
            }
            if (name === 'customer-test') {
                setEditedRow({...editedRow, customer: [EConsumer.TEST]});
                return;
            }
            setValidation({...validation, [name]: undefined});
            setEditedRow({...editedRow, [name]: value});
        }
    }


    const handleSave = () => {
        const hasError = Object.values(validation).some((i: any) => {
            return !!i;
        });
        if (!hasError) {
            actions.addOrUpdateStrategy(userName!, editedRow, () => {
                dispatch(addNotification("Info", `Запись успешно сохранена!`))
            }, (error: any) => dispatch(addNotification("Info", error)));
            setIsShowModal(false);
        }
    }

    const acceptRemove = () => {
        if (showRemoveModal) {
            actions.removeStrategy(userName!, editedRow?.name);
            setIsRemoveModal(false);
        }
    }

    const renderRemoveModal = () => {
        return <Row>
            <Col className={'pt-2'}>
                {showRemoveModal && <ModalView text={`Вы действительно хотите удалить стратегию?`} show={true}
                                               accept={acceptRemove} cancel={() => {
                    setIsRemoveModal(false)
                }}/>}
            </Col>
        </Row>
    }

    const renderEditModal = () => {
        return <Offcanvas style={{
            height: 500, width: 350, margin: "0px 170px 0 auto",
            backgroundColor: "rgb(30 30 47 / 92%)",
            color: "#dee2e6",
        }} placement={"top"} show={showModal}
                          onHide={() => {
                              setIsShowModal(false)
                          }}>
            <Offcanvas.Header closeButton>
                <Offcanvas.Title>{editedRow.id?`Редактирование стратегии №:${editedRow.id}`:'Добавление новой стратегии'}</Offcanvas.Title>
            </Offcanvas.Header>
            <Offcanvas.Body>
                <Form>
                    <Form.Select disabled name="producer" style={{width: 300}}>
                        <option value="1">{EProducer.TKS}</option>
                        <option value="2">{EProducer.ALOR}</option>
                    </Form.Select>
                    <br/>
                    <Form.Control
                        style={{width: 300}}
                        name='name'
                        defaultValue={editedRow?.name}
                        onChange={handleChangeEdit}
                        type="text"
                        placeholder="Наименование стратегии"
                        isInvalid={!isEmpty(validation?.name)}
                    />
                    <Form.Control.Feedback type="invalid">{validation.name}</Form.Control.Feedback>
                    <SelectFilter value={editedRow?.ticker ? {value: editedRow.ticker, label: editedRow.ticker} : null}
                                  onChange={handleChangeEdit} errorMsg={validation.ticker}/>
                    <Form.Group className="mb-3" controlId="exampleForm.ControlTextarea1">
                        <Form.Control placeholder={'Описание стратегии'} as="textarea" rows={3}/>
                    </Form.Group>
                    <Form.Check
                        type="switch"
                        name='customer-terminal'
                        id="custom-switch"
                        label="Терминал"
                        onChange={handleChangeEdit}
                        disabled
                    />
                    <Form.Check
                        type="switch"
                        name='customer-test'
                        id="custom2-switch"
                        onChange={handleChangeEdit}
                        label="Эмуляция"
                        disabled
                        checked
                    />
                    <br/>
                    <Button className={"me-2"} variant="outline-light" onClick={() => {
                        setIsShowModal(false)
                    }}>
                        Закрыть
                    </Button>
                    <Button variant="outline-success" onClick={handleSave}>
                        <Icon icon={"bi bi-save"} size={15} title={''} text={' Сохранить'}/>
                    </Button>
                </Form>
            </Offcanvas.Body>
        </Offcanvas>
    }

    const handleAddRow = () => {
        setEditedRow({
            id: undefined,
            name: undefined,
            producer: EProducer.TKS,
            ticker: '',
            position: 0,
            slippage: 0,
            consumer: [EConsumer.TEST],
            isActive: true,
        });
        setValidation({
            name: 'Поле обязательно для заполнения!',
            ticker: 'Поле обязательно для заполнения!'
        });
        setIsShowModal(true);
    }

    const handleEdit = (rowId: any) => {
        const data = strategy.find((i: any) => i?.id === rowId);
        if (data) {
            setEditedRow(data);
            setValidation({...validation, name: undefined, ticker: undefined});
            setIsShowModal(true);
        }
    }
    const handleRemove = (rowId: any) => {
        setEditedRow(strategy.find((i: any) => {
            return i.id === rowId
        }));
        setIsRemoveModal(true);
    }

    const renderDescriptionModal = () => {
        const text = `{"direction": "{{strategy.order.action}}",
                "name":"${showDescriptionModal.name}",
                "userName":"${userName}",
                "ticker": "RIH3",
                "quantity": "{{strategy.position_size}}",
                "consumer": ["test"],
                "priceTv":"{{strategy.order.price}}"}`;
        return <ModalView header={'Как добавить стратегию в Tradingview:'} show={showDescriptionModal.show}
                          cancel={() => {
                              setShowDescriptionModal({show: false, name: undefined})
                          }} text={<div>
            <h6 className="mt-3">Как мне создать оповещения для стратегии?</h6>
            <ul>
                <li>Нажать на <em>Добавить оповещение</em> в панели <em dir="ltr">Тестера стратегий.</em><br/>
                    <img style={{width: 434, maxHeight: 100}}
                         src="https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/43379926138/original/4XPI1kKX4FBOhwhvmKYybJPz3xhgXJKP2Q.jpg?1671202826"
                         alt="альтернативный текст"/>
                </li>
                <li>Воспользоваться кнопкой <em>Добавить оповещение для&nbsp;</em>в выпадающем меню стратегии.<br/>
                    <img style={{width: 434, maxHeight: 300}}
                         src="https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/43379926209/original/sHYLtSKfhC0C-lJfgUJvX21ivo21maS8sA.jpg?1671202839"/>
                </li>
                <li>Выбрать свою стратегию в диалоговом окне создания оповещений.<br/>
                    <img style={{width: 434, maxHeight: 480}}
                         src="https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/43379926246/original/BNHxg5elv4wUq_R5vih05E9FTlZ__yNx1w.jpg?1671202849"/>
                </li>
                <li>
                    Поле <em>Message</em> заполнить текстом ниже. <br/>
                    <div className="mt-2 text-bg-success">{text}|</div>
                    <br/>
                    <Button className="me-2" onClick={(e: any) => {
                        void copyTextToClipboard(text);
                        const x = document.getElementById('test');
                        x!.innerText = 'Скопировано!'
                    }} variant={"outline-success"}>
                        Скопировать
                    </Button>
                    <span id="test"/>
                </li>
            </ul>
        </div>}/>
    }

    const calcChart = calcDataForGraphProfit(strategy);

    return <>
        {showModal && renderEditModal()}
        {showDescriptionModal.show && renderDescriptionModal()}
        {renderRemoveModal()}
        {showGraph.show && (<ModalView
            show={showGraph.show}
            cancel={() => setShowGraph({show: false, idStrategy: undefined})}
            header={'График доходности'}
            text={<Chart data={calcChart.find((i: any) => {
                return i.id === Number(showGraph.idStrategy)
            })?.graphResult || []}/>}
        />)}
        <Button
            style={{width: 240}}
            className={"mb-1 ms-3"}
            onClick={handleAddRow}
            variant={"outline-secondary"}
        >
            <Icon icon={'bi bi-plus-square'} size={15} title={''} text={' Добавить стратегию'}/>
        </Button>
        <br/>
        <Table style={{color: "rgb(140, 144, 154)"}} className={"ms-3 w-75"} bordered
               variant="outline">
            <thead>
            <tr key={"header"}>
                {columns.map((column) => {
                    return <th key={column.field}>{column.fieldName}</th>
                })}
            </tr>
            </thead>
            <tbody>
            {strategy.length > 0 ? strategy?.map((row, index) => {
                return <tr key={row.id! + 1000}
                           style={hoverTable === index && clickTable !== index ? {
                               color: "white",
                               backgroundColor: "lightgray"
                           } : clickTable === index ? {backgroundColor: "lightblue", color: "white"} : {}}
                           onClick={() => {
                               setClickTable(index)
                           }} onMouseLeave={() => {
                    setHoverTable(-1)
                }} onMouseEnter={() => {
                    setHoverTable(index)
                }}
                >
                    <td className={"align-middle"}>{index + 1}</td>
                    <td className={"align-middle"}>{row?.name}</td>
                    <td className={"align-middle"}>{'TKS'}</td>
                    <td className={"align-middle"}>{row?.ticker}</td>
                    <td className={"align-middle"}>{row.consumer && row.consumer.toString().replace(',', '/')}</td>
                    <td className={"align-middle"}>
                        <Form.Check
                            id="custom-switch"
                            name="isActive"
                            checked={row.isActive}
                            type="switch"
                            label="Вкл/Выкл"
                            onChange={(e) => {
                                handleChangeEdit(e, row?.id)
                            }}
                        />
                    </td>
                    <td style={row.currentPosition ? (row.currentPosition > 0 ? {backgroundColor: "lightgreen"} : row.currentPosition < 0 ? {backgroundColor: "lightpink"} : {backgroundColor: "lightgreen"}) : undefined}
                        className={"align-middle text-center"}>
                        {row.currentPosition || 0}
                    </td>
                    <td className={"align-middle text-center"}>
                        {calcChart.find((i: any) => {
                            return i.id === Number(row.id)
                        })?.graphResult?.slice(-1)[0]?.y || 0}
                    </td>
                    <td>
                        <ButtonGroup className={"me-2"}>
                            <Button
                                onClick={
                                    () =>
                                        handleEdit(row?.id)
                                }
                                variant={"outline-success"}>
                                <Icon icon={"bi bi-pencil-square"} size={15} title={''}/>
                            </Button>
                        </ButtonGroup>
                        <ButtonGroup className={"me-2"}>
                            <Button onClick={() => {
                                handleRemove(row?.id)
                            }} variant={"outline-danger"}>
                                <Icon icon={"bi bi-trash"} size={15} title={''}/>
                            </Button>
                        </ButtonGroup>
                        <ButtonGroup className={"me-2"}>
                            <Button onClick={() => {
                                setShowGraph({show: true, idStrategy: row.id})
                            }} variant={"dark"}>
                                <Icon icon={"bi bi-graph-up"} size={15} title={'View Chart'}/>
                            </Button>
                        </ButtonGroup>
                        <ButtonGroup>
                            <Button onClick={() => {
                                setShowDescriptionModal({show: true, name: row.name})
                            }} variant={"dark"}>
                                <Icon icon={"bi bi-info-circle"} size={15} title={''}/>
                            </Button>
                        </ButtonGroup>
                    </td>
                </tr>
            }) : <tr key={'notFound'}>
                <td colSpan={9}>{"Вы пока не завели ни одной стратегии..."}</td>
            </tr>
            }
            </tbody>
        </Table>
    </>
}

export default MyTable;

