// import CanvasJSReact from '../libraries/canvasjs.react';
import CanvasJSReact from '../libraries/canvasjs.stock.react';
import React, {Component} from "react";
import moment from "moment";

const CanvasJS = CanvasJSReact.CanvasJS;
// const CanvasJSChart = CanvasJSReact.CanvasJSChart;
const CanvasJSStockChart = CanvasJSReact.CanvasJSStockChart;

interface IProps {
    data: any[];
    data2?:any[];
    data3?:any[];
    isAdmin?: boolean;
}

const containerProps = {
    margin: "auto"
};

export class Chart extends Component<IProps, never> {

    render() {
        const options = this.props.isAdmin ? {
            title: {text: ''},
            theme: "dark1",
            legend: {
                cursor: "pointer",
            },
            charts: [{
                axisY: {
                    title: "Отклик, ms",
                    titleFontSize: 12,
                    labelFontSize: 12,
                },
                data: [
                ...this.props.data.map((i: any) => {
                    return {
                        type: "line",
                        name: i.method,
                        showInLegend: true,
                        dataPoints: [...i.dataPoints]
                    }
                })]
            }],
        } : {
            title: {text: ''},
            theme: "light",
            rangeChanged: function (e: any) {
                // console.log(e)
            },
            //backgroundColor: "hwb(0deg 0% 100% / 8%)",
            charts: [{
                axisX: {
                    // labelAngle: -30,
                    labelFontSize: 12,
                    // labelPlacement:"inside",
                    gridDashType: "dot",
                    gridColor: "blue",
                    intervalType: "number",
                    gridThickness: 1,
                    labelFormatter: function (e: any) {
                        return e.value;
                        // return CanvasJS.formatDate( e.y, "DD MMM");
                    },
                },
                axisY: {
                    title: "Доходность, пункты (USD)",
                    titleFontSize: 14,
                    labelFontSize: 12,
                },
                data: [{
                    type: "splineArea",
                    color: "green",
                    dataPoints: this.props.data || [],
                },
                    {
                    type: "splineArea",
                    color: "red",
                    dataPoints: this.props.data3 || [],
                }]
            },
                {
                    height:200,
                    title: {
                        text: "Доход за сделку, USD"
                    },
                    data: [{
                        color: "green",
                        dataPoints : this.props.data2 || [],
                    }]
                }],
            rangeSelector: {
                enabled: false,
            },
            navigator: {
                height:30,
                data: [{
                    type: "line",
                    color: "red",
                    dataPoints : this.props.data || []
                }],
                //dynamicUpdate: true,
            }
        };

        return (
            <div>
                <CanvasJSStockChart
                    options={options}
                    containerProps = {containerProps}
                    /* onRef={ref => this.chart = ref} */
                />
                {/*You can get reference to the chart instance as shown above using onRef. This allows you to access all chart properties and methods*/}
            </div>
        );
    }
}