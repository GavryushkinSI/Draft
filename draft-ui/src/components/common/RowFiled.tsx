import {Col, Row} from "react-bootstrap";
import React, {CSSProperties, ReactNode} from "react";

interface IProps {
    children: ReactNode | ReactNode[];
    isAdaptive?: boolean;
    className?:string;
}

const RowFiled: React.FC<IProps> = (props) => {
    const content: React.ReactNode[] = React.Children.toArray(props.children);
    const html = [];

    props.isAdaptive?content.map((i, index)=>{
        html.push(<Row key={index}>
            <Col className="ps-4">
                {i}
            </Col>
        </Row>)
    }):html.push(<Col key={1000} className="ps-4">
                {content}
           </Col>);

    return <>{props.isAdaptive?<>{html}</>:<Row className={props.className}>{html}</Row>}</>;
}

export default RowFiled;