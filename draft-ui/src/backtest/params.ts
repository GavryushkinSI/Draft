export const STRATEGY_MAP: { [key in string]: any } = {
    "strategy1": {name: 'strategy1',
        param: [{id: 1, name: 'Размер кирпичика', min: 55, max: 500, step: 5, type: 'step'}, {
            id: 2,
            name: 'Комиссия',
            value: 0,
            type: 'one'
        }]
    },
    "strategy2": {name: 'strategy2',
        param: [{
            id: 1, name: 'Размер кирпичика', defaultValue:100, min: 150, max: 151, step: 1, type: 'step'}, {
            id: 2,
            name: 'atr_period',
            defaultValue:5,
            min: 5,
            max: 30,
            step: 5,
            type: 'step'
        }, {
            id: 3,
            name: 'coeff',
            defaultValue:3,
            min: 1,
            max: 10,
            step: 3,
            type: 'step'
        }, {
            id: 4,
            name: 'cci_period',
            defaultValue:10,
            min: 1,
            max: 50,
            step: 10,
            type: 'step'
        },
            {  id: 5,
                name: 'Комиссия',
                value: 0,
                type: 'one'}]
    }
};





