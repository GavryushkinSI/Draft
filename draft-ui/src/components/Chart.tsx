import CanvasJSReact from '../libraries/canvasjs.react';
import {Component} from "react";
const CanvasJS = CanvasJSReact.CanvasJS;
const CanvasJSChart = CanvasJSReact.CanvasJSChart;

interface IProps{
    data:any[];
}

export class Chart extends Component<IProps, never> {
    render() {
        const options = {
            animationEnabled: false,
            theme: "dark1",
            title: {
                text: ""
            },
            axisY: {
                title: "",
                suffix: ""
            },
            data: [{
                type: "splineArea",
                // xValueFormatString: "YYYY",
                // yValueFormatString: "#,##0.## bn kW⋅h",
                // showInLegend: true,
                // legendText: "kWh = one kilowatt hour",
                dataPoints: this.props.data
            }]
        }

        return (
            <div>
                {this.props.data?.length>0&&(<CanvasJSChart options = {options}
                    /* onRef={ref => this.chart = ref} */
                />)}
                {/*You can get reference to the chart instance as shown above using onRef. This allows you to access all chart properties and methods*/}
            </div>
        );
    }
}