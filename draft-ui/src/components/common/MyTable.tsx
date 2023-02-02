import React, {Dispatch, useState} from "react";
import Icon from "./Icon";
import {Button, ButtonGroup, Col, Form, Offcanvas, Row, Table} from "react-bootstrap";
import {EProducer, IStrategy} from "../../models/models";
import ModalView from "./ModalView";
import {useDispatch, useSelector} from "react-redux";
import {addNotification} from "../../actions/notificationActions";
import {IAppState} from "../../index";
import {useActions} from "../../hooks/hooks";
import {Service} from "../../services/Service";
import {isEmpty} from "lodash";
import {Chart} from "../Chart";
import {calcDataForGraphProfit} from "../../utils/utils";

const columns = [
    {field: 'id', fieldName: '№'},
    {field: 'name', fieldName: 'Наименование'},
    {field: 'producer', fieldName: 'Поставщик'},
    {field: 'ticker', fieldName: 'Тикер'},
    {field: 'consumer', fieldName: 'Получатель'},
    {field: 'status', fieldName: 'Статус'},
    {field: '', fieldName: ''},
];

const MyTable: React.FC = () => {
    const userName=localStorage.getItem("userName");
    const [showGraph, setShowGraph] = useState<any>({show:false, idStrategy: ""});
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
        if (name === 'isActive') {
            let changedRow: IStrategy = strategy.find(i => i.id === rowId)!;
            actions.addOrUpdateStrategy(userName!,{...changedRow, [name]: !changedRow?.isActive}, () => {
                dispatch(addNotification("Info", `Запись успешно сохранена!`))
            }, (error: any) => dispatch(addNotification("Info", error)));
        } else {
            if (!value) {
                setValidation({...validation, [name]: 'Поле обязательно для заполнения!'});
                return;
            }
            if (name === 'name' && (!!strategy.find(i => i.name === value))) {
                setValidation({...validation, [name]: 'Такое наименование уже есть!'});
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
        console.log(hasError, validation);
        if (!hasError) {
            actions.addOrUpdateStrategy(userName!,editedRow, () => {
                dispatch(addNotification("Info", `Запись успешно сохранена!`))
            }, (error: any) => dispatch(addNotification("Info", error)));
            setIsShowModal(false);
        }
    }

    const acceptRemove = () => {
        if (showRemoveModal) {
            actions.removeStrategy(userName!,editedRow?.name);
            setIsRemoveModal(false);
        }
    }

    const renderRemoveModal = () => {
        return <Row>
            <Col className={'pt-2'}>
                {showRemoveModal && <ModalView text={'Вы действительно хотите удалить стратегию?'} show={true}
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
                <Offcanvas.Title>{`Редактирование стратегии №:${editedRow?.id}`}</Offcanvas.Title>
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
                    <br/>
                    <Form.Control
                        style={{width: 300}}
                        name='ticker'
                        defaultValue={editedRow?.ticker}
                        onChange={handleChangeEdit}
                        type="text"
                        placeholder="Тикер"
                        isInvalid={!isEmpty(validation?.ticker)}
                    />
                    <Form.Control.Feedback type="invalid">{validation.ticker}</Form.Control.Feedback>
                    <br/>
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
                        id="custom-switch"
                        onChange={handleChangeEdit}
                        label="Эмуляция"
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
            id:undefined,
            name: undefined,
            producer: EProducer.TKS,
            ticker: '',
            position: 0,
            slippage: 0,
            consumer: [],
            isActive: false,
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
        setEditedRow(strategy.find((i:any)=>{return i.id===rowId}));
        setIsRemoveModal(true);
    }

    return <>
        {showModal && renderEditModal()}
        {renderRemoveModal()}
        {showGraph.show&&(<ModalView
            show={showGraph.show}
            cancel={()=>setShowGraph({show:false, idStrategy: undefined})}
            header={'График доходности'}
            text={<Chart data={calcDataForGraphProfit(strategy.find(i=>i.id===showGraph.idStrategy)!.orders!).graphResult}/>}
        />)}
        <Button
            className={"mb-1 ms-3"}
            onClick={handleAddRow}
            variant={"outline-secondary"}
        >
            <Icon icon={'bi bi-plus-square'} size={15} title={''} text={' Добавить стратегию'}/>
        </Button>
        <br/>
        <Table style={{color: "rgb(140, 144, 154)"}} className={"ms-3 w-75"} bordered variant="outline">
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
                        <ButtonGroup  className={"me-2"}>
                            <Button onClick={() => {
                                handleRemove(row?.id)
                            }} variant={"outline-danger"}>
                                <Icon icon={"bi bi-trash"} size={15} title={''}/>
                            </Button>
                        </ButtonGroup>
                        <ButtonGroup>
                            <Button onClick={() => {setShowGraph({show:true, idStrategy: row.id})}} variant={"dark"}>
                                <Icon icon={"bi bi-graph-up"} size={15} title={'View Chart'}/>
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

