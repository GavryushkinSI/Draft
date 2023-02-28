import {Button, Container, Spinner} from "react-bootstrap";

const PreLoad = () => {
    return <>
        <Container fluid style={{
            backgroundColor: "lightblue",
            position: "absolute",
            zIndex: 5000,
            opacity: 0.1,
            height: "100vh"
        }}/>
        <Button style={{
            position: "absolute",
            top: "50%",
            left: "50%", zIndex: 10000
        }} variant="secondary" disabled>
            <Spinner
                as="span"
                animation="grow"
                size="sm"
                role="status"
                aria-hidden="true"
            />
            {' Загрузка...'}
        </Button>
    </>
}

export default PreLoad;