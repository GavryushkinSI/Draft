export function saveRow(editRecord: any, array: Array<any>): Array<any> {
    let copy = array;
    copy = Object.keys(copy).map((index: any) => {
        let item = copy[index];
        if (item.id === editRecord.id) {
            item = editRecord;
        }

        return item;
    });
    return [...copy];
}

export function removeRow(array: Array<any>, selectRecordIndex?: string): Array<any> {
    const copy = array;
    copy.forEach((item, index) => {
        if (item.id === selectRecordIndex) {
            copy.splice(index, 1);
        }
    });
    return [...copy];
}