import React, {Dispatch, useMemo, useState} from "react";
import Icon from "./Icon";
import {Button, ButtonGroup, Col, Form, FormGroup, Offcanvas, Row, Table} from "react-bootstrap";
import {EProducer, IStrategy} from "../../models/models";
import ModalView from "./ModalView";
import {useDispatch, useSelector} from "react-redux";
import {addNotification} from "../../actions/notificationActions";
import {IAppState} from "../../index";
import {useActions} from "../../hooks/hooks";
import {Service} from "../../services/Service";
import {isEmpty, noop} from "lodash";
import {calcDataForGraphProfit, copyTextToClipboard, formatNumber} from "../../utils/utils";
import {Chart} from "../Chart";
import SelectFilter from "./SelectFilter";
import RowFiled from "./RowFiled";
import MyToolTip from "./MyToolTip";
import Gap from "./Gap";
import MyEditor from "./MyEditor";
import Portfolio from "../Portfolio";
import NameComponent from "./NameComponent";
import LastPrice from "./LastPrice";

const columns = [
    {field: 'id', fieldName: '№'},
    {field: 'name', fieldName: 'Наименование'},
    {field: 'producer', fieldName: 'Поставщик'},
    {field: 'ticker', fieldName: 'Тикер'},
    {field: 'consumer', fieldName: 'Получатель'},
    {field: 'status', fieldName: 'Статус'},
    {field: 'currentPosition', fieldName: 'Текущая позиция'},
    {filed: 'fixProfit', fieldName: 'Тек. доход'},
    // {filed: 'nonFixProfit', fieldName: 'Ср.цена посл.сд.'},
    {filed: 'lastPrice', fieldName: 'Посл. цена'},
    {field: '', fieldName: ''},
];

