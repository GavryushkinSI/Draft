import {Button, Modal} from "react-bootstrap";

interface IProps {
    show: boolean;
    accept?: () => void;
    cancel: () => void;
    text: JSX.Element | string;
    header?:string;
}

const ModalView = (props: IProps) => {

    return (
        <>
            <Modal show={props.show} onHide={() => {
                props.cancel();
            }}>
                <Modal.Header style={{
                    backgroundColor: "#212529",
                    color: "#dee2e6"
                }} closeButton>
                    <Modal.Title>{props.header?props.header:'Обратите внимание!'}</Modal.Title>
                </Modal.Header>
                <Modal.Body style={{
                    backgroundColor: "#212529",
                    color: "#dee2e6"
                }}>{props.text}</Modal.Body>
                {props.accept&&(<Modal.Footer style={{
                    backgroundColor: "#212529",
                    color: "#dee2e6"
                }}>
                    <Button variant="outline-light" onClick={() => {
                        props.cancel();
                    }}>
                        Отмена
                    </Button>
                    <Button variant="outline-success" onClick={() => {
                        props.accept?.();
                    }}>
                        ОК
                    </Button>
                </Modal.Footer>)}
            </Modal>
        </>
    );
}

export default ModalView;
