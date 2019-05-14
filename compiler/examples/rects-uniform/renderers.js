var rectsRendering = function (svg, data) {
    g = svg.append("g");
    g.selectAll("rect")
        .data(data)
        .enter()
        .append("rect")
        .attr("x", function (d) {return d.x1;})
        .attr("y", function (d) {return d.y1;})
        .attr("width", function (d) {return d.x1;})
        .attr("height", function (d) {return d.y2;})
        .attr("r", 2)
        .attr("fill", "#145bce");

    var rectWidth = data.x2 - data.x1;
    var rectHeight =  data.y2 - data.y1

    // rect
    g.selectAll("rect")
        .data(data)
        .enter()
        .append("rect")
        .attr("x", function (d) {return +d.x2 - rectWidth / 2;})
        .attr("y", function (d) {return +d.y2 - rectHeight / 2;})
        .attr("rx", 10)
        .attr("ry", 10)
        .attr("width", rectWidth)
        .attr("height", rectHeight)
        .style("fill", "#FFF")
        .style("stroke", "#CCC")
        .style("stroke-width", 3);
};

module.exports = {
    rectsRendering: rectsRendering
};