const MyTable: React.FC = () => {
    const userName = localStorage.getItem("userName");
    const [showGraph, setShowGraph] = useState<any>({show: false, nameStrategy: ""});
    const [showDescriptionModal, setShowDescriptionModal] = useState<any>({show: false, strategy: undefined});
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

    const user: any = useSelector((state: IAppState) =>
        state.user.data);

    const handleChangeEdit = (e: any, rowId?: any) => {
        const {name, value} = e.target;

        if (name === 'isActive') {
            let changedRow: IStrategy = strategy.find(i => i.id === rowId)!;
            actions.addOrUpdateStrategy(userName!, {...changedRow, [name]: !changedRow?.isActive}, () => {
                dispatch(addNotification("Info", `Запись успешно сохранена!`))
            }, (error: any) => dispatch(addNotification("Info", error)));
        } else {
            if(name==='producer'){
                const producer:string= value==2?'BYBIT':'TKS';
                setEditedRow({...editedRow, producer});
                return;
            }
            if (!value && name !== 'description') {
                setValidation({...validation, [name]: 'Поле обязательно для заполнения!'});
                setEditedRow({...editedRow, [name]: value});
                return;
            }
            if (name === 'name' && (!!strategy.find(i => i.name === value))) {
                setValidation({...validation, [name]: 'Такое наименование уже есть!'});
                return;
            }
            if (name === 'name') {
                if (value?.length >= 20) {
                    setValidation({...validation, [name]: 'Наименование должно быть не больше 20 символов!'});
                    return;
                }
            }
            if (name === 'description' && value?.length >= 2000) {
                setValidation({...validation, [name]: 'Описание должно быть не больше 2000 символов!'});
            }
            if (['test', 'telegram', 'terminal'].includes(name) && editedRow.consumer) {
                if (editedRow.consumer.includes(name)) {
                    const consumer = editedRow.consumer.filter((i: any) => i !== name);
                    if (name === 'terminal') {
                        consumer.push('test');
                    }
                    setEditedRow({...editedRow, consumer});
                } else {
                    let consumer = [...editedRow.consumer, name];
                    if (name === 'terminal') {
                        consumer = consumer.filter(i => i != 'test');
                    }
                    setEditedRow({...editedRow, consumer});
                }

                return;
            }
            if(name.includes("options")){
                if(name==="optionsUseGrid"){
                    console.log(value);
                    setEditedRow({...editedRow, options:{...editedRow.options, useGrid:value === "on" && editedRow?.options?.useGrid == false}})
                }
                if(name==="optionsCountGrid"){
                    setEditedRow({...editedRow, options:{...editedRow.options, countOfGrid:value}})
                }
                if(name==="optionsOffsetGrid"){
                    setEditedRow({...editedRow, options:{...editedRow.options, offsetOfGrid:value}})
                }
                if(name==="optionsLotOfGrid"){
                    setEditedRow({...editedRow, options:{...editedRow.options, lotOfOneGrid:value}})
                }
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
            backgroundColor: "rgb(30 30 47 / 92%)",
            color: "#dee2e6",
        }} placement={"top"} show={showModal}
                          onHide={() => {
                              setIsShowModal(false)
                          }}>
            <Offcanvas.Header style={{borderBottom: "2px solid black"}} closeButton>
                <Offcanvas.Title>{editedRow.id ? `Редактирование стратегии №:${editedRow.id}` : 'Добавление новой стратегии'}</Offcanvas.Title>
            </Offcanvas.Header>
            <Offcanvas.Body>
                <Form className="bg-custom-1">
                    <Row>
                        <Col style={{maxWidth: 390}}>
                            <Form.Select
                                onChange={handleChangeEdit}
                                className="mb-2"
                                name="producer"
                                style={{width: 351}}>
                                <option value="1">{EProducer.TKS}</option>
                                <option value="2">{EProducer.BYBIT}</option>
                            </Form.Select>
                            <Form.Control
                                className="mb-1"
                                name='name'
                                defaultValue={editedRow?.name}
                                onChange={handleChangeEdit}
                                type="text"
                                placeholder="Наименование стратегии"
                                isInvalid={!isEmpty(validation?.name)}
                                style={{width: 351}}
                            />
                            <Form.Control.Feedback type="invalid">{validation.name}</Form.Control.Feedback>
                            <SelectFilter
                                style={{maxWidth: 351}}
                                producer= {editedRow.producer}
                                value={editedRow?.ticker ? {
                                value: editedRow.ticker,
                                label: editedRow.ticker
                            } : null}
                                          onChange={handleChangeEdit} errorMsg={validation.ticker}/>
                            <Form.Group style={{maxWidth: 351}} className="mb-2 mt-2"
                                        controlId="exampleForm.ControlTextarea1">
                                <Form.Control name="description" value={editedRow?.description}
                                              onChange={handleChangeEdit}
                                              placeholder={'Краткое описание стратегии'} as="textarea" rows={2}/>
                            </Form.Group>
                        </Col>
                        {editedRow?.isPublic && (<Col style={{maxWidth: 850}}>
                            <MyEditor renderHeader={false} addNewArticle cancel={noop}/>
                        </Col>)}
                    </Row>
                    <Row>
                        <Col style={{maxWidth: 120}}>
                            <Form.Check
                                type="switch"
                                name='terminal'
                                id="custom-switch"
                                label="Терминал"
                                onChange={handleChangeEdit}
                                checked={editedRow.consumer?.includes("terminal")}
                                disabled={userName != "Admin"}
                                className="pe-2"
                            />
                        </Col>
                        <Col>
                            <MyToolTip text={'В текущем релизе недоступно!'}/>
                        </Col>
                    </Row>
                    <Row>
                        <Col style={{maxWidth: 120}}>
                            <Form.Check
                                type="switch"
                                name='test'
                                id="custom2-switch"
                                onChange={handleChangeEdit}
                                label="Эмуляция"
                                disabled
                                checked={editedRow.consumer?.includes("test") && !editedRow.consumer?.includes('terminal')}
                                className="pe-2"
                            />
                        </Col>
                        <Col>
                            <MyToolTip
                                text={'В данном режиме можно протестировать стратегию без отправки сигналов на биржу.'}/>
                        </Col>
                    </Row>
                    <Row>
                        <Col style={{maxWidth: 130}}>
                            <Form.Check
                                type="switch"
                                name='telegram'
                                id="custom3-switch"
                                onChange={handleChangeEdit}
                                label="Телеграмм"
                                checked={editedRow.consumer?.includes('telegram')}
                                disabled={!user?.telegramSubscriptionExist}
                            />
                        </Col>
                        <Col>
                            <MyToolTip
                                text={'Отправка уведомлений о сделках в телеграмм. Требуется регистрации в боте: tview_bot'}/>
                        </Col>
                    </Row>
                    <Row>
                        <Col style={{maxWidth: 230}}>
                            <Form.Check
                                type="switch"
                                name='isPublic'
                                id="custom-switch"
                                label="Публичная стратегия"
                                onChange={handleChangeEdit}
                                disabled
                            />
                        </Col>
                        <Col style={{marginLeft: -23}}>
                            <MyToolTip
                                text={'Стратегия будет видна всем пользователям в разделе \"Публичные стратегии\". В текущем релизе недоступно.'}/>
                        </Col>
                    </Row>
                    <Row>
                        <Col style={{display:"flex", alignItems:"center"}}>
                                <Form.Check
                                    className="me-2"
                                    type="switch"
                                    name='optionsUseGrid'
                                    id="custom-switch"
                                    label="Использовать сетку при срабатывании сигнала"
                                    checked={editedRow?.options?.useGrid === true}
                                    onChange={handleChangeEdit}
                                />
                                <Form.Control name="optionsCountGrid" value={editedRow?.options?.countOfGrid || 3}
                                              className="me-2"
                                              onChange={handleChangeEdit}
                                              placeholder={'count_grid'} type="text" style={{width:80}}
                                />
                                <Form.Control name="optionsOffsetGrid" value={editedRow?.options?.offsetOfGrid || 0.6}
                                              className="me-2"
                                              onChange={handleChangeEdit}
                                              placeholder={'offset_grid'} type="text" style={{width:80}}
                                />
                            <Form.Control name="optionsLotOfGrid" value={editedRow?.options?.lotOfOneGrid|| 0.001}
                                          onChange={handleChangeEdit}
                                          placeholder={'offset_grid'} type="text" style={{width:80}}
                            />
                        </Col>
                    </Row>
                    {/*<Row>*/}
                    {/*    <Col>*/}
                    {/*        <Button className="me-2" variant="outline-success">*/}
                    {/*            +Add Stop*/}
                    {/*        </Button>*/}
                    {/*    </Col>*/}
                    {/*</Row>*/}
                    {/*<Row>*/}
                    {/*    <Col>*/}
                    {/*        <Form.Control name="stopDirection" value={editedRow?.options?.lotOfOneGrid|| 0.001}*/}
                    {/*                      onChange={handleChangeEdit}*/}
                    {/*                      placeholder={'offset_grid'} type="text" style={{width:80}}/>*/}
                    {/*        <Form.Control name="stopLot" value={editedRow?.options?.lotOfOneGrid|| 0.001}*/}
                    {/*                      onChange={handleChangeEdit}*/}
                    {/*                      placeholder={'offset_grid'} type="text" style={{width:80}}/>*/}
                    {/*        <Form.Control name="stopPrice" value={editedRow?.options?.lotOfOneGrid|| 0.001}*/}
                    {/*                      onChange={handleChangeEdit}*/}
                    {/*                      placeholder={'offset_grid'} type="text" style={{width:80}}/>*/}
                    {/*    </Col>*/}
                    {/*</Row>*/}
                    <Row>
                        <Col style={{maxWidth: 100}}>
                            <Button className={"me-2 mt-2"} variant="outline-light" onClick={() => {
                                setIsShowModal(false)
                            }}>
                                Закрыть
                            </Button>
                        </Col>
                        <Col style={{paddingTop: 8}}>
                            <Button variant="outline-success" onClick={handleSave}>
                                <Icon icon={"bi bi-save"} size={15} title={''} text={' Сохранить'}/>
                            </Button>
                        </Col>
                    </Row>
                    <Gap/>
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
            consumer: ["test"],
            isActive: true,
            description: undefined,
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
        const text = `{\n\r
                "userName":"${userName}",\n\r
                "name":"${showDescriptionModal.strategy?.name}",\n\r
                "direction": "{{strategy.order.action}}",\n\r
                "quantity": "{{strategy.position_size}}",\n\r
                "producer": "${showDescriptionModal.strategy?.producer}"\n\r}
                `;
        const alert = `alert(\"{\\"userName\\"\"+\":\"+\"\\"Admin\\"\"+\",\"+\"\\"name\\"\"+\":\"+\"\\"\"+str.tostring(name)+\"\\"\"+\",\"+\"\\"producer\\"\"+\":\"+\"\\"BYBIT\\"\"+\",\"+\"\\"direction\\"\"+\":\"+\"\\"sell\\"\"+\",\"+\"\\"quantity\\"\" + \":\"+ \"\\"\" + str.tostring(quantity) + \"\\"\" + \",\"+\"\\"triggerPrice\\"\" + \":\"+ \"\\"\" + str.tostring(upos) + \"\\"\" +\",\"+\"\\"comment\\"\" + \":\"+ \"\\"\" + str.tostring(comment) + \"\\"\" + \"}\", alert.freq_all)`;
        return <ModalView header={'Как добавить стратегию в Tradingview:'} show={showDescriptionModal.show}
                          cancel={() => {
                              setShowDescriptionModal({show: false, name: undefined})
                          }} text={<div>
            <h6 className="mt-3">Как мне создать оповещения для стратегии?</h6>
            <ul style={{paddingLeft: "1rem"}}>
                <li>Нажать на <em>Добавить оповещение</em> в панели <em dir="ltr">Тестера стратегий.</em><br/>
                    <img className="myImg" style={{maxHeight: 100}}
                         src="/icons/4.jpeg"
                         alt="альтернативный текст"/>
                </li>
                <li>Воспользоваться кнопкой <em>Добавить оповещение для&nbsp;</em>в выпадающем меню стратегии.<br/>
                    <img className="myImg" style={{maxHeight: 300}}
                         src="/icons/3.jpeg"/>
                </li>
                <li>Выбрать свою стратегию в диалоговом окне создания оповещений.<br/>
                    <img className="myImg" style={{maxHeight: 480}}
                         src="/icons/2.jpeg"/>
                </li>
                <li>
                    Поле <em>Message</em> заполнить текстом ниже. <br/>
                    <div className="mt-2 text-bg-success">{text}</div>
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
                <li>
                    Для alert-ов заполнить: <br/>
                    Поле <em>Message</em> заполнить текстом ниже. <br/>
                    <div className="mt-2 text-bg-success">{alert}</div>
                    <br/>
                    <Button className="me-2" onClick={(e: any) => {
                        void copyTextToClipboard(alert);
                        const x = document.getElementById('test3');
                        x!.innerText = 'Скопировано!'
                    }} variant={"outline-success"}>
                        Скопировать
                    </Button>
                    <span id="test3"/>
                </li>
                <li>
                    Указать URL для отправки сигналов<br/>
                    <img className="myImg" src='/icons/1.png' style={{maxHeight: 480}}/>
                </li>
                <li>
                    URL для отправки сигналов<br/>
                    <div className="mt-2 text-bg-success">{'http://77.232.130.186/api/tv'}</div>
                    <br/>
                    <Button className="me-2" onClick={(e: any) => {
                        void copyTextToClipboard('http://77.232.130.186/api/tv');
                        const x = document.getElementById('test2');
                        x!.innerText = 'Скопировано!'
                    }} variant={"outline-success"}>
                        Скопировать
                    </Button>
                    <span id="test2"/>
                </li>
            </ul>
        </div>}/>
    }

    const calcChart = calcDataForGraphProfit(strategy);
    const calcLastAvrPrice = (row: any): number => {
        const prices = row.enterAveragePrice;
        if (prices.length > 0) {
            return prices.reduce((a: string, b: string) => (Number(a) + Number(b))) / prices.length;
        }
        return 0;
    }

    const renderPortfolio = () => {
        return userName === 'Admin' ? (<Portfolio/>) : <></>;
    };

    return <>
        {renderPortfolio()}
        {showModal && renderEditModal()}
        {showDescriptionModal.show && renderDescriptionModal()}
        {renderRemoveModal()}
        {showGraph.show && (<ModalView
            show={showGraph.show}
            cancel={() => setShowGraph({show: false, nameStrategy: undefined})}
            header={`График доходности: ${showGraph.nameStrategy || ""}`}
            text={<Chart data={
                calcChart.find((i: any) => {
                    const id = strategy?.find((i: any) => {
                        return i.name === showGraph?.nameStrategy
                    })!.id;
                    return i.id === Number(id)
                })?.graphResult || []
            }/>}
        />)}
        <RowFiled>
            <Button
                style={{width: 240}}
                className={"mb-1"}
                onClick={handleAddRow}
                variant={"outline-secondary"}
            >
                <Icon icon={'bi bi-plus-square'} size={15} title={''} text={' Добавить стратегию'}/>
            </Button>
        </RowFiled>
        <Row>
            <Col className="ps-4">
                <div style={{border: "0.01rem solid grey", maxHeight: 300}}
                     className={strategy.length > 0 ? "resp table-responsive" : "table-responsive"}>
                    <Table style={{color: "rgb(140, 144, 154)"}} bordered
                           variant="outline">
                        <thead>
                        <tr key={"header"}>
                            {columns.map((column, index) => {
                                return <th key={column.field}>{column.fieldName}</th>
                            })}
                        </tr>
                        </thead>
                        <tbody>
                        {strategy.length > 0 ? strategy?.map((row, index) => {

                            return <tr className={row.errorData?.message ? "errorStrategy" : ""} key={row.id! + 3005}
                                       style={hoverTable === index && clickTable !== index ? {
                                           color: "white",
                                           backgroundColor: "lightgray"
                                       } : clickTable === index ? {backgroundColor: "lightblue", color: "white"} : {}}
                                       onClick={() => {
                                           setClickTable(index);
                                           setEditedRow(strategy.find((i: any) => i?.id === row?.id));
                                       }} onMouseLeave={() => {
                                setHoverTable(-1)
                            }} onMouseEnter={() => {
                                setHoverTable(index)
                            }}
                            >
                                <td className={"align-middle"}>{index + 1}</td>
                                <td className={"align-middle"}>
                                    <NameComponent row={row}/>
                                </td>
                                <td className={"align-middle"}>{row.producer}</td>
                                <td className={"align-middle"}>{row?.ticker}</td>
                                <td className={"align-middle"}>{row.consumer?.map((i: any) => {
                                    return <Form.Check
                                        label={i === "test" ? "Эмуляция" : i === "telegram" ? "Телеграмм" : "Биржа"}
                                        checked
                                        disabled
                                    />
                                })}</td>
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
                                    {formatNumber(calcChart.find((i: any) => {
                                        return i.id === Number(row.id)
                                    })?.graphResult?.slice(-1)[0]?.y) || 0}
                                </td>
                                {/*<td className={"align-middle text-center"}>*/}
                                {/*    {formatNumber(calcLastAvrPrice(row), 4)}*/}
                                {/*</td>*/}
                                <td className={"align-middle text-center"}>
                                    <LastPrice row={row}/>
                                </td>
                                <td style={{verticalAlign: "middle"}}>
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
                                            setShowGraph({show: true, nameStrategy: row.name})
                                        }} variant={"dark"}>
                                            <Icon icon={"bi bi-graph-up"} size={15} title={'View Chart'}/>
                                        </Button>
                                    </ButtonGroup>
                                    <ButtonGroup>
                                        <Button onClick={() => {
                                            setShowDescriptionModal({show: true, strategy: row})
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
                </div>
            </Col>
        </Row>
    </>
}

export default MyTable;

