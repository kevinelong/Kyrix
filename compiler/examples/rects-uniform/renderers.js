var rectsRendering = function (svg, data) {
    g = svg.append("g");

    // rect
    g.selectAll("rect")
        .data(data)
        .enter()
        .append("rect")
        .attr("x", function (d) {return d.x;})
        .attr("y", function (d) {return d.y;})
        .attr("width", function (d) {return d.w;})
        .attr("height", function (d) {return d.h;})
        .style("fill", "#888888")
        .style("stroke", "#222222")
        .style("stroke-width", 3);
};

module.exports = {
    rectsRendering: rectsRendering
};
