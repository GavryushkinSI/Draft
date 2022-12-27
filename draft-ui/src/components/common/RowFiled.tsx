import {Row} from "react-bootstrap";

const RowFiled = (children: any): JSX.Element => {
    let html = [];

    return <Row>
        {children?.map((item: any) => {
            return html.push(item);
        })}
    </Row>
}

export default RowFiled;