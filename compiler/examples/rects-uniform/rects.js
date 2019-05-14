// libraries
const Project = require("../../src/index").Project;
const Canvas = require("../../src/Canvas").Canvas;
const Jump = require("../../src/Jump").Jump;
const Layer = require("../../src/Layer").Layer;
const View = require("../../src/View").View;

// project components
const renderers = require("./renderers");
const transforms = require("./transforms");
const placements = require("./placements");

// construct a project
var p = new Project("rects_uniform", "../../../config.txt");

// ================== top zoom level ===================
var topWidth = 1000000, topHeight = 1000000;
var topCanvas = new Canvas("top", topWidth, topHeight);
p.addCanvas(topCanvas);

// rects layer
var rectsLayer = new Layer(transforms.rectsTransform, false);
topCanvas.addLayer(rectsLayer);
rectsLayer.addPlacement(placements.rectsPlacement);
rectsLayer.addRenderingFunc(renderers.rectsRendering);

// ================== bottom zoom level ===================
var bottomCanvas = new Canvas("bottom", topWidth * 2, topHeight * 2);
p.addCanvas(bottomCanvas);
bottomCanvas.addLayer(rectsLayer);

// ================== Views ===================
var view = new View("rectview", 0, 0, 1000, 1000);
p.addView(view);
p.setInitialStates(view, topCanvas, 5000, 5000);

// save to db
p.saveProject();
