const Transform = require("../../src/Transform").Transform;

var rectsTransform = new Transform("select * from rects_uniform;",
    "rects_uniform",
    function (row, width, height) {
        var ret = [];
        ret.push(row[0]);
        ret.push(d3.scaleLinear().domain([0, 100000]).range([0, width])(row[1]));
        ret.push(d3.scaleLinear().domain([0, 100000]).range([0, height])(row[2]));
        ret.push(row[3]);
        ret.push(row[4]);
        return Java.to(ret, "java.lang.String[]");
    },
    ["id", "x", "y", "w", "h"],
    true);

module.exports = {
    rectsTransform : rectsTransform
};
