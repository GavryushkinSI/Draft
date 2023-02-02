import {Button, Col, Container, FormText, Nav, Navbar, Row} from "react-bootstrap"
import Icon from "./Icon";
import '../../styles/common.css';
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css"

const Page=()=>{
    return <Container fluid>
        <Row className='vh-100'>
            <Col lg={1} style={{backgroundColor:"lightgray"}}>
                {/*<Row>*/}
                {/*    <Col style={{borderRight: "1px solid #353a53", height: 64, paddingTop: 10}}>*/}
                {/*        <Icon icon={'bi bi-microsoft'} size={17} title={''} text={'ParseSignal'}/>*/}
                {/*        <br/>*/}
                {/*    </Col>*/}
                {/*</Row>*/}
                {/*<Row>*/}
                {/*    <Col className={'pt-1'}*/}
                {/*         style={{borderRight: "1px solid #353a53", borderTop: "1px solid #353a53", lineHeight: 2}}>*/}
                {/*        <Button*/}
                {/*            style={{*/}
                {/*            backgroundColor: "inherit",*/}
                {/*            border: "none", padding: 0*/}
                {/*        }}>*/}
                {/*            <Icon icon={'bi bi-gear'} size={15} title={''} text={'Настройки'}/>*/}
                {/*        </Button>*/}
                {/*        <br/>*/}
                {/*        <Icon icon={'bi bi-question-square'} size={15} title={''} text={'Поддержка'}/>*/}
                {/*    </Col>*/}
                {/*</Row>*/}
            </Col>
            <Col lg={11}>
                <Row>
                    <Col style={{
                        height: 65,
                        // borderBottom: "1px solid #353a53",
                        backgroundColor: "lightgray",
                    }}>
                        {/*<Navbar>*/}
                        {/*    <Container style={{justifyContent: "flex-end"}}>*/}
                        {/*        <Nav>*/}
                        {/*            <Nav.Item className={'pe-2'}><Icon icon={'bi bi-bell'} size={20} text={''}*/}
                        {/*                                               title={''}/>*/}
                        {/*            </Nav.Item>*/}
                        {/*        </Nav>*/}
                        {/*    </Container>*/}
                        {/*</Navbar>*/}
                    </Col>
                </Row>
            </Col>
        </Row>
    </Container>
}

export default Page;