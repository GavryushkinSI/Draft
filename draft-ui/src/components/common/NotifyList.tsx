import {Toast} from "react-bootstrap";
import React from "react";
import '../../styles/common.css';

interface IProps {
    text: string[];
    close: (index: number) => void;
}

export class NotifyList extends React.Component<IProps, {}> {

    private handleClose = (index: number) => {
        this.props.close(index);
    }

    render() {
        return (<div className={"notifyList"}
        >
            {this.props.text.map((item, index) => {
                return <><Toast
                    className="d-inline-block m-1 myToast"
                    bg={'dark'}
                    onClose={(e) => this.handleClose(index)}
                    delay={3000}
                    animation
                >
                    <Toast.Header>
                        <strong className="me-auto">Info</strong>
                    </Toast.Header>
                    <Toast.Body className={'text-white'}>
                        {item}
                    </Toast.Body>
                </Toast>
                    <br/>
                </>
            })}
        </div>)
    }
}
