// import CanvasJSReact from '../libraries/canvasjs.react';
import CanvasJSReact from '../libraries/canvasjs.stock.react';
import {Component} from "react";
import moment from "moment";
const CanvasJS = CanvasJSReact.CanvasJS;
// const CanvasJSChart = CanvasJSReact.CanvasJSChart;
const CanvasJSStockChart = CanvasJSReact.CanvasJSStockChart;

interface IProps {
    data: any[];
}

export class Chart extends Component<IProps, never> {
    render() {

        const options = {
            title: {text: ''},
            theme: "dark1",
            rangeChanged: function (e: any) {
                // console.log(e)
            },
            backgroundColor: "hwb(0deg 0% 100% / 0%)",
            charts: [{
                axisX: {
                    // labelAngle: -30,
                    labelFontSize: 12,
                    // labelPlacement:"inside",
                    gridDashType: "dot",
                    gridColor:"orange",
                    intervalType: "number",
                    gridThickness: 1,
                    labelFormatter: function (e:any) {
                        return e.value;
                        // return CanvasJS.formatDate( e.y, "DD MMM");
                    },
                },
                axisY:{
                    title: "Доходность, пункты",
                    titleFontSize:12,
                    labelFontSize: 12,
                },
                data: [{
                    type: "splineArea",
                    color: "lightblue",
                    dataPoints: this.props.data,
                }]
            }],
            rangeSelector: {
                enabled: false,
            },
            navigator: {
                dynamicUpdate: true,
            }
        };

        return (
            <div>
                <CanvasJSStockChart options={options}
                    /* onRef={ref => this.chart = ref} */
                />
                {/*You can get reference to the chart instance as shown above using onRef. This allows you to access all chart properties and methods*/}
            </div>
        );
    }
}